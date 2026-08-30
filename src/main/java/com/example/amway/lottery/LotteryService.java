package com.example.amway.lottery;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Service
public class LotteryService {

    private final StringRedisTemplate redisTemplate;
    private final List<Prize> prizes = new ArrayList<>();
    private final Random random = new Random();

    private static final String REDIS_PRIZE_PREFIX = "lottery:prize:";
    private static final String LUA_DECR_STOCK =
            "local current = tonumber(redis.call('get', KEYS[1]))\n" +
            "if current and current > 0 then\n" +
            "    redis.call('decr', KEYS[1])\n" +
            "    return 1\n" +
            "else\n" +
            "    return 0\n" +
            "end";

    private final DefaultRedisScript<Long> decrScript;

    public LotteryService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        // Prize A: iPhone 15 Pro, Quantity: 3, Prob: 1%
        prizes.add(new Prize("prize_a", "iPhone 15 Pro", 3, 0.01));
        // Prize B: iPad Air, Quantity: 10, Prob: 5%
        prizes.add(new Prize("prize_b", "iPad Air", 10, 0.05));
        // Prize C: AirPods Pro, Quantity: 20, Prob: 14%
        prizes.add(new Prize("prize_c", "AirPods Pro", 20, 0.14));

        this.decrScript = new DefaultRedisScript<>();
        this.decrScript.setScriptText(LUA_DECR_STOCK);
        this.decrScript.setResultType(Long.class);

        initializeRedisStock();
    }

    private void initializeRedisStock() {
        for (Prize prize : prizes) {
            String key = REDIS_PRIZE_PREFIX + prize.getId();
            redisTemplate.opsForValue().setIfAbsent(key, String.valueOf(prize.getInitialQuantity()));
        }
    }

    public List<Prize> getPrizes() {
        // Sync stocks from Redis dynamically
        for (Prize prize : prizes) {
            String key = REDIS_PRIZE_PREFIX + prize.getId();
            String stockStr = redisTemplate.opsForValue().get(key);
            if (stockStr != null) {
                try {
                    prize.setRemainingQuantity(Integer.parseInt(stockStr));
                } catch (NumberFormatException ignored) {}
            } else {
                // If not found in Redis, initialize and sync
                redisTemplate.opsForValue().set(key, String.valueOf(prize.getInitialQuantity()));
                prize.setRemainingQuantity(prize.getInitialQuantity());
            }
        }
        return Collections.unmodifiableList(prizes);
    }

    public DrawResult draw() {
        double r = random.nextDouble();
        double cumulativeProbability = 0.0;

        for (Prize prize : prizes) {
            cumulativeProbability += prize.getProbability();
            if (r < cumulativeProbability) {
                // Attempt atomic check-and-decrement in Redis via Lua script
                String key = REDIS_PRIZE_PREFIX + prize.getId();
                Long status = redisTemplate.execute(decrScript, Collections.singletonList(key));
                if (status != null && status == 1L) {
                    return new DrawResult(prize.getId(), prize.getName(), true);
                } else {
                    // Out of stock, fallback to Thank You
                    break;
                }
            }
        }
        // Fallback or explicit "Thank You"
        return new DrawResult("none", "銘謝惠顧", false);
    }

    public List<DrawResult> drawMultiple(int count) {
        if (count <= 0) {
            return Collections.emptyList();
        }
        List<DrawResult> results = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            results.add(draw());
        }
        return results;
    }

    public void reset() {
        for (Prize prize : prizes) {
            String key = REDIS_PRIZE_PREFIX + prize.getId();
            redisTemplate.opsForValue().set(key, String.valueOf(prize.getInitialQuantity()));
            prize.setRemainingQuantity(prize.getInitialQuantity());
        }
    }
}
