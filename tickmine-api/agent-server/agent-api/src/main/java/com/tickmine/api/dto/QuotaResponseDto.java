package com.tickmine.api.dto;

public record QuotaResponseDto(String tier, int dailyLimit, int used, int remaining) {}
