package se.samueltober.ratelimiter;

import se.samueltober.ratelimiter.http.RequestContext;

public interface RateLimiter {
    public boolean push(RequestContext rc);
    public void clear();
    public RequestContext pull();
    public boolean isEmpty();
    public int size();
}
