package com.example.amway;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class LotteryService {

    private final List<Prize> prizes = new ArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final Random random = new Random();

    public LotteryService() {
        // Prize A: iPhone 15 Pro, Quantity: 3, Prob: 1%
        prizes.add(new Prize("prize_a", "iPhone 15 Pro", 3, 0.01));
        // Prize B: iPad Air, Quantity: 10, Prob: 5%
        prizes.add(new Prize("prize_b", "iPad Air", 10, 0.05));
        // Prize C: AirPods Pro, Quantity: 20, Prob: 14%
        prizes.add(new Prize("prize_c", "AirPods Pro", 20, 0.14));
    }

    public List<Prize> getPrizes() {
        return Collections.unmodifiableList(prizes);
    }

    public DrawResult draw() {
        lock.lock();
        try {
            double r = random.nextDouble();
            double cumulativeProbability = 0.0;

            for (Prize prize : prizes) {
                cumulativeProbability += prize.getProbability();
                if (r < cumulativeProbability) {
                    // Try to win this prize
                    if (prize.decrementQuantity()) {
                        return new DrawResult(prize.getId(), prize.getName(), true);
                    } else {
                        // Out of stock, fallback to Thank You
                        break;
                    }
                }
            }
            // Fallback or explicit "Thank You"
            return new DrawResult("none", "銘謝惠顧", false);
        } finally {
            lock.unlock();
        }
    }

    public List<DrawResult> drawMultiple(int count) {
        if (count <= 0) {
            return Collections.emptyList();
        }
        List<DrawResult> results = new ArrayList<>();
        lock.lock();
        try {
            for (int i = 0; i < count; i++) {
                results.add(draw());
            }
            return results;
        } finally {
            lock.unlock();
        }
    }

    public void reset() {
        lock.lock();
        try {
            for (Prize prize : prizes) {
                prize.reset();
            }
        } finally {
            lock.unlock();
        }
    }
}
