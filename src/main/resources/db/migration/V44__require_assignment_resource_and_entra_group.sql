-- Backfill the assignment group ID from its resource before enforcing the invariant.
UPDATE assignments assignment
SET entra_group_id = resource.identity_provider_group_object_id
FROM assignment_resources resource
WHERE assignment.resource_ref = resource.id
  AND assignment.entra_group_id IS NULL;

-- Keep user projections aligned with their source assignment where legacy data is incomplete.
UPDATE flattened_user_assignments flattened_assignment
SET resource_ref = COALESCE(flattened_assignment.resource_ref, assignment.resource_ref),
    identity_provider_group_object_id = COALESCE(
        flattened_assignment.identity_provider_group_object_id,
        assignment.entra_group_id
    )
FROM assignments assignment
WHERE flattened_assignment.assignment_id = assignment.id
  AND (flattened_assignment.resource_ref IS NULL
       OR flattened_assignment.identity_provider_group_object_id IS NULL);

-- A device flattened assignment has a foreign key to assignments, so remove it before removing an
-- invalid source assignment. User projections with an invalid source or missing values are removed too.
DELETE FROM flattened_device_assignments flattened_assignment
USING assignments assignment
WHERE flattened_assignment.assignment_id = assignment.id
  AND (assignment.resource_ref IS NULL OR assignment.entra_group_id IS NULL);

DELETE FROM flattened_user_assignments flattened_assignment
USING assignments assignment
WHERE flattened_assignment.assignment_id = assignment.id
  AND (assignment.resource_ref IS NULL OR assignment.entra_group_id IS NULL);

DELETE FROM flattened_user_assignments
WHERE resource_ref IS NULL
   OR identity_provider_group_object_id IS NULL;

DELETE FROM assignments
WHERE resource_ref IS NULL
   OR entra_group_id IS NULL;

ALTER TABLE assignments
    ALTER COLUMN resource_ref SET NOT NULL,
    ALTER COLUMN entra_group_id SET NOT NULL;

ALTER TABLE flattened_user_assignments
    ALTER COLUMN resource_ref SET NOT NULL,
    ALTER COLUMN identity_provider_group_object_id SET NOT NULL;
