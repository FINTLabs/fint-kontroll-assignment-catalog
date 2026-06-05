ALTER TABLE flattened_assignments
    RENAME TO flattened_user_assignments;

CREATE TABLE user_entra_memberships
(
    id                        BIGSERIAL PRIMARY KEY,
    user_entra_id             UUID                     NOT NULL,
    resource_entra_id         UUID                     NOT NULL,
    created_date              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    membership_status         VARCHAR(255)             NOT NULL,
    entra_status              VARCHAR(255)             NOT NULL,
    sent_to_entra_at          TIMESTAMP WITH TIME ZONE,
    deletion_sent_to_entra_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_user_resource UNIQUE (user_entra_id, resource_entra_id)
);

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
               AND bool_and(
                    CASE
                        WHEN assignment_termination_date IS NULL
                            THEN coalesce(identity_provider_group_membership_confirmed, false)
                        ELSE true
                        END
                   ) THEN 'MEMBERSHIP_CONFIRMED'
           WHEN bool_or(assignment_termination_date IS NULL) THEN 'NOT_SENT'
           WHEN bool_and(coalesce(identity_provider_group_membership_deletion_confirmed, false)) THEN 'DELETION_CONFIRMED'
           ELSE 'TO_BE_DELETED'
           END
FROM flattened_user_assignments
WHERE identity_provider_user_object_id IS NOT NULL
  AND identity_provider_group_object_id IS NOT NULL
GROUP BY identity_provider_user_object_id, identity_provider_group_object_id;

ALTER TABLE flattened_user_assignments
    ADD COLUMN user_entra_membership_id BIGINT;

UPDATE flattened_user_assignments flattened_assignment
SET user_entra_membership_id = user_entra_membership.id
FROM user_entra_memberships user_entra_membership
WHERE flattened_assignment.identity_provider_user_object_id = user_entra_membership.user_entra_id
  AND flattened_assignment.identity_provider_group_object_id = user_entra_membership.resource_entra_id;

ALTER TABLE flattened_user_assignments
    ADD CONSTRAINT fk_flattened_user_assignments_user_entra_membership
        FOREIGN KEY (user_entra_membership_id)
            REFERENCES user_entra_memberships (id);

ALTER TABLE flattened_user_assignments
    DROP COLUMN identity_provider_group_membership_confirmed,
    DROP COLUMN identity_provider_group_membership_deletion_confirmed;
