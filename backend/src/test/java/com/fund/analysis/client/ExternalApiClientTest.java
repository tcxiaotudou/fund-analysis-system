package com.fund.analysis.client;

import com.fund.analysis.exception.ExternalApiException;
import com.google.gson.JsonElement;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExternalApiClientTest {

    private ExternalApiClient client;
    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void setUp() throws IOException {
        client = new ExternalApiClient();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();

        server.createContext("/ok", exchange -> write(exchange, 200, "{\"code\":0}"));
        server.createContext("/error", exchange -> write(exchange, 502, "{\"message\":\"bad\"}"));
        server.createContext("/empty", exchange -> write(exchange, 200, ""));
        server.createContext("/invalid-json", exchange -> write(exchange, 200, "not-json"));
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void parsesJsonResponse() {
        JsonElement json = client.getJson(baseUrl + "/ok");

        assertEquals(0, json.getAsJsonObject().get("code").getAsInt());
    }

    @Test
    void throwsOnNon2xxStatus() {
        assertThrows(ExternalApiException.class, () -> client.get(baseUrl + "/error"));
    }

    @Test
    void throwsOnEmptyResponse() {
        assertThrows(ExternalApiException.class, () -> client.get(baseUrl + "/empty"));
    }

    @Test
    void throwsOnInvalidJson() {
        assertThrows(ExternalApiException.class, () -> client.getJson(baseUrl + "/invalid-json"));
    }

    private void write(com.sun.net.httpserver.HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
