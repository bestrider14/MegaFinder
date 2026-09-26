package com.finder.demo.testquotes;

public record TestStepRequest(Integer stepNumber, Integer subStep, String startCondition,
                              String pinToTest, String measurementType, String unit,
                              Double expectedMin, Double expectedMax, String relay,
                              Double voltage, String notes) {
}
