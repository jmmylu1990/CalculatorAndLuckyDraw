package com.example.amway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AmwayApplicationTests {

    private Calculator calculator;

    @Autowired
    private LotteryService lotteryService;

    @BeforeEach
    void setUp() {
        calculator = new Calculator();
        if (lotteryService != null) {
            lotteryService.reset();
        }
    }

    // --- Calculator Tests ---
    @Test
    void testCalculatorInitialValue() {
        assertEquals(0.0, calculator.getCurrentValue());
        assertFalse(calculator.isUndoAvailable());
        assertFalse(calculator.isRedoAvailable());
    }

    @Test
    void testCalculatorAddition() {
        calculator.executeCommand(new AddCommand(10.0));
        assertEquals(10.0, calculator.getCurrentValue());

        calculator.executeCommand(new AddCommand(5.5));
        assertEquals(15.5, calculator.getCurrentValue());

        List<String> history = calculator.getHistory();
        assertEquals(2, history.size());
        assertEquals("0 + 10 = 10", history.get(0));
        assertEquals("10 + 5.5 = 15.5", history.get(1));
    }

    @Test
    void testCalculatorSubtraction() {
        calculator.executeCommand(new SubtractCommand(5.0));
        assertEquals(-5.0, calculator.getCurrentValue());

        calculator.executeCommand(new SubtractCommand(-3.0));
        assertEquals(-2.0, calculator.getCurrentValue());
    }

    @Test
    void testCalculatorMultiplication() {
        calculator.executeCommand(new AddCommand(4.0));
        calculator.executeCommand(new MultiplyCommand(3.0));
        assertEquals(12.0, calculator.getCurrentValue());
    }

    @Test
    void testCalculatorDivision() {
        calculator.executeCommand(new AddCommand(10.0));
        calculator.executeCommand(new DivideCommand(4.0));
        assertEquals(2.5, calculator.getCurrentValue());
    }

    @Test
    void testCalculatorDivisionByZero() {
        calculator.executeCommand(new AddCommand(10.0));
        assertThrows(ArithmeticException.class, () -> {
            calculator.executeCommand(new DivideCommand(0.0));
        });
        assertEquals(10.0, calculator.getCurrentValue());
    }

    @Test
    void testCalculatorUndoRedo() {
        calculator.executeCommand(new AddCommand(10.0));
        calculator.executeCommand(new AddCommand(5.0));
        calculator.executeCommand(new MultiplyCommand(2.0));

        assertEquals(30.0, calculator.getCurrentValue());
        assertTrue(calculator.isUndoAvailable());
        assertFalse(calculator.isRedoAvailable());

        double val = calculator.undo();
        assertEquals(15.0, val);
        assertTrue(calculator.isUndoAvailable());
        assertTrue(calculator.isRedoAvailable());

        val = calculator.undo();
        assertEquals(10.0, val);

        val = calculator.undo();
        assertEquals(0.0, val);
        assertFalse(calculator.isUndoAvailable());

        val = calculator.redo();
        assertEquals(10.0, val);
        assertTrue(calculator.isUndoAvailable());

        val = calculator.redo();
        assertEquals(15.0, val);

        val = calculator.redo();
        assertEquals(30.0, val);
        assertFalse(calculator.isRedoAvailable());
    }

    // --- Lottery Concurrency & Overselling Prevention Tests ---
    @Test
    void testLotteryInitialStock() {
        List<Prize> prizes = lotteryService.getPrizes();
        assertEquals(3, prizes.size());
        assertEquals(3, prizes.get(0).getRemainingQuantity());  // iPhone
        assertEquals(10, prizes.get(1).getRemainingQuantity()); // iPad
        assertEquals(20, prizes.get(2).getRemainingQuantity()); // AirPods
    }

    @Test
    void testLotteryDrawSafety() {
        // Draw 100 times to exhaust the stock and check
        List<DrawResult> results = lotteryService.drawMultiple(100);
        assertEquals(100, results.size());

        // Verify remaining stock of each prize is non-negative
        for (Prize prize : lotteryService.getPrizes()) {
            assertTrue(prize.getRemainingQuantity() >= 0, "Prize quantity should not be negative");
        }

        // Count how many real prizes were won
        long realPrizesWon = results.stream().filter(DrawResult::isWon).count();
        // Total initial stock is 3 + 10 + 20 = 33
        assertTrue(realPrizesWon <= 33, "Prizes won should not exceed total initial stock of 33");
    }

    @Test
    void testLotteryHighConcurrencyDraw() throws Exception {
        int threadsCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadsCount);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadsCount);

        List<Future<DrawResult>> futures = new ArrayList<>();

        for (int i = 0; i < threadsCount; i++) {
            futures.add(executor.submit(() -> {
                latch.await(); // wait for start signal
                DrawResult result = lotteryService.draw();
                finishLatch.countDown();
                return result;
            }));
        }

        latch.countDown(); // Start all threads at once
        finishLatch.await(); // Wait for all threads to finish

        // Verify stock safety
        int totalRemainingStock = 0;
        for (Prize p : lotteryService.getPrizes()) {
            assertTrue(p.getRemainingQuantity() >= 0, "Prize " + p.getName() + " remaining stock must be >= 0");
            totalRemainingStock += p.getRemainingQuantity();
        }

        // Verify that won prizes plus remaining stock equals original total stock (33)
        long wonCount = 0;
        for (Future<DrawResult> f : futures) {
            if (f.get().isWon()) {
                wonCount++;
            }
        }

        assertEquals(33, wonCount + totalRemainingStock, "Total won prizes plus remaining stock must equal 33");
        executor.shutdown();
    }
}
