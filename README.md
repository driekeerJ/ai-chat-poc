# ai-chat-poc
## Step 1. Create project
1. Go to `https://start.spring.io/`
2. Choose at least `openAI`, `Thymeleaf` and `Web`
3. Generate a project

## Step 2. Register your key for openAI and the model you will use
Put the following setting in your `application.properties`. I have put it in my local properties that will not be uploaded to git, for obvious reasons.
```bash
spring.ai.openai.api-key=YOUR_OPENAI_API_KEY
spring.ai.openai.chat.options.model=gpt-4.1-nano
```

## Step 3. Add a commandline runner to your application
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
                .defaultSystem("You are a Unicorn Rentals Agent, expert in all sorts of things related to Unicorns and renting them.")
                .build();

            System.out.println("\nI am your Unicorn Rentals assistant.\n");
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
