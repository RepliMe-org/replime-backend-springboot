ALTER TABLE influencer_verifications
    ADD COLUMN IF NOT EXISTS avatar_url varchar(255);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'chatbot_config'
          AND column_name = 'avatar_url'
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
    END IF;
END $$;
