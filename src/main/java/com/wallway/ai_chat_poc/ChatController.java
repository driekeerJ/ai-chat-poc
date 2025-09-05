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