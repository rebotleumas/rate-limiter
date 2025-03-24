package org.example;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client {
    public static void main(String[] args) {
        HttpTestClient testClient = new HttpTestClient();
        int n = 50;
        ExecutorService pool = Executors.newFixedThreadPool(n);
        for (int i = 0; i < n; i++) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            pool.execute(() -> {
                try {
                    HttpResponse<String> response = testClient.sendRequest();
                    System.out.printf("Request from thread %s: %s %s%n", Thread.currentThread().getName() ,response.statusCode(), response.body());
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        pool.shutdown();
    }

    static class HttpTestClient {
        private final HttpClient httpClient;
        public HttpTestClient() {
            this.httpClient = HttpClient.newHttpClient();
        }

        public HttpResponse<String> sendRequest() throws URISyntaxException {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("http://127.0.0.1:8080/rate-limit"))
                    .version(HttpClient.Version.HTTP_1_1)
                    .GET()
                    .build();
            try {
                return this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
