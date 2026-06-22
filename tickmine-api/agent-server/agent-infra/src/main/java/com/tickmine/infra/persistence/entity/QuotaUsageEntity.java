package com.tickmine.infra.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "quota_usage")
@Data
public class QuotaUsageEntity {

    @EmbeddedId
    private QuotaUsageId id;

    @Column(name = "chat_count")
    private int chatCount;
}
