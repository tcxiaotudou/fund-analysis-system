package com.fund.analysis.client;

import com.fund.analysis.exception.ExternalApiException;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import org.apache.http.HttpEntity;
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

    private final Gson gson = new Gson();

    public String get(String url) {
        HttpGet request = new HttpGet(url);
        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(request)) {
            return readResponse(url, response);
        } catch (IOException e) {
            throw new ExternalApiException("第三方 GET 请求失败: " + url, e);
        }
    }

    public String postJson(String url, Object body) {
        HttpPost request = new HttpPost(url);
        request.setHeader("Content-Type", "application/json");
        request.setHeader("Accept", "*/*");
        if (body != null) {
            request.setEntity(new StringEntity(gson.toJson(body), StandardCharsets.UTF_8));
        }

        try (CloseableHttpClient client = HttpClients.createDefault();
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
