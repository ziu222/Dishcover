package com.dishcover.notification.client;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * UserClient tự set ClientHttpRequestFactory riêng (timeout, xem UserClient constructor) — ghi đè
 * mất factory mà MockRestServiceServer.bindTo(RestClient.Builder) cài vào, nên không dùng được
 * pattern MockRestServiceServer như MatchingServiceTest. Dùng HttpServer JDK thuần (không thêm
 * dependency) để có 1 server HTTP thật, nhẹ, chạy cục bộ cho từng test.
 */
class UserClientTest {

    private static final String SECRET = "internal-secret-test";

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    private UserClient buildClient(HttpHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", handler);
        server.start();
        return new UserClient(RestClient.builder(), "http://localhost:" + server.getAddress().getPort(), SECRET);
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        if (body == null) {
            exchange.sendResponseHeaders(status, -1);
        } else {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
        }
        exchange.close();
    }

    @Test
    void getEmailReturnsEmailOnSuccessAndSendsInternalSecretHeader() throws IOException {
        AtomicReference<String> capturedSecret = new AtomicReference<>();
        UserClient client = buildClient(exchange -> {
            capturedSecret.set(exchange.getRequestHeaders().getFirst("X-Internal-Secret"));
            respond(exchange, 200, "{\"id\":36,\"email\":\"user36@test.com\"}");
        });

        String email = client.getEmail(36L);

        assertEquals("user36@test.com", email);
        assertEquals(SECRET, capturedSecret.get());
    }

    @Test
    void getEmailReturnsNullOn404NotThrown() throws IOException {
        UserClient client = buildClient(exchange -> respond(exchange, 404, null));

        assertNull(client.getEmail(999L));
    }

    @Test
    void getEmailReturnsNullOnServerError() throws IOException {
        UserClient client = buildClient(exchange -> respond(exchange, 500, null));

        assertNull(client.getEmail(1L));
    }
}
