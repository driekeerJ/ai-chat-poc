# ai-chat-poc
## Create project
1. Go to `https://start.spring.io/`
2. Choose at least `openAI`, `Thymeleaf` and `Web`
3. Generate a project

## Register your key for openAI and the model you will use
Put the following setting in your `application.properties`. I have put it in my local properties that will not be uploaded to git, for obvious reasons.
```bash
spring.ai.openai.api-key=YOUR_OPENAI_API_KEY
spring.ai.openai.chat.options.model=gpt-4.1-nano
```

## Add a commandline runner to your application
```java
package com.wallway.ai_chat_poc;

import java.util.Scanner;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class AiChatPocApplication {

	public static void main(String[] args) {
		SpringApplication.run(AiChatPocApplication.class, args);
	}


    @Bean
    public CommandLineRunner cli(ChatClient.Builder chatClientBuilder) {
        return args -> {
            var chatClient = chatClientBuilder
                .defaultSystem("You are a virtual football coach, expert in football tactics, training, and player development.")
                .build();

            System.out.println("\nI am your virtual football coach.\n");
            try (Scanner scanner = new Scanner(System.in)) {
                while (true) {
                    System.out.print("\nUSER: ");
                    System.out.println("\nASSISTANT: " +
                        chatClient.prompt(scanner.nextLine())
                            .call()
                            .content());
                }
            }
        };
    }
}

```

What do we do here?
- `defaultSystem` sets the default system message. A system message is a short instruction that tells the AI how it should behave or what role it should play during the conversation.
- `chatClient.prompt(scanner.nextLine())` just gets the text that you typed. This is also the trigger. 
- `call` will do the actual call
- `content` retrieves the 'content' out of the response model.

Now test it. Build the project and start it with 'local' settings:
```bash
./gradlew build
java -jar build/libs/ai-chat-poc-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

Just ask it a question and see what it response it with. 

## Now let's use a controller
Lets first cleanup the basic application file by remove the commandline scanner:
```java
package com.wallway.ai_chat_poc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AiChatPocApplication {

	public static void main(String[] args) {
		SpringApplication.run(AiChatPocApplication.class, args);
	}
}
```
And now create the following RestController in your system
```java
package com.wallway.ai_chat_poc;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api")
public class ChatController {
    private static final String DEFAULT_SYSTEM_PROMPT = """
            You are a virtual football coach, expert in football tactics, training, and player development.
            Be knowledgeable, motivating, and provide practical advice for players and teams.
            """;
    private final ChatClient chatClient;

    public ChatController(ChatClient.Builder chatClient) {
        this.chatClient = chatClient
                .defaultSystem(DEFAULT_SYSTEM_PROMPT)
                .build();
    }

    @PostMapping("chat")
    public String chat(@RequestBody PromptRequest promptRequest) {
        var chatResponse = chatClient
                .prompt()
                .user(promptRequest.prompt())
                .call()
                .chatResponse();
        return (chatResponse != null) ? chatResponse.getResult().getOutput().getText() : null;
    }

    record PromptRequest(String prompt) {
    }
}
```

Build and run your application
```bash
./gradlew build
java -jar build/libs/ai-chat-poc-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

And run the following curl command from your commandline:
```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"prompt": "What are the key principles of effective football training?"}'
```

Now try the following:
```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"prompt": "Tell me about football tactics in max 3 paragraphs?"}'
```

As you can see it answers it all at once. We also might be interested in having part of the answer already while it is generating the rest. For this we can use streaming. 

## Use streaming for flow
Change the controller by adding streaming:
```java
package com.wallway.ai_chat_poc;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("api")
public class ChatController {
    private static final String DEFAULT_SYSTEM_PROMPT = """
            You are a virtual football coach, expert in football tactics, training, and player development.
            Be knowledgeable, motivating, and provide practical advice for players and teams.
            """;

    private final ChatClient chatClient;

    public ChatController(ChatClient.Builder chatClient) {
        this.chatClient = chatClient
                .defaultSystem(DEFAULT_SYSTEM_PROMPT)
                .build();
    }

    @PostMapping("/chat/stream")
    public Flux<String> chatStream(@RequestBody PromptRequest promptRequest) {
        return chatClient
                .prompt()
                .user(promptRequest.prompt())
                .stream()
                .content();
    }

    record PromptRequest(String prompt) {
    }
}
```
As you can see, we will now return a `Flux` so we can async return what we get. 

Lets start the application again:
```bash
./gradlew build
java -jar build/libs/ai-chat-poc-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

And ask the same question but now on the stream endpoint:
```bash
curl -X POST http://localhost:8080/api/chat/stream \
  -H "Content-Type: application/json" \
  -d '{"prompt": "Tell me about football tactics in max 3 paragraphs?"}'
```

As you can see, the answer now comes in parts.