package com.dishcover.rag.llm;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** KHÔNG gọi Gemini thật — mock ChatModel (SPI dưới ChatClient), build ChatClient thật trên đó. */
class GeminiProviderTest {

    @Test
    void completeRoundTripsThroughRealChatClientOnMockedModel() {
        ChatModel mockModel = mock(ChatModel.class);
        Generation generation = new Generation(new AssistantMessage("Món gợi ý: Trứng chiên cà chua"));
        ChatResponse canned = new ChatResponse(List.of(generation), ChatResponseMetadata.builder().build());
        when(mockModel.call(any(Prompt.class))).thenReturn(canned);

        GeminiProvider provider = new GeminiProvider(ChatClient.builder(mockModel));

        String result = provider.complete("tôi có trứng và cà chua, nấu gì được?");

        assertEquals("Món gợi ý: Trứng chiên cà chua", result);
    }
}
