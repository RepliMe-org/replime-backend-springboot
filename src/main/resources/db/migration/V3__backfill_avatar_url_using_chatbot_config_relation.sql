ALTER TABLE influencer_verifications
    ADD COLUMN IF NOT EXISTS avatar_url varchar(255);

DO $$
DECLARE
    updated_count integer := 0;
    total_updated integer := 0;
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'chatbot_config'
          AND column_name = 'avatar_url'
    ) THEN
        IF EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = current_schema()
              AND table_name = 'chatbot_config'
              AND column_name = 'chatbot_id'
        ) THEN
            EXECUTE '
                UPDATE influencer_verifications iv
                SET avatar_url = cc.avatar_url
                FROM chatbot_config cc
                JOIN chatbot c ON c.id = cc.chatbot_id
                WHERE iv.user_id = c.influencer_id
                  AND cc.avatar_url IS NOT NULL
                  AND cc.avatar_url <> ''''
                  AND (iv.avatar_url IS NULL OR iv.avatar_url = '''')
            ';
            GET DIAGNOSTICS updated_count = ROW_COUNT;
            total_updated := total_updated + updated_count;
            RAISE NOTICE 'Backfilled avatar_url through chatbot_config.chatbot_id: % row(s)', updated_count;
        END IF;

        IF EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = current_schema()
              AND table_name = 'chatbot'
              AND column_name = 'config_id'
        ) THEN
            EXECUTE '
                UPDATE influencer_verifications iv
                SET avatar_url = cc.avatar_url
                FROM chatbot c
                JOIN chatbot_config cc ON cc.id = c.config_id
                WHERE iv.user_id = c.influencer_id
                  AND cc.avatar_url IS NOT NULL
                  AND cc.avatar_url <> ''''
                  AND (iv.avatar_url IS NULL OR iv.avatar_url = '''')
            ';
            GET DIAGNOSTICS updated_count = ROW_COUNT;
            total_updated := total_updated + updated_count;
            RAISE NOTICE 'Backfilled avatar_url through chatbot.config_id: % row(s)', updated_count;
        END IF;

        RAISE NOTICE 'Total avatar_url rows backfilled into influencer_verifications: %', total_updated;
    ELSE
        RAISE NOTICE 'chatbot_config.avatar_url does not exist; no avatar_url values to backfill';
    END IF;
END $$;
