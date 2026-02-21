package com.neuralhealer.backend.feature.engagement.enums;

/**
 * Defines the access level between a doctor and patient.
 * Stored in doctor_patients.relationship_status as VARCHAR(255),
 * referencing engagement_access_rules.rule_name.
 * 
 * This is NOT a database enum type - it references the access rules table,
 * allowing custom rules to be defined without schema changes.
 */
public enum RelationshipStatus {
    /**
     * First engagement request created, patient hasn't verified yet.
     * - Temporary status before first activation
     * - is_active = false
     * - If cancelled, transitions to INITIAL_CANCELLED_PENDING
     * - If verified, transitions to engagement's access_rule_name
     */
    INITIAL_PENDING("INITIAL_PENDING",
            "First engagement created, awaiting patient verification",
            false),

    /**
     * First engagement was cancelled before patient verified.
     * - Marks failed first attempt
     * - is_active = false
     * - Permanent record of declined relationship
     */
    INITIAL_CANCELLED_PENDING("INITIAL_CANCELLED_PENDING",
            "First engagement cancelled before activation",
            false),

    /**
     * Doctor has complete access to all patient data and history.
     * - View all past engagements
     * - View all AI chat history
     * - Modify notes and assessments
     * - Message patient
     * - Retention: Keeps access after engagement ends
     */
    FULL_ACCESS("FULL_ACCESS",
            "Complete access to all patient data and history",
            true),

    /**
     * Doctor can view but not modify patient data.
     * - View-only mode
     * - Cannot edit notes
     * - Can view historical data
     * - Retention: Keeps read access after engagement
     */
    READ_ONLY_ACCESS("READ_ONLY_ACCESS",
            "Read-only access to patient data",
            true),

    /**
     * Access limited to current engagement period only.
     * - Can only view data during active engagement
     * - After engagement ends, access revoked
     * - Retention: NO_ACCESS after engagement
     */
    CURRENT_ENGAGEMENT_ACCESS("CURRENT_ENGAGEMENT_ACCESS",
            "Access only during current engagement period",
            true),

    /**
     * Restricted access with specific limitations.
     * - Cannot view full history
     * - Can message and view current engagement
     * - Limited note permissions
     * - Retention: Revoked after engagement
     */
    LIMITED_ENGAGEMENT_ACCESS("LIMITED_ENGAGEMENT_ACCESS",
            "Limited access during engagement",
            true),

    /**
     * All access revoked.
     * - Doctor cannot view any patient data
     * - Cannot message patient
     * - Relationship exists in DB but inactive
     * - is_active = false
     */
    NO_ACCESS("NO_ACCESS",
            "No access to patient data",
            false);

    private final String ruleName;
    private final String description;
    private final boolean impliesActive;

    RelationshipStatus(String ruleName, String description, boolean impliesActive) {
        this.ruleName = ruleName;
        this.description = description;
        this.impliesActive = impliesActive;
    }

    public String getRuleName() {
        return ruleName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Returns whether this status implies is_active = true.
     * NO_ACCESS and INITIAL_* statuses always have is_active = false.
     */
    public boolean impliesActive() {
        return impliesActive;
    }

    /**
     * Lookup enum by rule name string.
     * 
     * @throws IllegalArgumentException if rule name not found
     */
    public static RelationshipStatus fromRuleName(String ruleName) {
        if (ruleName == null) {
            return null;
        }

        for (RelationshipStatus status : values()) {
            if (status.ruleName.equals(ruleName)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown relationship status: " + ruleName);
    }
}
