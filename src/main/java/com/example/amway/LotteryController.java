package com.example.amway;

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
