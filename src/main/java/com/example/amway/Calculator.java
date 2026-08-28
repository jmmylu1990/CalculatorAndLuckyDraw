package com.example.amway;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class Calculator implements Serializable {
    private static final long serialVersionUID = 1L;

    private double currentValue = 0.0;
    private final Stack<CalculatorCommand> undoStack = new Stack<>();
    private final Stack<CalculatorCommand> redoStack = new Stack<>();

    public double getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(double currentValue) {
        this.currentValue = currentValue;
    }

    public double executeCommand(CalculatorCommand command) {
        double result = command.execute(currentValue);
        currentValue = result;
        undoStack.push(command);
        redoStack.clear(); // Clear redo stack on new command
        return currentValue;
    }

    public boolean isUndoAvailable() {
        return !undoStack.isEmpty();
    }

    public boolean isRedoAvailable() {
        return !redoStack.isEmpty();
    }

    public double undo() {
        if (!isUndoAvailable()) {
            return currentValue;
        }
        CalculatorCommand command = undoStack.pop();
        currentValue = command.undo();
        redoStack.push(command);
        return currentValue;
    }

    public double redo() {
        if (!isRedoAvailable()) {
            return currentValue;
        }
        CalculatorCommand command = redoStack.pop();
        currentValue = command.execute(currentValue);
        undoStack.push(command);
        return currentValue;
    }

    public void clear() {
        currentValue = 0.0;
        undoStack.clear();
        redoStack.clear();
    }

    public List<String> getHistory() {
        List<String> history = new ArrayList<>();
        for (CalculatorCommand cmd : undoStack) {
            history.add(cmd.getDescription());
        }
        return history;
    }

    public String getRedoOperator() {
        if (redoStack.isEmpty()) {
            return null;
        }
        return redoStack.peek().getOperator();
    }
}
