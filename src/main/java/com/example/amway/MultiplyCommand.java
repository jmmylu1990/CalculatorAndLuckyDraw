package com.example.amway;

public class MultiplyCommand implements CalculatorCommand {
    private final double operand;
    private double prevValue;
    private String description;

    public MultiplyCommand(double operand) {
        this.operand = operand;
    }

    @Override
    public double execute(double currentValue) {
        this.prevValue = currentValue;
        double result = currentValue * operand;
        this.description = String.format("%s * %s = %s", format(currentValue), format(operand), format(result));
        return result;
    }

    @Override
    public double undo() {
        return prevValue;
    }

    @Override
    public String getOperator() {
        return "*";
    }

    @Override
    public double getOperand() {
        return operand;
    }

    @Override
    public String getDescription() {
        return description;
    }
}
