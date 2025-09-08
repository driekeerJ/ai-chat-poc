# AI-chat-poc
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
        return args -%3E {
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
    public Flux%3CString> chatStream(@RequestBody PromptRequest promptRequest) {
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
## Adding a frontend
We have added Thymeleaf as a dependency at the start. We are now going to use that. 

Add the following to your application proporties file:
```
# UI Configuration
spring.thymeleaf.cache=false
spring.thymeleaf.prefix=classpath:/templates/
spring.thymeleaf.suffix=.html
```

Create a new chat.html file in `templates/`
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Virtual Football Coach AI</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <script>
        tailwind.config = {
            theme: {
                extend: {
                    colors: {
                        'dev-dark': '#1a2e1a',
                        'dev-darker': '#0f1f0f',
                        'dev-accent': '#22c55e',
                        'dev-accent-hover': '#16a34a',
                        'dev-secondary': '#15803d',
                        'dev-text': '#f0fdf4',
                        'dev-text-muted': '#bbf7d0',
                        'dev-border': '#166534',
                        'dev-message-user': '#1e3a1e',
                        'dev-message-ai': '#1a2f1a'
                    }
                }
            }
        }
    </script>
    <style>
        .chat-container {
            height: calc(100vh - 180px);
        }
        .message-container {
            height: calc(100vh - 280px);
            overflow-y: auto;
        }
        @keyframes blink {
            0% { opacity: 1; }
            50% { opacity: 0; }
            100% { opacity: 1; }
        }
        .cursor {
            display: inline-block;
            width: 2px;
            height: 1em;
            background-color: #22c55e;
            margin-left: 2px;
            animation: blink 1s infinite;
        }
        /* Custom scrollbar for dark mode */
        ::-webkit-scrollbar {
            width: 8px;
        }
        ::-webkit-scrollbar-track {
            background: #0f1f0f;
        }
        ::-webkit-scrollbar-thumb {
            background: #166534;
            border-radius: 4px;
        }
        ::-webkit-scrollbar-thumb:hover {
            background: #15803d;
        }
    </style>
</head>
<body class="bg-dev-darker min-h-screen text-dev-text">
    <div class="container mx-auto px-4 py-8">
        <!-- Header -->
        <header class="mb-8">
            <div class="flex justify-between items-center">
                <div class="flex items-center">
                    <h1 class="text-3xl font-bold text-dev-accent">⚽ Virtual Football Coach</h1>
                </div>
            </div>
            <p class="text-dev-text-muted mt-2">Get real-time football coaching advice and tactical insights from your virtual coach!</p>
        </header>

        <!-- Chat Interface -->
        <div class="bg-dev-dark rounded-xl shadow-lg p-6 chat-container border border-dev-border flex flex-col">
            <div class="message-container flex-grow mb-4" id="messageContainer">
                <div class="flex mb-4">
                    <div class="w-10 h-10 rounded-full bg-dev-accent bg-opacity-20 flex items-center justify-center mr-3">
                        <span class="text-lg">⚽</span>
                    </div>
                    <div class="bg-dev-message-ai rounded-lg p-3 max-w-3xl border border-dev-border">
                        <p class="text-dev-text">Welcome to your Virtual Football Coach! I'm here to help you with football tactics, training advice, and player development. What can I help you with today?</p>
                    </div>
                </div>
                <!-- Messages will be added here dynamically -->
            </div>

            <!-- Input Area -->
            <div class="border-t border-dev-border pt-4 mt-auto">
                <form id="chatForm" class="flex w-full">
                    <input type="text" id="userInput" class="flex-grow bg-dev-darker border border-dev-border rounded-l-lg px-4 py-3 focus:outline-none focus:ring-2 focus:ring-dev-accent text-dev-text" placeholder="Ask about football tactics, training, or strategies...">
                    <button type="submit" class="bg-dev-accent text-white px-6 py-3 rounded-r-lg hover:bg-dev-accent-hover transition">Send</button>
                </form>
            </div>
        </div>
    </div>

    <script>
        document.addEventListener('DOMContentLoaded', function() {
            const messageContainer = document.getElementById('messageContainer');
            const chatForm = document.getElementById('chatForm');
            const userInput = document.getElementById('userInput');
            let isStreaming = false;
            let currentResponseElement = null;
            let currentResponseText = '';

            chatForm.addEventListener('submit', async function(e) {
                e.preventDefault();

                const message = userInput.value.trim();
                if (!message || isStreaming) return;

                // Add user message to chat
                addMessage(message, 'user');
                userInput.value = '';

                try {
                    // Create AI response container
                    currentResponseElement = createResponseContainer();
                    isStreaming = true;
                    currentResponseText = '';

                    // Send request to backend
                    const response = await fetch('api/chat/stream', {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json',
                            'Accept': 'application/octet-stream' // Use a direct streaming content type
                        },
                        body: JSON.stringify({ prompt: message })
                    });

                    if (!response.ok || !response.body) {
                        throw new Error('Failed to get response');
                    }

                    const reader = response.body.getReader();
                    const decoder = new TextDecoder();

                    // Process the stream directly without SSE parsing
                    while (true) {
                        const { done, value } = await reader.read();

                        if (done) {
                            // Remove cursor when stream ends
                            const cursor = currentResponseElement.querySelector('.cursor');
                            if (cursor) cursor.remove();
                            break;
                        }

                        // Decode and append the chunk directly
                        const chunk = decoder.decode(value, {stream: true});
                        appendToResponse(chunk);

                        // Force browser to render the update immediately
                        await new Promise(resolve => setTimeout(resolve, 0));
                    }
                } catch (error) {
                    console.error('Error:', error);
                    appendToResponse('Sorry, I encountered an error. Please try again.');
                } finally {
                    isStreaming = false;
                    currentResponseElement = null;
                }
            });

            function addMessage(content, sender) {
                const messageDiv = document.createElement('div');
                messageDiv.className = 'flex mb-4';

                if (sender === 'user') {
                    messageDiv.innerHTML = `
                        <div class="ml-auto flex">
                            <div class="bg-dev-message-user rounded-lg p-3 max-w-3xl border border-dev-border">
                                <p class="text-dev-text">${escapeHtml(content)}</p>
                            </div>
                            <div class="w-10 h-10 rounded-full bg-dev-secondary bg-opacity-20 flex items-center justify-center ml-3">
                                <span class="text-lg">👤</span>
                            </div>
                        </div>
                    `;
                }

                messageContainer.appendChild(messageDiv);
                messageContainer.scrollTop = messageContainer.scrollHeight;
            }

            function createResponseContainer() {
                const responseDiv = document.createElement('div');
                responseDiv.className = 'flex mb-4';
                responseDiv.innerHTML = `
                    <div class="w-10 h-10 rounded-full bg-dev-accent bg-opacity-20 flex items-center justify-center mr-3">
                        <span class="text-lg">⚽</span>
                    </div>
                    <div class="bg-dev-message-ai rounded-lg p-3 max-w-3xl border border-dev-border">
                        <p class="text-dev-text response-text"></p>
                        <span class="cursor"></span>
                    </div>
                `;

                messageContainer.appendChild(responseDiv);
                messageContainer.scrollTop = messageContainer.scrollHeight;

                return responseDiv;
            }

            function appendToResponse(text) {
                if (!currentResponseElement) return;

                currentResponseText += text;
                const responseTextElement = currentResponseElement.querySelector('.response-text');

                // Replace newlines with <br> tags for proper line breaks
                const formattedText = currentResponseText.replace(/\n/g, '<br>');
                responseTextElement.innerHTML = formattedText;

                // Ensure scroll happens after content is updated
                requestAnimationFrame(() => {
                    messageContainer.scrollTop = messageContainer.scrollHeight;
                });
            }

            function escapeHtml(unsafe) {
                return unsafe
                    .replace(/&/g, "&amp;")
                    .replace(/</g, "&lt;")
                    .replace(/>/g, "&gt;")
                    .replace(/"/g, "&quot;")
                    .replace(/'/g, "&#039;");
            }
        });
    </script>
</body>
</html>
```

The main thing about this webpage is that it accepts a message, sends it to the `stream` endpoint we just created and then displays the response. 

Make sure Spring knows what to return by adding a webcontroller.:
```java
package com.wallway.ai_chat_poc;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    @GetMapping("/")
    public String chatPage() {
        return "chat";
    }
}
```

And again, run the application:
```shell
./gradlew build
java -jar build/libs/ai-chat-poc-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

And now open `http://localhost:8080`, just ask it a question.

## Local memory
The AI doesn't remember previous conversations because each request is completely independent - it's like talking to someone who has amnesia and forgets everything after each sentence. To make it remember, we need to store the conversation history and send it along with each new message so the AI can see the full context of what was discussed before.

If you still have the application running, just tell it your name and then in the next prompt, ask it to tell you what your name is.

As you can see, it doesn't remember.

Lets update the ChatController:
```java
package com.wallway.ai_chat_poc;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
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
        var chatMemory = MessageWindowChatMemory.builder()
                .maxMessages(20)
                .build();

        this.chatClient = chatClient
                .defaultSystem(DEFAULT_SYSTEM_PROMPT)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
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
The basic change is that we are adding local memory and add that as an Advisor. Now you have Advisors and you have tools within Spring AI. Advisors, like ChatMemory and VectorDatabases are always there to help out. They will work side by side basically on every call being made. Tools, like api calls, calculations etcetera, are only used when the AI thinks it is a good idea to use them. 

Now start the application again:

```shell
./gradlew build
java -jar build/libs/ai-chat-poc-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

And tell it your name. 
Once it answered, ask it what your name was. 
## RAG and vector databases
### Explanation
RAG (Retrieval-Augmented Generation) is a way of making AI smarter by letting it look things up in a database before answering. Instead of relying only on what it has been trained on, the AI first searches a _vector database_ (a special kind of database that stores information in a way that makes it easy to find things that “mean” the same, not just things that look the same). This means the AI can pull in the most relevant facts and then generate a better, more accurate response for the user.

### Analogy
Think of the AI as a very skilled storyteller who sometimes forgets exact details. A vector database is like a super-organized library where the books are sorted by meaning instead of by title. With RAG, before telling the story, the AI quickly walks into that library, grabs the right books with the details it needs, and then weaves them into its answer. That way, the story is both creative _and_ factually grounded.

### Implementation
First, lets add the correct dependencies:
```gradle
	implementation 'org.springframework.ai:spring-ai-advisors-vector-store'
	implementation 'org.springframework.ai:spring-ai-vector-store'
```

Now, let's start our application:
```shell
./gradlew build
java -jar build/libs/ai-chat-poc-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

Let's add the configuration to use a in memory vector store:
```java
package com.wallway.ai_chat_poc;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VectorStoreConfig {

    @Bean
    public VectorStore simpleVectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}

```

Let's update our controller:
```java
package com.wallway.ai_chat_poc;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("api")
public class ChatController {

    private static final String DEFAULT_SYSTEM_PROMPT = """
            You are a virtual football coach, expert in football tactics, training, and player development.
            Be knowledgeable, motivating, and provide practical advice for players and teams.
            Use the provided context information when available to give accurate and detailed answers.
            """;

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public ChatController(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        this.vectorStore = vectorStore;

        var chatMemory = MessageWindowChatMemory.builder()
                .maxMessages(20)
                .build();

        this.chatClient = chatClientBuilder
                .defaultSystem(DEFAULT_SYSTEM_PROMPT)
                .defaultAdvisors(
                    MessageChatMemoryAdvisor.builder(chatMemory).build(),
                    QuestionAnswerAdvisor.builder(vectorStore).build()
                )
                .build();
    }

    @PostMapping("load")
    public void loadDataToVectorStore(@RequestBody String content) {
        vectorStore.add(List.of(new Document(content)));
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

Let's now go to [localhost](http://localhost:8080) and ask it what 4-3-3 means. As you can see it doesn't know from what we provided. 

Let's add some information to the vector store:
```shell
curl -X POST http://localhost:8080/api/load \
  -H "Content-Type: application/json" \
  -d '{"content": "4-3-3 Formation: This is an attacking formation with 4 defenders, 3 midfielders, and 3 forwards. The wingers provide width and can cut inside or cross. The central midfielder acts as a playmaker. This formation is great for teams that want to control possession and create many attacking opportunities."}'
```

As you can see it now searches the vector store, finds something and uses that. 
