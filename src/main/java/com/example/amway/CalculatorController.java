package com.example.amway;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class CalculatorController {

    private static final String CALCULATOR_SESSION_KEY = "calculator";

    private Calculator getCalculator(HttpSession session) {
        Calculator calculator = (Calculator) session.getAttribute(CALCULATOR_SESSION_KEY);
        if (calculator == null) {
            calculator = new Calculator();
            session.setAttribute(CALCULATOR_SESSION_KEY, calculator);
        }
        return calculator;
    }

    private CalculatorResponse buildResponse(Calculator calculator) {
        String pendingOp = calculator.isRedoAvailable() ? calculator.getRedoOperator() : null;
        return new CalculatorResponse(
                format(calculator.getCurrentValue()),
                calculator.getHistory(),
                calculator.isUndoAvailable(),
                calculator.isRedoAvailable(),
                pendingOp
        );
    }

    private String format(double d) {
        if (Double.isInfinite(d) || Double.isNaN(d)) {
            return String.valueOf(d);
        }
        if (d == (long) d) {
            return String.format("%d", (long) d);
        }
        return String.format("%s", d);
    }

    @GetMapping("/")
    public String index(HttpSession session, Model model) {
        Calculator calculator = getCalculator(session);
        model.addAttribute("currentValue", format(calculator.getCurrentValue()));
        model.addAttribute("history", calculator.getHistory());
        model.addAttribute("undoAvailable", calculator.isUndoAvailable());
        model.addAttribute("redoAvailable", calculator.isRedoAvailable());
        return "index";
    }

    @PostMapping("/api/calculate")
    @ResponseBody
    public CalculatorResponse calculate(@RequestParam("operator") String operator,
                                         @RequestParam("operand") double operand,
                                         HttpSession session) {
        Calculator calculator = getCalculator(session);
        try {
            CalculatorCommand command;
            switch (operator) {
                case "+":
                    command = new AddCommand(operand);
                    break;
                case "-":
                    command = new SubtractCommand(operand);
                    break;
                case "*":
                    command = new MultiplyCommand(operand);
                    break;
                case "/":
                    command = new DivideCommand(operand);
                    break;
                default:
                    return new CalculatorResponse("error", "Invalid operator: " + operator,
                            format(calculator.getCurrentValue()), calculator.getHistory(),
                            calculator.isUndoAvailable(), calculator.isRedoAvailable(), null);
            }
            calculator.executeCommand(command);
            return buildResponse(calculator);
        } catch (ArithmeticException e) {
            return new CalculatorResponse("error", e.getMessage(),
                    format(calculator.getCurrentValue()), calculator.getHistory(),
                    calculator.isUndoAvailable(), calculator.isRedoAvailable(), null);
        } catch (Exception e) {
            return new CalculatorResponse("error", "An error occurred: " + e.getMessage(),
                    format(calculator.getCurrentValue()), calculator.getHistory(),
                    calculator.isUndoAvailable(), calculator.isRedoAvailable(), null);
        }
    }

    @PostMapping("/api/undo")
    @ResponseBody
    public CalculatorResponse undo(HttpSession session) {
        Calculator calculator = getCalculator(session);
        calculator.undo();
        return buildResponse(calculator);
    }

    @PostMapping("/api/redo")
    @ResponseBody
    public CalculatorResponse redo(HttpSession session) {
        Calculator calculator = getCalculator(session);
        calculator.redo();
        return buildResponse(calculator);
    }

    @PostMapping("/api/clear")
    @ResponseBody
    public CalculatorResponse clear(HttpSession session) {
        Calculator calculator = getCalculator(session);
        calculator.clear();
        return buildResponse(calculator);
    }
}
