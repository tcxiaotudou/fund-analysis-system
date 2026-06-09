package com.fund.analysis.client;

import com.fund.analysis.exception.ExternalApiException;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

@Component
public class ExternalApiClient {

    /**
     * 默认连接超时时间，单位毫秒
     */
    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 5000;

    /**
     * 默认连接池取连接超时时间，单位毫秒
     */
    private static final int DEFAULT_CONNECTION_REQUEST_TIMEOUT_MS = 5000;

    /**
     * 默认响应读取超时时间，单位毫秒
     */
    private static final int DEFAULT_SOCKET_TIMEOUT_MS = 30000;

    /**
     * JSON 序列化工具
     */
    private final Gson gson = new Gson();

    /**
     * HTTP 请求配置
     */
    private final RequestConfig requestConfig;

    /**
     * 使用默认超时配置创建客户端
     */
    public ExternalApiClient() {
        this(DEFAULT_CONNECT_TIMEOUT_MS, DEFAULT_CONNECTION_REQUEST_TIMEOUT_MS, DEFAULT_SOCKET_TIMEOUT_MS);
    }

    /**
     * 使用指定超时配置创建客户端
     *
     * @param connectTimeoutMs 连接超时时间
     * @param connectionRequestTimeoutMs 取连接超时时间
     * @param socketTimeoutMs 响应读取超时时间
     */
    ExternalApiClient(int connectTimeoutMs, int connectionRequestTimeoutMs, int socketTimeoutMs) {
        this.requestConfig = RequestConfig.custom()
                .setConnectTimeout(connectTimeoutMs)
                .setConnectionRequestTimeout(connectionRequestTimeoutMs)
                .setSocketTimeout(socketTimeoutMs)
                .build();
    }

    /**
     * 执行第三方 GET 请求
     *
     * @param url 请求地址
     * @return 响应正文
     */
    public String get(String url) {
        HttpGet request = new HttpGet(url);
        request.setConfig(requestConfig);
        try (CloseableHttpClient client = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();
             CloseableHttpResponse response = client.execute(request)) {
            return readResponse(url, response);
        } catch (IOException e) {
            throw new ExternalApiException("第三方 GET 请求失败: " + url, e);
        }
    }

    /**
     * 执行第三方 JSON POST 请求
     *
     * @param url 请求地址
     * @param body 请求体
     * @return 响应正文
     */
    public String postJson(String url, Object body) {
        HttpPost request = new HttpPost(url);
        request.setConfig(requestConfig);
        request.setHeader("Content-Type", "application/json");
        request.setHeader("Accept", "*/*");
        if (body != null) {
            request.setEntity(new StringEntity(gson.toJson(body), StandardCharsets.UTF_8));
        }

        try (CloseableHttpClient client = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();
             CloseableHttpResponse response = client.execute(request)) {
            return readResponse(url, response);
        } catch (IOException e) {
            throw new ExternalApiException("第三方 POST 请求失败: " + url, e);
        }
    }

    public JsonElement getJson(String url) {
        return parseJson(url, get(url));
    }

    public JsonElement postJsonElement(String url, Object body) {
        return parseJson(url, postJson(url, body));
    }

    private String readResponse(String url, CloseableHttpResponse response) throws IOException {
        int statusCode = response.getStatusLine().getStatusCode();
        HttpEntity entity = response.getEntity();
        String body = entity == null ? "" : EntityUtils.toString(entity, StandardCharsets.UTF_8);

        if (statusCode < 200 || statusCode >= 300) {
            throw new ExternalApiException("第三方接口返回异常状态码 " + statusCode + ": " + url);
        }
        if (body == null || body.trim().isEmpty()) {
            throw new ExternalApiException("第三方接口返回空响应: " + url);
        }
        return body;
    }

    private JsonElement parseJson(String url, String body) {
        try (JsonReader reader = new JsonReader(new StringReader(body))) {
            reader.setLenient(false);
            JsonElement json = JsonParser.parseReader(reader);
            if (reader.peek() != JsonToken.END_DOCUMENT) {
                throw new ExternalApiException("第三方接口返回非法 JSON: " + url);
            }
            if (!json.isJsonObject() && !json.isJsonArray()) {
                throw new ExternalApiException("第三方接口返回非法 JSON 结构: " + url);
            }
            return json;
        } catch (RuntimeException e) {
            throw new ExternalApiException("第三方接口返回非法 JSON: " + url, e);
        } catch (IOException e) {
            throw new ExternalApiException("第三方接口返回非法 JSON: " + url, e);
        }
    }
}
