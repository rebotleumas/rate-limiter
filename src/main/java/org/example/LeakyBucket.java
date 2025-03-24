package org.example;
import java.util.ArrayDeque;

public class LeakyBucket implements RateLimiter {
    private final int bucketSize;
    private final ArrayDeque<RequestContext> bucket;

    public LeakyBucket(int bucketSize) {
        this.bucketSize = bucketSize;
        bucket = new ArrayDeque<>();
    }

    @Override
    public int size() {
        return this.bucket.size();
    }

    @Override
    public synchronized boolean push(RequestContext rc) {
        if (this.bucket.size() == this.bucketSize) {
            return false;
        } else {
            this.bucket.addLast(rc);
            return true;
        }
    }

    @Override
    public synchronized RequestContext pull() {
        return this.bucket.pop();
    }

    @Override
    public void clear() {
        this.bucket.clear();
    }

    @Override
    public boolean isEmpty() {
        return this.bucket.isEmpty();
    }
}
