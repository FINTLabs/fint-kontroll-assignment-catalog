INSERT INTO user_entra_memberships (id, user_entra_id, resource_entra_id, membership_status, entra_status)
SELECT sequence_number,
       md5('user-' || sequence_number)::uuid,
       md5('resource-' || sequence_number)::uuid,
       'ACTIVE',
       'MEMBERSHIP_CONFIRMED'
FROM generate_series(1, 100000) AS sequence_number;

INSERT INTO flattened_user_assignments (
    id,
    user_ref,
    identity_provider_user_object_id,
    resource_ref,
    identity_provider_group_object_id,
    user_entra_membership_id,
    assignment_creation_date,
    assignment_termination_date
)
SELECT sequence_number,
       sequence_number,
       md5('user-' || sequence_number)::uuid,
       sequence_number,
       md5('resource-' || sequence_number)::uuid,
       sequence_number,
       NOW(),
       NULL
FROM generate_series(1, 100000) AS sequence_number;

SELECT setval(pg_get_serial_sequence('user_entra_memberships', 'id'), 100000, true);
SELECT setval(pg_get_serial_sequence('flattened_user_assignments', 'id'), 100000, true);
