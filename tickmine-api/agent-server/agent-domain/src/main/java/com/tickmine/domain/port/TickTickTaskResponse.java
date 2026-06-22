package com.tickmine.domain.port;

public record TickTickTaskResponse(
        String id,
        String projectId,
        String projectName,
        String title,
        String dueDate,
        String startDate,
        String timeZone,
        Boolean isAllDay,
        Integer status) {

    public TickTickTaskResponse(
            String id, String projectId, String projectName, String title, String dueDate, Integer status) {
        this(id, projectId, projectName, title, dueDate, null, null, null, status);
    }

    public TickTickTaskResponse(
            String id,
            String projectId,
            String projectName,
            String title,
            String dueDate,
            String startDate,
            Integer status) {
        this(id, projectId, projectName, title, dueDate, startDate, null, null, status);
    }
}
