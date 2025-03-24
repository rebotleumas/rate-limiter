package org.example;

import java.net.http.HttpRequest;

public interface RateLimiter {
    public boolean push(RequestContext rc);
    public void clear();
    public RequestContext pull();
    public boolean isEmpty();
    public int size();
}
