-- Run after Flyway V39, before starting the new application version.
-- Migrate active assignments and inactive pairs with unconfirmed deletion.
-- Back up the database separately if needed; a full table copy adds I/O here.
BEGIN;
INSERT INTO user_entra_memberships (user_entra_id,
                                    resource_entra_id,
                                    created_date,
                                    membership_status,
                                    entra_status)
SELECT identity_provider_user_object_id,
       identity_provider_group_object_id,
       now(),
       CASE WHEN bool_or(assignment_termination_date IS NULL)
           THEN 'ACTIVE' ELSE 'INACTIVE' END,
       CASE
           WHEN bool_or(assignment_termination_date IS NULL)
                AND bool_and(CASE
                    WHEN assignment_termination_date IS NULL
                        THEN coalesce(identity_provider_group_membership_confirmed, false)
                    ELSE true
                END) THEN 'MEMBERSHIP_CONFIRMED'
           WHEN bool_or(assignment_termination_date IS NULL) THEN 'NOT_SENT'
           ELSE 'TO_BE_DELETED'
       END
FROM flattened_user_assignments
WHERE user_entra_membership_id IS NULL
  AND (assignment_termination_date IS NULL
       OR identity_provider_group_membership_deletion_confirmed IS NOT TRUE)
GROUP BY identity_provider_user_object_id, identity_provider_group_object_id
ON CONFLICT (user_entra_id, resource_entra_id) DO NOTHING;

-- V37 creates the target empty; update planner statistics before the large join.
ANALYZE user_entra_memberships;

UPDATE flattened_user_assignments flattened_assignment
SET user_entra_membership_id = user_entra_membership.id
FROM user_entra_memberships user_entra_membership
WHERE flattened_assignment.user_entra_membership_id IS NULL
  AND (flattened_assignment.assignment_termination_date IS NULL
       OR flattened_assignment.identity_provider_group_membership_deletion_confirmed IS NOT TRUE)
  AND flattened_assignment.identity_provider_user_object_id = user_entra_membership.user_entra_id
  AND flattened_assignment.identity_provider_group_object_id = user_entra_membership.resource_entra_id;

-- Validate the active/actionable invariant and enforce it until finalization.
ALTER TABLE flattened_user_assignments
    ADD CONSTRAINT ck_flattened_user_assignments_actionable_membership
    CHECK (user_entra_membership_id IS NOT NULL OR
           (assignment_termination_date IS NOT NULL AND
            identity_provider_group_membership_deletion_confirmed IS TRUE)) NOT VALID;

ALTER TABLE flattened_user_assignments
    VALIDATE CONSTRAINT ck_flattened_user_assignments_actionable_membership;

COMMIT;
