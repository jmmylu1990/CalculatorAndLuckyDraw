package com.example.amway.lottery;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Controller
public class LotteryController {

    private final LotteryService lotteryService;

    public LotteryController(LotteryService lotteryService) {
        this.lotteryService = lotteryService;
    }

    @GetMapping("/lottery")
    public String lotteryPage(Model model) {
        model.addAttribute("prizes", lotteryService.getPrizes());
        return "lottery";
    }

    @PostMapping("/api/lottery/draw")
    @ResponseBody
    public DrawResponse draw(@RequestParam(value = "count", defaultValue = "1") int count, HttpSession session) {
        if (count < 1 || count > 10) {
            return new DrawResponse("error", "單次抽獎次數限制為 1 至 10 次", null, null);
        }

        // Prevent double submit within the same user session
        synchronized (session) {
            if (Boolean.TRUE.equals(session.getAttribute("drawing_lock"))) {
                return new DrawResponse("error", "抽獎正在進行中，請勿重複提交", null, null);
            }
            session.setAttribute("drawing_lock", true);
        }

        try {
            // Execute drawings
            List<DrawResult> results = lotteryService.drawMultiple(count);
            
            // Build DTOs for remaining stock display
            List<PrizeDto> remainingPrizes = new ArrayList<>();
            for (Prize p : lotteryService.getPrizes()) {
                remainingPrizes.add(new PrizeDto(p.getId(), p.getName(), p.getRemainingQuantity(), p.getInitialQuantity()));
            }
            
            return new DrawResponse("success", "", results, remainingPrizes);
        } finally {
            synchronized (session) {
                session.removeAttribute("drawing_lock");
            }
        }
    }

    @PostMapping("/api/lottery/reset")
    @ResponseBody
    public DrawResponse reset() {
        lotteryService.reset();
        
        List<PrizeDto> remainingPrizes = new ArrayList<>();
        for (Prize p : lotteryService.getPrizes()) {
            remainingPrizes.add(new PrizeDto(p.getId(), p.getName(), p.getRemainingQuantity(), p.getInitialQuantity()));
        }
        
        return new DrawResponse("success", "獎品數量重設成功", null, remainingPrizes);
    }

    @PostMapping("/api/lottery/simulate-concurrent")
    @ResponseBody
    public java.util.Map<String, Object> simulateConcurrent() {
        int numThreads = 100;
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(numThreads);
        java.util.concurrent.CountDownLatch startLatch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch finishLatch = new java.util.concurrent.CountDownLatch(numThreads);

        java.util.List<DrawResult> results = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        java.util.concurrent.atomic.AtomicInteger successCount = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicInteger failCount = new java.util.concurrent.atomic.AtomicInteger(0);

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for start signal
                    DrawResult result = lotteryService.draw();
                    results.add(result);
                    if (result.isWon()) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        long startTime = System.nanoTime();
        // Fire all threads simultaneously!
        startLatch.countDown();
        try {
            finishLatch.await(); // Wait for all threads to finish
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        long durationMs = (System.nanoTime() - startTime) / 1_000_000;
        executor.shutdown();

        // Calculate statistics
        long iphoneWon = results.stream().filter(r -> "prize_a".equals(r.getPrizeId())).count();
        long ipadWon = results.stream().filter(r -> "prize_b".equals(r.getPrizeId())).count();
        long airpodsWon = results.stream().filter(r -> "prize_c".equals(r.getPrizeId())).count();
        long noneCount = results.stream().filter(r -> "none".equals(r.getPrizeId())).count();

        // Verify remaining prizes list to return to frontend
        java.util.List<PrizeDto> remainingPrizes = new java.util.ArrayList<>();
        for (Prize p : lotteryService.getPrizes()) {
            remainingPrizes.add(new PrizeDto(p.getId(), p.getName(), p.getRemainingQuantity(), p.getInitialQuantity()));
        }

        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("totalRequests", numThreads);
        response.put("successCount", successCount.get());
        response.put("failCount", failCount.get());
        response.put("durationMs", durationMs);
        response.put("iphoneWon", iphoneWon);
        response.put("ipadWon", ipadWon);
        response.put("airpodsWon", airpodsWon);
        response.put("noneCount", noneCount);
        response.put("remainingPrizes", remainingPrizes);

        return response;
    }

    // Response and DTO Classes
    public static class DrawResponse implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String status;
        private final String message;
        private final List<DrawResult> results;
        private final List<PrizeDto> remainingPrizes;

        public DrawResponse(String status, String message, List<DrawResult> results, List<PrizeDto> remainingPrizes) {
            this.status = status;
            this.message = message;
            this.results = results;
            this.remainingPrizes = remainingPrizes;
        }

        public String getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }

        public List<DrawResult> getResults() {
            return results;
        }

        public List<PrizeDto> getRemainingPrizes() {
            return remainingPrizes;
        }
    }

    public static class PrizeDto implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String id;
        private final String name;
        private final int remainingQuantity;
        private final int initialQuantity;

        public PrizeDto(String id, String name, int remainingQuantity, int initialQuantity) {
            this.id = id;
            this.name = name;
            this.remainingQuantity = remainingQuantity;
            this.initialQuantity = initialQuantity;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public int getRemainingQuantity() {
            return remainingQuantity;
        }

        public int getInitialQuantity() {
            return initialQuantity;
        }
    }
}
