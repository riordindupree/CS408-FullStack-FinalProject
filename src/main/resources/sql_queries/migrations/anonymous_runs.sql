-- Retain run history after the associated player account is deleted.
ALTER TABLE RUN
    ALTER COLUMN player_id DROP NOT NULL;
