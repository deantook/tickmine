package com.tickmine.executor;

public final class PriorityMapper {

    private PriorityMapper() {
    }

    public static int toTickTick(String priority) {
        if (priority == null) {
            return 0;
        }
        return switch (priority.toLowerCase()) {
            case "low" -> 1;
            case "medium" -> 3;
            case "high" -> 5;
            default -> 0;
        };
    }
}
