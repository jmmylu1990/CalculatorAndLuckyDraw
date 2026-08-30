package com.example.amway.exception;

import com.example.amway.calculator.CalculatorResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public CalculatorResponse handleException(Exception e) {
        return new CalculatorResponse("error", "系統錯誤: " + e.getMessage(), "0", java.util.List.of(), false, false, null);
    }
}
