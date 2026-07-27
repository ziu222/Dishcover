package com.dishcover.rag.service;

import com.dishcover.rag.llm.ConversationTurn;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConversationHistoryStoreTest {

    private final ConversationHistoryStore store = new ConversationHistoryStore();

    @Test
    void unknownConversationIdReturnsEmpty() {
        assertTrue(store.recentTurns("never-seen").isEmpty());
    }

    @Test
    void nullConversationIdReturnsEmpty() {
        assertTrue(store.recentTurns(null).isEmpty());
    }

    @Test
    void appendThenReadReturnsTurnsInOrder() {
        String id = "conv-1";
        store.append(id, "user", "tôi có trứng");
        store.append(id, "assistant", "nấu trứng chiên nhé");

        List<ConversationTurn> turns = store.recentTurns(id);

        assertEquals(2, turns.size());
        assertEquals("user", turns.get(0).role());
        assertEquals("tôi có trứng", turns.get(0).text());
        assertEquals("assistant", turns.get(1).role());
    }

    @Test
    void trimsToMaxTurnsPerConversation() {
        String id = "conv-2";
        for (int i = 0; i < 15; i++) {
            store.append(id, "user", "turn " + i);
        }

        List<ConversationTurn> turns = store.recentTurns(id);

        assertEquals(10, turns.size()); // MAX_TURNS
        assertEquals("turn 5", turns.get(0).text()); // 5 turn đầu bị trim, còn lại 10 turn gần nhất
        assertEquals("turn 14", turns.get(9).text());
    }
}
