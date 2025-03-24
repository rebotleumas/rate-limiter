package org.example;

import com.sun.net.httpserver.HttpExchange;

import java.net.http.HttpRequest;

public record RequestContext(HttpRequest request, HttpExchange exchange) {
}
