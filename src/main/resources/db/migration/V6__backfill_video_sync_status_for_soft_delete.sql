-- Soft delete uses sync_status = 'DELETED'. Existing active rows need a
-- non-null status so "not deleted" queries continue to include them.
DO $$
BEGIN
    IF to_regclass('public.video') IS NOT NULL THEN
        UPDATE public.video
        SET sync_status = 'COMPLETED'
        WHERE sync_status IS NULL;
    END IF;

    IF to_regclass('public.videos') IS NOT NULL THEN
        UPDATE public.videos
        SET sync_status = 'COMPLETED'
        WHERE sync_status IS NULL;
    END IF;
END $$;
