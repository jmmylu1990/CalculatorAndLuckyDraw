package com.example.amway.calculator;

public interface CalculatorCommand {
    double execute(double currentValue);
    double undo();
    String getOperator();
    double getOperand();
    String getDescription();

    default String format(double d) {
        if (Double.isInfinite(d) || Double.isNaN(d)) {
            return String.valueOf(d);
        }
        if (d == (long) d) {
            return String.format("%d", (long) d);
        }
        return String.format("%s", d);
    }
}
