UPDATE RUN r
SET korokorobi = EXISTS (
    SELECT 1
    FROM GAINED_MONSTER gm
    WHERE gm.run_id = r.run_id
      AND gm.monster_id = 7
)
WHERE r.korokorobi IS DISTINCT FROM EXISTS (
    SELECT 1
    FROM GAINED_MONSTER gm
    WHERE gm.run_id = r.run_id
      AND gm.monster_id = 7
);
