-- Run after the first phase when the historical backfill can be scheduled.
-- Preserve membership statuses that the running application may have changed.
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
           WHEN bool_and(coalesce(identity_provider_group_membership_deletion_confirmed, false))
               THEN 'DELETION_CONFIRMED'
           ELSE 'TO_BE_DELETED'
       END
FROM flattened_user_assignments
WHERE user_entra_membership_id IS NULL
GROUP BY identity_provider_user_object_id, identity_provider_group_object_id
ON CONFLICT (user_entra_id, resource_entra_id) DO NOTHING;

ANALYZE user_entra_memberships;

UPDATE flattened_user_assignments flattened_assignment
SET user_entra_membership_id = user_entra_membership.id
FROM user_entra_memberships user_entra_membership
WHERE flattened_assignment.user_entra_membership_id IS NULL
  AND flattened_assignment.identity_provider_user_object_id = user_entra_membership.user_entra_id
  AND flattened_assignment.identity_provider_group_object_id = user_entra_membership.resource_entra_id;

-- SET NOT NULL verifies that every row was linked before old IDs are dropped.
ALTER TABLE flattened_user_assignments
    ALTER COLUMN user_entra_membership_id SET NOT NULL,
    DROP CONSTRAINT ck_flattened_user_assignments_actionable_membership,
    DROP COLUMN identity_provider_group_membership_confirmed,
    DROP COLUMN identity_provider_group_membership_deletion_confirmed,
    DROP COLUMN identity_provider_user_object_id,
    DROP COLUMN identity_provider_group_object_id;

COMMIT;
