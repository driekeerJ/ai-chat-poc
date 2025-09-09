# AI-chat-poc
## Create project
1. Go to `https://start.spring.io/`
2. Choose at least `openAI`, `Thymeleaf` and `Web`
3. Generate a project
4. Remove the test class (not for now)
5. Initialize the project as a git project so you can see the differences
	1. `git init`
	2. `git add .`
	3. `git commit -am "initial commit"`
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

Now start the application
```bash
./gradlew build
java -jar build/libs/ai-chat-poc-0.0.1-SNAPSHOT.jar
```
Ask:
```
What does football mean?
```

What do we do here?
- `defaultSystem` sets the default system message. A system message is a short instruction that tells the AI how it should behave or what role it should play during the conversation.
- `chatClient.prompt(scanner.nextLine())` just gets the text that you typed. This is also the trigger. 
- `call` will do the actual call
- `content` retrieves the 'content' out of the response model.

Now test it. Build the project and start it with 'local' settings:
```bash
./gradlew build
java -jar build/libs/ai-chat-poc-0.0.1-SNAPSHOT.jar
```

Just ask it a question and see what it response it with. 
```
git commit -am "added commandline bot"
```

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
java -jar build/libs/ai-chat-poc-0.0.1-SNAPSHOT.jar
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
```
git commit -am "added RestController"
```

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
java -jar build/libs/ai-chat-poc-0.0.1-SNAPSHOT.jar
```

And ask the same question but now on the stream endpoint:
```bash
curl -X POST http://localhost:8080/api/chat/stream \
  -H "Content-Type: application/json" \
  -d '{"prompt": "Tell me about football tactics in max 3 paragraphs?"}'
```

As you can see, the answer now comes in parts.
```
git commit -am "added streams"
```
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
java -jar build/libs/ai-chat-poc-0.0.1-SNAPSHOT.jar
```

