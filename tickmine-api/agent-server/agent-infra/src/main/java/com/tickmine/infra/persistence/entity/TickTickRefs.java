package com.tickmine.infra.persistence.entity;

import java.util.List;

public record TickTickRefs(String projectId, List<String> taskIds) {
}
