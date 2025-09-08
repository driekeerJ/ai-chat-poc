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