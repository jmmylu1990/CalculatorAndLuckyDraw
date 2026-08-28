package com.example.amway;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

public class Prize implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String name;
    private final int initialQuantity;
    private final AtomicInteger remainingQuantity;
    private final double probability;

    public Prize(String id, String name, int initialQuantity, double probability) {
        this.id = id;
        this.name = name;
        this.initialQuantity = initialQuantity;
        this.remainingQuantity = new AtomicInteger(initialQuantity);
        this.probability = probability;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getInitialQuantity() {
        return initialQuantity;
    }

    public int getRemainingQuantity() {
        return remainingQuantity.get();
    }

    public double getProbability() {
        return probability;
    }

    public boolean decrementQuantity() {
        while (true) {
            int current = remainingQuantity.get();
            if (current <= 0) {
                return false;
            }
            if (remainingQuantity.compareAndSet(current, current - 1)) {
                return true;
            }
        }
    }

    public void reset() {
        remainingQuantity.set(initialQuantity);
    }
}
