ALTER TABLE media_asset
    ADD COLUMN unreferenced_at DATETIME(3) NULL AFTER reference_count,
    ADD COLUMN delete_attempts INT NOT NULL DEFAULT 0 AFTER unreferenced_at,
    ADD COLUMN next_delete_retry_at DATETIME(3) NULL AFTER delete_attempts,
    ADD COLUMN last_delete_error VARCHAR(1000) NULL AFTER next_delete_retry_at,
    ADD KEY idx_media_asset_cleanup (
        status,
        unreferenced_at,
        next_delete_retry_at,
        deleted_at
    );

UPDATE media_asset
SET unreferenced_at = created_at
WHERE deleted_at IS NULL
  AND status <> 'ACTIVE'
  AND unreferenced_at IS NULL;
