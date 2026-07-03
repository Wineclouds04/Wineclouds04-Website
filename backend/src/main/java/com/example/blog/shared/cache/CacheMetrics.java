package com.example.blog.shared.cache;

import java.util.concurrent.atomic.LongAdder;

import org.springframework.stereotype.Component;

@Component
public class CacheMetrics {

    private final LongAdder hits = new LongAdder();
    private final LongAdder misses = new LongAdder();
    private final LongAdder errors = new LongAdder();

    void hit() {
        hits.increment();
    }

    void miss() {
        misses.increment();
    }

    void error() {
        errors.increment();
    }

    public Snapshot snapshot() {
        long hitCount = hits.sum();
        long missCount = misses.sum();
        long requests = hitCount + missCount;
        double hitRate = requests == 0 ? 0 : (double) hitCount / requests;
        return new Snapshot(hitCount, missCount, errors.sum(), hitRate);
    }

    public record Snapshot(long hits, long misses, long errors, double hitRate) {
    }
}
