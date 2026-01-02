package com.neuralhealer.backend.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Immutable;

/**
 * Rules defining what data a doctor can access after an engagement ends.
 * Maps to: engagement_access_rules table
 * 
 * NOTE: This table is READ-ONLY. Rules are pre-seeded in the database.
 */
@Entity
@Table(name = "engagement_access_rules")
@Immutable // Mark as immutable for Hibernate
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EngagementAccessRule {

    @Id
    @Column(name = "rule_name", nullable = false)
    private String ruleName;

    @Column(name = "description")
    private String description;

    @Column(name = "can_view_all_history")
    private boolean canViewAllHistory;

    @Column(name = "can_view_current_only")
    private boolean canViewCurrentOnly;

    @Column(name = "can_view_demographics")
    private boolean canViewDemographics;

    @Column(name = "can_view_checkins")
    private boolean canViewCheckins;

    @Column(name = "retention_period_days")
    private Integer retentionPeriodDays;
}
