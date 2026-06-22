package com.tickmine.domain.model;

import java.util.List;

public record MilestoneDsl(String name, List<TaskDsl> tasks) {
}
