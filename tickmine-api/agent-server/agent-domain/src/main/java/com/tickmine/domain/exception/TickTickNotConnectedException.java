package com.tickmine.domain.exception;

public class TickTickNotConnectedException extends RuntimeException {

    public TickTickNotConnectedException(String userId) {
        super("TickTick token not connected for user: " + userId);
    }
}
