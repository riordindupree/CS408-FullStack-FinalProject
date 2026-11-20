-- Remove legacy leaderboard fixture rows previously loaded into the live database.
UPDATE PLAYER
SET high_score_run_id = NULL
WHERE player_id IN (111111, 222222, 333333, 444444, 555555);

DELETE FROM GAINED_ITEM
WHERE run_id IN (
    SELECT run_id FROM RUN
    WHERE player_id IN (111111, 222222, 333333, 444444, 555555)
);
DELETE FROM GAINED_MONSTER
WHERE run_id IN (
    SELECT run_id FROM RUN
    WHERE player_id IN (111111, 222222, 333333, 444444, 555555)
);
DELETE FROM RUN
WHERE player_id IN (111111, 222222, 333333, 444444, 555555);
DELETE FROM PLAYER
WHERE player_id IN (111111, 222222, 333333, 444444, 555555);
