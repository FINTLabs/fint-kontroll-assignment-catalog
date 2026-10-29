CREATE INDEX idx_user_entra_memberships_stale_add
    ON user_entra_memberships (membership_status, entra_status, sent_to_entra_at, id);

CREATE INDEX idx_user_entra_memberships_stale_remove
    ON user_entra_memberships (membership_status, entra_status, deletion_sent_to_entra_at, id);

CREATE INDEX idx_device_entra_memberships_stale_add
    ON device_entra_memberships (membership_status, entra_status, sent_to_entra_at, id);

CREATE INDEX idx_device_entra_memberships_stale_remove
    ON device_entra_memberships (membership_status, entra_status, deletion_sent_to_entra_at, id);
