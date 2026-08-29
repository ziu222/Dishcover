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
    private static final int MAX_CONVERSATIONS = 5000; // chặn tổng số conversationId tracked cùng lúc

    private final ConcurrentHashMap<String, Entry> store = new ConcurrentHashMap<>();

    private static final class Entry {
        final List<ConversationTurn> turns = new CopyOnWriteArrayList<>();
        final AtomicReference<Instant> lastAccess = new AtomicReference<>(Instant.now());
    }

    /**
     * Ghi thêm 1 lượt hội thoại. Nếu entry cũ của {@code conversationId} đã hết TTL, entry được
     * THAY MỚI (không nối tiếp lịch sử quá hạn). Bỏ qua ghi nếu store đã đầy
     * ({@link #MAX_CONVERSATIONS}) và đây là conversationId mới. Chỉ giữ tối đa
     * {@link #MAX_TURNS} lượt gần nhất mỗi conversation.
     *
     * @param conversationId id hội thoại (client tự sinh)
     * @param role           "user" hoặc "assistant"
     * @param text           nội dung lượt chat
     */
    public void append(String conversationId, String role, String text) {
        if (!store.containsKey(conversationId) && store.size() >= MAX_CONVERSATIONS) {
            return; // đầy -- bỏ qua lưu lịch sử cho conversation MỚI thay vì phình bộ nhớ vô hạn
        }
        // compute (không phải computeIfAbsent): entry cũ đã hết TTL phải được THAY MỚI, không nối
        // tiếp lịch sử cũ -- computeIfAbsent sẽ giữ nguyên entry cũ (chỉ recentTurns() lọc theo TTL,
        // append() không hề kiểm tra) khiến hội thoại "hồi sinh" lịch sử quá hạn.
        Entry entry = store.compute(conversationId, (id, existing) ->
                (existing == null || isExpired(existing)) ? new Entry() : existing);
        entry.lastAccess.set(Instant.now());
        // synchronized trên entry: 2 request cùng conversationId ghi đồng thời có thể vượt
        // MAX_TURNS nhất thời hoặc cùng remove(0) nếu không khoá — entry ổn định theo compute()
        // ở trên nên khoá trên chính nó không ảnh hưởng conversationId khác.
        synchronized (entry) {
            entry.turns.add(new ConversationTurn(role, text, Instant.now()));
            while (entry.turns.size() > MAX_TURNS) {
                entry.turns.remove(0);
            }
        }
    }

    /**
     * Lấy các lượt hội thoại gần nhất còn hiệu lực của 1 conversation.
     *
     * @param conversationId id hội thoại, có thể {@code null}
     * @return danh sách lượt chat theo thứ tự thời gian; rỗng nếu {@code conversationId} là
     *         {@code null}, chưa từng có, hoặc đã hết TTL
     */
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
