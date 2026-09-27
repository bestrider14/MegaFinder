package com.finder.demo.testbenches;

public record BenchEquipmentRequest(String equipmentType, String name, Integer quantity,
                                    Integer channelCount, Double voltageMin, Double voltageMax,
                                    Double currentMax, String notes) {
}
