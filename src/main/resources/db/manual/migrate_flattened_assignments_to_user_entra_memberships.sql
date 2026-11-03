BEGIN;

INSERT INTO user_entra_memberships (user_entra_id,
                                    resource_entra_id,
                                    created_date,
                                    membership_status,
                                    entra_status)
SELECT identity_provider_user_object_id,
       identity_provider_group_object_id,
       now(),
       CASE
           WHEN bool_or(assignment_termination_date IS NULL) THEN 'ACTIVE'
           ELSE 'INACTIVE'
       END,
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
GROUP BY identity_provider_user_object_id, identity_provider_group_object_id
ON CONFLICT (user_entra_id, resource_entra_id) DO NOTHING;

UPDATE flattened_user_assignments flattened_assignment
SET user_entra_membership_id = user_entra_membership.id
FROM user_entra_memberships user_entra_membership
WHERE flattened_assignment.user_entra_membership_id IS NULL
  AND flattened_assignment.identity_provider_user_object_id = user_entra_membership.user_entra_id
  AND flattened_assignment.identity_provider_group_object_id = user_entra_membership.resource_entra_id;

ALTER TABLE flattened_user_assignments
    ALTER COLUMN user_entra_membership_id SET NOT NULL,
    DROP COLUMN identity_provider_group_membership_confirmed,
    DROP COLUMN identity_provider_group_membership_deletion_confirmed,
    DROP COLUMN identity_provider_user_object_id,
    DROP COLUMN identity_provider_group_object_id;

COMMIT;
