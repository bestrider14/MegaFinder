package com.finder.demo.testbenches;

public record BenchTerminalRequest(String terminalLabel, String pinName, String signalType,
                                   String description, String relayReference, String notes) {
}
