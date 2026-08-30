package com.example.amway.calculator;

import java.io.Serializable;
import java.util.List;

public class CalculatorResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String currentValue;
    private final List<String> history;
    private final boolean undoAvailable;
    private final boolean redoAvailable;
    private final String status;
    private final String message;
    private final String pendingOperator;

    public CalculatorResponse(String currentValue, List<String> history, boolean undoAvailable, boolean redoAvailable, String pendingOperator) {
        this.currentValue = currentValue;
        this.history = history;
        this.undoAvailable = undoAvailable;
        this.redoAvailable = redoAvailable;
        this.status = "success";
        this.message = "";
        this.pendingOperator = pendingOperator;
    }

    public CalculatorResponse(String status, String message, String currentValue, List<String> history, boolean undoAvailable, boolean redoAvailable, String pendingOperator) {
        this.status = status;
        this.message = message;
        this.currentValue = currentValue;
        this.history = history;
        this.undoAvailable = undoAvailable;
        this.redoAvailable = redoAvailable;
        this.pendingOperator = pendingOperator;
    }

    public String getCurrentValue() {
        return currentValue;
    }

    public List<String> getHistory() {
        return history;
    }

    public boolean isUndoAvailable() {
        return undoAvailable;
    }

    public boolean isRedoAvailable() {
        return redoAvailable;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getPendingOperator() {
        return pendingOperator;
    }
}
