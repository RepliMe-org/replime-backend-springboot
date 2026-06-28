ALTER TABLE video
    DROP CONSTRAINT IF EXISTS video_sync_status_check;

ALTER TABLE video
    ADD CONSTRAINT video_sync_status_check
    CHECK (sync_status IN ('PROCESSING', 'COMPLETED', 'FAILED', 'DEAD', 'DELETED'));
