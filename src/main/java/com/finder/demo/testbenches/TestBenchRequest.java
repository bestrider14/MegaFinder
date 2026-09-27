package com.finder.demo.testbenches;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record TestBenchRequest(@NotBlank String name, String description, String location,
                               List<BenchEquipmentRequest> equipment,
                               List<BenchTerminalRequest> terminals) {
}
