package com.tickmine.domain.exception;

public class QuotaExceededException extends RuntimeException {

    public QuotaExceededException(String userId, int limit) {
        super("Daily chat quota exceeded for user " + userId + ", limit " + limit);
    }
}
