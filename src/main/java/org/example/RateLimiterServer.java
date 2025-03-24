package org.example;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RateLimiterServer {
    private final HttpServer server;
    private final RateLimiter rateLimiter;
    private final ScheduledExecutorService executor;
    private final int rate;

    public RateLimiterServer(int rate) throws IOException {
        this.rate = rate;
        this.rateLimiter = new LeakyBucket(10);
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 8080), 0);
        this.server.createContext("/rate-limit", new Handler(rateLimiter));
        this.server.setExecutor(Executors.newFixedThreadPool(1));
        this.executor = Executors.newSingleThreadScheduledExecutor();
    }

    public void start() {
        this.server.start();
        System.out.println("Rate limiter running on 8080");
        this.executor.scheduleAtFixedRate(new RequestProcessor(this.rateLimiter), 0, this.rate, TimeUnit.SECONDS);

        try {
            Thread.currentThread().join();
        } catch (Exception e) {
            System.out.println(e);
            throw new RuntimeException(e);
        }
    }

    static class RequestProcessor implements Runnable {
        private final RateLimiter rateLimiter;
        private final HttpClient client;
        public RequestProcessor(RateLimiter rateLimiter) {
            this.rateLimiter = rateLimiter;
            this.client = HttpClient.newHttpClient();
        }
        @Override
        public void run() {
            try {
                if (!this.rateLimiter.isEmpty()) {
                    RequestContext rc = this.rateLimiter.pull();
                    System.out.println("Processing request " + rc.request());

                    HttpResponse<String> response = this.client.send(rc.request(), HttpResponse.BodyHandlers.ofString());
                    HttpExchange exchange = rc.exchange();
                    exchange.sendResponseHeaders(200, response.body().getBytes().length);

                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(response.body().getBytes());
                        exchange.close();
                    } catch (IOException e) {
                        System.out.println(e);
                        throw new RuntimeException(e);
                    }
                }
            } catch (IOException | InterruptedException e) {
                System.out.println(e);
            }
        }
    };

    static class Handler implements HttpHandler {
        private final RateLimiter rateLimiter;

        public Handler(RateLimiter rateLimiter) {
            this.rateLimiter = rateLimiter;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                // Read the request body if present
                byte[] requestBody = exchange.getRequestBody().readAllBytes();
                String targetUrl = "https://jsonplaceholder.typicode.com" + exchange.getRequestURI().getPath();
                if (exchange.getRequestURI().getQuery() != null) {
                    targetUrl += "?" + exchange.getRequestURI().getQuery();
                }

                // Build the request with the same method and headers
                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create(targetUrl))
                        .method(exchange.getRequestMethod(),
                                requestBody.length > 0 ?
                                        HttpRequest.BodyPublishers.ofByteArray(requestBody) :
                                        HttpRequest.BodyPublishers.noBody());

                // Copy relevant headers
                exchange.getRequestHeaders().forEach((key, values) -> {
                    if (!key.equalsIgnoreCase("Host")) { // Skip the Host header
                        values.forEach(value -> requestBuilder.header(key, value));
                    }
                });

                HttpRequest request = requestBuilder.build();
                OutputStream os = exchange.getResponseBody();
                boolean accepted = this.rateLimiter.push(new RequestContext(request, exchange));

                String response;
                if (!accepted) {
                    response = "Too many requests";
                    exchange.sendResponseHeaders(429, response.length());
                    try {
                        os.write(response.getBytes());
                        exchange.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            } catch (Exception e) {
                String errorMessage = "Error processing request: " + e.getMessage();
                exchange.sendResponseHeaders(500, errorMessage.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(errorMessage.getBytes());
                }
                exchange.close();
            }
        }
    }}