And now open [localhost](http://localhost:8080), just ask it a question.
```
git add .
git commit -am "added frontend"
```

## Local memory
The AI doesn't remember previous conversations because each request is completely independent - it's like talking to someone who has amnesia and forgets everything after each sentence. To make it remember, we need to store the conversation history and send it along with each new message so the AI can see the full context of what was discussed before.

If you still have the application running, just tell it your name and then in the next prompt, ask it to tell you what your name is.

```
Hi my name is Jeroen
```

```
What was my name again?
```

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
java -jar build/libs/ai-chat-poc-0.0.1-SNAPSHOT.jar
```

Now lets try it again on [localhost](http://localhost:8080)
```
Hi my name is Jeroen
```

```
What was my name again?
```

Now commit it
```
git commit -am "added local memory"
```
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

Now, let's start our application:
```shell
./gradlew build
java -jar build/libs/ai-chat-poc-0.0.1-SNAPSHOT.jar
```

Let's now go to [localhost](http://localhost:8080) and ask:
```
What does it mean to play 17 over 22 within 30
```

As you see it doesn't respond correctly

Let's add some information to the vector store:
```shell
curl -X POST http://localhost:8080/api/load \
  -H "Content-Type: application/json" \
  -d '{"content": "To play 17 over 22 within 30 means to play in an attacking formation with 4 defenders, 3 midfielders, and 3 forwards. The wingers provide width and can cut inside or cross. The central midfielder acts as a playmaker. This formation is great for teams that want to control possession and create many attacking opportunities."}'
```

As you can see it now searches the vector store, finds something and uses that. 

```
git add .
git commit -am "added vector"
```
## Tooling
### Explanation
Tools are like little external helpers you connect to an AI. On its own, the AI doesn’t know things like what today’s date is or what the weather will be tomorrow. By giving it a tool, you let the AI call out to another system that _does_ know this, and then use that information in its answer. So tools extend what the AI can do by giving it access to real data or specific actions.

### Analogy
Think of the AI as a very smart colleague who can reason well but doesn’t have internet or a calendar. By giving them tools, it’s like handing them a phone to call the weather service, or a diary to check today’s date. Suddenly they can combine their intelligence with up-to-date information, and the answers become both clever _and_ correct.
### Implementation
Before we do this, in the running application on [localhost](http://localhost:8080), ask it 
```
what should I wear when I play coming Saturday against a team in Utrecht
```

As you can see, it doesn't know. Let use some tooling. 
First, let's create a folder 'tools' and add a DateTimeTool to get the datetime:
```java
package com.wallway.ai_chat_poc.tools;

import java.time.format.DateTimeFormatter;
import org.springframework.ai.tool.annotation.Tool;

public class DateTimeTools {
    @Tool(description = "Get the current date")
    public String getCurrentDateTime(String timeZone) {
        return java.time.ZonedDateTime.now(java.time.ZoneId.of(timeZone))
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
```

And a weather tool to get the temperature for a specific city on a specific day:

```java
package com.wallway.ai_chat_poc.tools;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

public class WeatherTools {

    private final RestTemplate restTemplate = new RestTemplate();

    @Tool(description = "Get weather forecast for a city on a specific date (format: YYYY-MM-DD)")
    public String getWeather(String city, String date) {
        try {
            // Convert city to coordinates using Geocoding API
            var encodedCity = UriUtils.encode(city, StandardCharsets.UTF_8);
            var geocodingUrl = URI.create("https://geocoding-api.open-meteo.com/v1/search?name=" +
                                         encodedCity + "&count=1");

            var geocodingResponse = restTemplate.exchange(
                geocodingUrl,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            var body = geocodingResponse.getBody();
            var results = (body != null) ? (List<?>) body.getOrDefault("results", Collections.emptyList()) : Collections.emptyList();
            if (results.isEmpty()) {
                return "City not found: " + city;
            }

            var location = (Map<?, ?>) results.get(0);
            var latitude = ((Number) location.get("latitude")).doubleValue();
            var longitude = ((Number) location.get("longitude")).doubleValue();
            var cityName = (String) location.get("name");

            // Get weather data from Open-Meteo API
            var weatherUrl = URI.create(
                "https://api.open-meteo.com/v1/forecast" +
                "?latitude=%s&longitude=%s".formatted(latitude, longitude) +
                "&daily=temperature_2m_max,temperature_2m_min" +
                "&timezone=auto" +
                "&start_date=%s&end_date=%s".formatted(date, date)
            );

            var weatherResponse = restTemplate.exchange(
                weatherUrl,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            var weatherData = weatherResponse.getBody();
            if (weatherData == null) {
                return "Failed to retrieve weather data";
            }

            var dailyData = (Map<?, ?>) weatherData.get("daily");
            var dailyUnits = (Map<?, ?>) weatherData.get("daily_units");

            if (dailyData == null || dailyUnits == null) {
                return "Weather data format is invalid";
            }

            var maxTempList = (List<?>) dailyData.get("temperature_2m_max");
            var minTempList = (List<?>) dailyData.get("temperature_2m_min");

            if (maxTempList == null || minTempList == null || maxTempList.isEmpty() || minTempList.isEmpty()) {
                return "Temperature data not available for the specified date";
            }

            var maxTemp = ((Number) maxTempList.get(0)).doubleValue();
            var minTemp = ((Number) minTempList.get(0)).doubleValue();
            var unit = (String) dailyUnits.get("temperature_2m_max");

            return """
                   Weather for %s on %s:
                   Min: %.1f%s, Max: %.1f%s
                   """.formatted(cityName, date, minTemp, unit, maxTemp, unit);

        } catch (Exception e) {
            return "Error fetching weather data: " + e.getMessage();
        }
    }
}
```

And now we just need to update the ChatController to use these tools:
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
import com.wallway.ai_chat_poc.tools.DateTimeTools;
import com.wallway.ai_chat_poc.tools.WeatherTools;

import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("api")
public class ChatController {

    private static final String DEFAULT_SYSTEM_PROMPT = """
            You are a virtual football coach, expert in football tactics, training, and player development.
            Be knowledgeable, motivating, and provide practical advice for players and teams.
            Use the provided context information when available to give accurate and detailed answers.
            Always retrieve the current date via tools when asked about dates in natural language.
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
                .defaultTools(
                        new DateTimeTools(), 
                        new WeatherTools()
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
I have also added a message to the system prompt to always use a new date. This is just in case. 

Alright, let's rebuild:
```shell
./gradlew build
java -jar build/libs/ai-chat-poc-0.0.1-SNAPSHOT.jar
```

And open [localhost](http://localhost:8080) again and ask the same question.
```
what should I wear when I play coming Saturday against a team in Utrecht
```
And now it works.

```
git add .
git commit -am "added tools"
```
## MCP
### Explanation
MCP (Model Context Protocol) is a standard way for AI systems to connect with external tools, databases, or apps. Instead of building a custom integration for each system, MCP gives AI a universal “language” to communicate. This makes it much easier to plug AI into different parts of a business without reinventing the wheel each time.
### Analogy
Think of MCP as a universal translator for the AI. Without it, the AI would need to learn every individual dialect of every system it talks to. With MCP, it speaks one language, and the translator takes care of the rest. That way, it can quickly and smoothly interact with many different systems.
### Implementation
#### TeamInformation Service
Let's say, we have a simple application running in our system that retrieves the team information. So, this is a different application than the chatbot application we are building. See [this github repo](https://github.com/driekeerJ/super-simple-mcp-service).
We have a method to get all the teams:

```java
package com.wallway.teaminformation;

import java.util.List;

import org.springframework.ai.tool.annotation.Tool;

public class TeamInformationService {

    @Tool(description = "Get a list of all teams")
    public List<Team> getAllTeams() {
        return List.of(new Team("1", "Flying Meatballs United", "Alice, Bob"),
                       new Team("2", "Banana Boots FC", "Charlie, David"),
                       new Team("3", "Goalpost Goblins", "Eve, Frank"));
    }

    public record Team(String id, String name, String members) {}
}
```
By using the `@Tool` annotation we make this known as a tool.

We make sure there is a ToolCallbackProvider. The bean exposes the methods of `TeamInformationService` as tools that can be used by Spring AI components.
```java
package com.wallway.teaminformation;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class TeaminformationApplication {

	public static void main(String[] args) {
		SpringApplication.run(TeaminformationApplication.class, args);
	}

	@Bean
	public ToolCallbackProvider teamTools(TeamInformationService teamInformationService) {
		return MethodToolCallbackProvider.builder().toolObjects(teamInformationService).build();
	}
}
```

And we use the following application properties:
```
spring.application.name=teaminformation
server.port=8082

# MCP Server Configuration
spring.ai.mcp.server.name=teaminformation-store-spring
spring.ai.mcp.server.version=1.0.0
logging.level.org.springframework.ai=DEBUG
```

For future reference, this is the build.gradle settings:
```gradle
plugins {
	id 'java'
	id 'org.springframework.boot' version '3.5.5'
	id 'io.spring.dependency-management' version '1.1.7'
}

group = 'com.wallway'
version = '0.0.1-SNAPSHOT'
description = 'Demo project for Spring Boot'

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

ext {
	set('springAiVersion', "1.0.1")
}

dependencies {
	implementation 'org.springframework.boot:spring-boot-starter-web'
	implementation 'org.springframework.ai:spring-ai-starter-mcp-server-webmvc'
	testImplementation 'org.springframework.boot:spring-boot-starter-test'
	testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}


dependencyManagement {
	imports {
		mavenBom "org.springframework.ai:spring-ai-bom:${springAiVersion}"
	}
}

tasks.named('test') {
	useJUnitPlatform()
}
```

Lets start this service
```shell
./gradlew build
java -jar build/libs/teaminformation-0.0.1-SNAPSHOT.jar
```
#### ChatBot
Now let's use this in our chatbot service:
Add the following dependency:
```
	implementation 'org.springframework.ai:spring-ai-starter-mcp-client'
```

The following property:
```
spring.ai.mcp.client.sse.connections.names.url=http://localhost:8082
```

And update the controller:
```java
package com.wallway.ai_chat_poc;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.wallway.ai_chat_poc.tools.DateTimeTools;
import com.wallway.ai_chat_poc.tools.WeatherTools;

import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("api")
public class ChatController {

    private static final String DEFAULT_SYSTEM_PROMPT = """
            You are a virtual football coach, expert in football tactics, training, and player development.
            Be knowledgeable, motivating, and provide practical advice for players and teams.
            Use the provided context information when available to give accurate and detailed answers.
            Always retrieve the current date via tools when asked about dates in natural language.
            """;

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public ChatController(ChatClient.Builder chatClientBuilder, VectorStore vectorStore, ToolCallbackProvider tools) {
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
                .defaultTools(
                        new DateTimeTools(), 
                        new WeatherTools()
                )
                .defaultToolCallbacks(tools)
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
Now build your application

```shell
./gradlew build
java -jar build/libs/ai-chat-poc-0.0.1-SNAPSHOT.jar
```

And lets go to [localhost](http://localhost:8080) and ask:
```
Give me a list of all teams
```

That's it.

## Using a local LLM
### Running a Local LLM Model with Docker

You can run a language model locally using the [Docker Model Runner](https://docs.docker.com/ai/model-runner/).  
This is useful for development and privacy, and it exposes an **OpenAI-compatible API** on `http://localhost:12434`.

1. **Enable the Docker Model Runner**  
Open Docker Desktop → **Settings** → **AI**  
- Enable **Docker Model Runner**  
- Enable **Host-side TCP endpoint** (default port: `12434`)

2. **Pull a model**  
For example, to pull the Gemma 3 model:
```bash
docker model pull ai/gemma3:latest
```

3. **Test the model**
Run a one-off prompt directly:

```bash
docker model run ai/gemma3:latest "Tell me a joke about Spring AI."
```

Or call the API endpoint:

```bash
curl http://localhost:12434/engines/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "ai/gemma3:latest",
    "messages":[{"role":"user","content":"Explain quantization in 1 sentence."}]
  }'
```

4. **Update your application configuration**
Point Spring AI to the local server in `application.yml`:
```yaml
spring:
  ai:
    model:
      chat: openai
    openai:
      base-url: http://localhost:12434
      chat:
        completions-path: /engines/v1/chat/completions
        options:
          model: ai/gemma3:latest
      api-key: dummy
```

Now your Spring AI application will use the local Docker model for chat and other tasks.

> **Tip:** Replace `ai/gemma3:latest` with any other supported model tag from [Docker Hub](https://hub.docker.com/r/ai).

### Important Note about Vector Store and Embeddings

When using a local LLM like `gemma3:latest` through Ollama, the **Vector Store functionality is not available** because:

1. **Missing Embedding Model**: Vector stores require an embedding model to convert text into numerical vectors for similarity search
2. **Ollama Limitations**: Most local LLM setups (including Ollama with `gemma3:latest`) only provide chat completions, not embedding endpoints
3. **API Incompatibility**: The Spring AI framework expects an OpenAI-compatible `/v1/embeddings` endpoint which local models typically don't provide

### Current Workaround
In the `ChatController`, the `QuestionAnswerAdvisor` (which uses the vector store) is commented out:

```java
.defaultAdvisors(
    MessageChatMemoryAdvisor.builder(chatMemory).build()
    // QuestionAnswerAdvisor.builder(vectorStore).build() // Disabled for local LLM
)
```

### To Use Vector Store Features
If you need vector store functionality, you have these options:
1. Use OpenAI's hosted API (with embeddings support)
2. Set up a separate embedding service (like `text-embedding-ada-002` via OpenAI API)
3. Use a local embedding model setup that provides OpenAI-compatible endpoints

For development and basic chat functionality, the current setup works perfectly without vector store features.