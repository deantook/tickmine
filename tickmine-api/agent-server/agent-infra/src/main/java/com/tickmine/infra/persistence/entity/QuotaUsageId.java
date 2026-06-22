package com.tickmine.infra.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuotaUsageId implements Serializable {

    @Column(name = "user_id")
    private String userId;

    @Column(name = "usage_date")
    private LocalDate usageDate;
}
