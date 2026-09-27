package com.finder.demo.testquotes;

public record TestStepRequest(Integer stepNumber, Integer subStep, String verificationType,
                              String startCondition, String groundReference, String pinToTest,
                              String measurementType, String unit, Double expectedMin,
                              Double expectedMax, String relay, Double voltage, String notes,
                              String powerSupplyName, Double supplyVoltage, Double supplyCurrent,
                              String relayChannels, Integer relayDelayMs, String testMethod,
                              String measurementMethod, Integer waitMs, String requiredInstruments,
                              Long testTemplateId) {
}
