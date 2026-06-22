package com.tickmine.api.sse;

public record ChatErrorEvent(String error, String message, int status) {}
