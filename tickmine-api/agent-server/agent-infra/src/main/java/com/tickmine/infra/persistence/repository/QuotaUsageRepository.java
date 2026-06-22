package com.tickmine.infra.persistence.repository;

import com.tickmine.infra.persistence.entity.QuotaUsageEntity;
import com.tickmine.infra.persistence.entity.QuotaUsageId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuotaUsageRepository extends JpaRepository<QuotaUsageEntity, QuotaUsageId> {
}
