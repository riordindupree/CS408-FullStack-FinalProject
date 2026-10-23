-- The file contains default users which are meant to be used for testing the leaderboard functionality.
-- File must be run after schema.sql on a fresh test database.
BEGIN;

UPDATE PLAYER SET high_score_run_id = NULL
WHERE player_id IN (111111, 222222, 333333, 444444, 555555);

DELETE FROM GAINED_ITEM
WHERE run_id IN (SELECT run_id FROM RUN WHERE player_id IN (111111, 222222, 333333, 444444, 555555));
DELETE FROM GAINED_MONSTER
WHERE run_id IN (SELECT run_id FROM RUN WHERE player_id IN (111111, 222222, 333333, 444444, 555555));
DELETE FROM RUN WHERE player_id IN (111111, 222222, 333333, 444444, 555555);
DELETE FROM PLAYER WHERE player_id IN (111111, 222222, 333333, 444444, 555555);

-- Create default player values.
INSERT INTO PLAYER (player_id, password_hash, number_of_runs) VALUES
    (111111, 'PASSWORD', 3),
    (222222, 'PASSWORD', 2),
    (333333, 'PASSWORD', 2),
    (444444, 'PASSWORD', 1),
    (555555, 'PASSWORD', 0);

INSERT INTO RUN (player_id, steps, score, korokorobi) VALUES
    (111111, 40, 1240, FALSE),
    (111111, 12, 6812, FALSE),
    (111111, 5, 9205, TRUE),
    (222222, 35, 935, FALSE),
    (222222, 18, 8118, TRUE),
    (333333, 22, 4222, FALSE),
    (333333, 8, 6308, FALSE),
    (444444, 7, 8807, TRUE);

UPDATE PLAYER p
SET high_score_run_id = (
    SELECT r.run_id
    FROM RUN r
    WHERE r.player_id = p.player_id
    ORDER BY r.score DESC, r.run_id
    LIMIT 1
)
WHERE p.player_id BETWEEN 111111 AND 444444;

SELECT setval(
    pg_get_serial_sequence('player', 'player_id'),
    (SELECT MAX(player_id) FROM PLAYER),
    TRUE
);

COMMIT;