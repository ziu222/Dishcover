package com.dishcover.rag.service;

import com.dishcover.rag.llm.ConversationTurn;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Lịch sử hội thoại in-memory + TTL (specs/rag-service.md mục 3.8) — không cần Caffeine/Redis ở
 * quy mô đồ án. TTL enforce qua {@code @Scheduled} quét định kỳ (KHÔNG chỉ check-lười lúc đọc —
 * check-lười-only sẽ rò rỉ bộ nhớ với conversationId user bỏ ngang không hỏi lại nữa); đường đọc
 * vẫn tự kiểm tra hết hạn (double-check) để đúng hành vi giữa 2 lần quét.
 */
@Component
public class ConversationHistoryStore {

    private static final Duration TTL = Duration.ofMinutes(30);
    private static final int MAX_TURNS = 10; // giới hạn per-conversation, chặn phình bộ nhớ

    private final ConcurrentHashMap<String, Entry> store = new ConcurrentHashMap<>();

    private static final class Entry {
        final List<ConversationTurn> turns = new CopyOnWriteArrayList<>();
        final AtomicReference<Instant> lastAccess = new AtomicReference<>(Instant.now());
    }

    public void append(String conversationId, String role, String text) {
        Entry entry = store.computeIfAbsent(conversationId, id -> new Entry());
        entry.lastAccess.set(Instant.now());
        entry.turns.add(new ConversationTurn(role, text, Instant.now()));
        while (entry.turns.size() > MAX_TURNS) {
            entry.turns.remove(0);
        }
    }

    public List<ConversationTurn> recentTurns(String conversationId) {
        if (conversationId == null) {
            return List.of();
        }
        Entry entry = store.get(conversationId);
        if (entry == null || isExpired(entry)) {
            return List.of();
        }
        entry.lastAccess.set(Instant.now());
        return List.copyOf(entry.turns);
    }

    private boolean isExpired(Entry entry) {
        return Duration.between(entry.lastAccess.get(), Instant.now()).compareTo(TTL) > 0;
    }

    @Scheduled(fixedDelay = 5 * 60 * 1000)
    void evictExpired() {
        store.entrySet().removeIf(e -> isExpired(e.getValue()));
    }
}
