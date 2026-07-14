CREATE UNIQUE INDEX IF NOT EXISTS uk_assignments_active_device_group_resource
    ON assignments (resource_ref, device_group_ref)
    WHERE assignment_removed_date IS NULL
      AND device_group_ref IS NOT NULL;
