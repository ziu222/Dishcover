package com.dishcover.matching.repository;

import com.dishcover.matching.dto.IndexDtos.VectorMatch;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * JdbcTemplate mock thay vì Postgres thật -- H2 dùng cho test module này (application-test.yml)
 * không hỗ trợ kiểu {@code vector}/toán tử {@code <=>} của pgvector nên không dựng được bảng thật ở
 * đây; live-verify với Postgres thật (docker-setup/) mới xác nhận SQL chạy đúng trên engine thật.
 * Test này chỉ khóa đúng: SQL/tham số gửi xuống JdbcTemplate và cách map lại kết quả.
 */
class RecipeEmbeddingRepositoryTest {

    private final JdbcTemplate jdbc = Mockito.mock(JdbcTemplate.class);
    private final RecipeEmbeddingRepository repo = new RecipeEmbeddingRepository(jdbc, new ObjectMapper());

    @Test
    void upsertDeletesOldRowThenInsertsWithoutMetadataColumnWhenMetadataNull() {
        repo.upsert("r1", "noi dung", new float[]{0.1f, 0.25f}, null);

        verify(jdbc).update("DELETE FROM recipe_embeddings WHERE recipe_id = ?", "r1");
        ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).update(
                eq("INSERT INTO recipe_embeddings (recipe_id, content, embedding) VALUES (?, ?, CAST(? AS vector))"),
                args.capture());
        assertEquals("r1", args.getValue()[0]);
        assertEquals("noi dung", args.getValue()[1]);
        assertEquals("[0.1,0.25]", args.getValue()[2]);
    }

    @Test
    void upsertIncludesMetadataColumnAsJsonWhenPresent() {
        repo.upsert("r2", "noi dung", new float[]{1f}, Map.of("name", "Pho bo"));

        ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).update(
                eq("INSERT INTO recipe_embeddings (recipe_id, content, metadata, embedding) VALUES (?, ?, CAST(? AS jsonb), CAST(? AS vector))"),
                args.capture());
        assertEquals("r2", args.getValue()[0]);
        assertEquals("noi dung", args.getValue()[1]);
        assertEquals("{\"name\":\"Pho bo\"}", args.getValue()[2]);
        assertEquals("[1.0]", args.getValue()[3]);
    }

    @Test
    void findNearestBuildsVectorLiteralAndMapsRows() throws Exception {
        ResultSet rs = Mockito.mock(ResultSet.class);
        when(rs.getString("recipe_id")).thenReturn("r9");
        when(rs.getDouble("similarity")).thenReturn(0.87);

        when(jdbc.query(anyString(), any(RowMapper.class), any(), any(), any()))
                .thenAnswer(invocation -> {
                    RowMapper<VectorMatch> mapper = invocation.getArgument(1);
                    return List.of(mapper.mapRow(rs, 0));
                });

        List<VectorMatch> result = repo.findNearest(new float[]{0.5f, 0.5f}, 5);

        assertEquals(1, result.size());
        assertEquals("r9", result.get(0).recipeId());
        assertEquals(0.87, result.get(0).similarity());
        verify(jdbc).query(anyString(), any(RowMapper.class), eq("[0.5,0.5]"), eq("[0.5,0.5]"), eq(5));
    }
}
