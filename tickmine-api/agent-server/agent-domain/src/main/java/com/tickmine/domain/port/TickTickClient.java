package com.tickmine.domain.port;

import java.util.List;

public interface TickTickClient {

    String createProject(String name, String token);

    String createTask(TickTickTaskRequest task, String token);

    void updateTask(String taskId, TickTickTaskRequest task, String token);

    List<TickTickTaskResponse> listTasks(String projectId, String token);

    List<TickTickProjectResponse> listProjects(String token);

    List<TickTickTaskResponse> getProjectTasks(String projectId, String projectName, String token);
}
