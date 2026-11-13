-- Seed data from the monsters and items listed in guide.html.
BEGIN;

DELETE FROM GAINED_ITEM
WHERE item_id IN (
    SELECT item_id
    FROM ITEM
    WHERE item_name IN ('Berry', 'Salty Berry', 'Spicy Berry', 'Twin Berry', 'Key 1', 'Key 2')
);
DELETE FROM GAINED_MONSTER
WHERE monster_id IN (
    SELECT monster_id
    FROM MONSTER
    WHERE monster_name IN ('Molynx', 'Iktorb', 'Lappup', 'Quapok', 'Fleastrike', 'Soupon', 'Korokorobi')
);
DELETE FROM ITEM
WHERE item_name IN ('Berry', 'Salty Berry', 'Spicy Berry', 'Twin Berry', 'Key 1', 'Key 2');
DELETE FROM MONSTER
WHERE monster_name IN ('Molynx', 'Iktorb', 'Lappup', 'Quapok', 'Fleastrike', 'Soupon', 'Korokorobi');

INSERT INTO MONSTER (monster_id, monster_name, monster_desc, monster_icon, monster_score) VALUES
    (1, 'Molynx', 'A somewhat rodent-like creature. They are very opportunistic hunters.', 'ᘛ⁐̤ᕐᐷ', 3800),
    (2, 'Iktorb', 'Spider-like creatures used to capture certain creatures with its sticky webs.', 'ᕐ⚗︎', 1400),
    (3, 'Lappup', 'Dog-like creatures used to capture certain creatures by swallowing them whole.', '૮(´ ཀ `)ა', 1300),
    (4, 'Quapok', 'Large duck-like creature. Great at attacking other creatures.', E'⊃ভ了\n⋂', 5500),
    (5, 'Fleastrike', 'Wool-covered parasites. Its fluffy body makes it hard for other creatures to swallow.', '☁︎੬ཐི༏ཋྀ3', 500),
    (6, 'Soupon', 'A turtle whose body is covered in an oil which makes its skin hot like magma.', '໔㈫৯', 2500),
    (7, 'Korokorobi', 'Final boss. Defeating and capturing it is the goal of the game.', E'(｡෴｡)\n(｡෴｡)\n(ᄋ)(｡෴｡)(ᄋ)', 7800);

SELECT setval(pg_get_serial_sequence('monster', 'monster_id'), (SELECT MAX(monster_id) FROM MONSTER), TRUE);

INSERT INTO ITEM (item_id, item_name, item_desc, item_icon, item_score) VALUES
    (1, 'Salty Berry', 'Liked by different types of enemies. Can be used to befriend monsters.', 'ጬ', 100),
    (2, 'Spicy Berry', 'Liked by different types of enemies. Can be used to befriend monsters.', 'ଟ', 100),
    (3, 'Twin Berry', 'Liked by different types of enemies. Can be used to befriend monsters.', '☊', 100),
    (4, 'Key 1', 'Used to unlock door 1.', '♀', 500),
    (5, 'Key 2', 'Used to unlock door 2.', '♂', 500);

SELECT setval(pg_get_serial_sequence('item', 'item_id'), (SELECT MAX(item_id) FROM ITEM), TRUE);

-- Fight Screen Interactions
-- NOTE: 
--  *  For all monsters, 'a' is the starting state.
--  *  Catchable state will always be '!'.
--  *  Caught state will always be '$'.
--  *  Failed captures will always be 'x'.
--  *  If an interaction does not exist, default response is 'Nothing Happened'. 
--     The player does not lose a step.
--  *  If the current state in the key is marked as '0', that means the given
--     interaction will alwasys happen, regardless of state.

INSERT INTO MONSTER_MONSTER () VALUES
    -- Molynx (1) Interactions
    (1, 2, 'a', 'The Opposing Molynx Ate Iktorb!', NULL, TRUE),
    (1, 4, '0', 'The Opposing Molynx Ran Away!', 'x', FALSE),
    (1, 5, 'a', 'The Opposing Molynx Tried To Swallow Fleastrike! It Cannot Stomache The Fluff! Molynx Is Vulnerable!', '!', TRUE),
    (1, 6, 'a', 'The Opposing Molynx Tried To Swallow Soupon! It Burned Its Tounge! Molynx Is Vulnerable!', '!', FALSE),
    (1, 7, '0', 'The Opposing Molynx Ran Away!', 'x', FALSE),
    (1, 2, '!', 'Molynx Was Caught!', '$', FALSE),
    (1, 3, '!', 'Molynx Was Caught!', '$', FALSE),

    -- Iktorkb (2) Interactions
    (2, 1, '0', 'Your Molynx Ate The Iktorb!', 'x', FALSE),
    (2, 2, '0', 'The Net Failed! (How Are You Supposed To Catch A Spider With Its Own Web?)', NULL),
    (2, 3, '0', 'Iktorb Was Caught!', '$', FALSE),
    (2, 4, '0', 'Your Quapok Ate The Iktorb!', 'x', FALSE),
    (2, 5, '0', 'Iktorb Accepted Your Offering! Iktorb Was Caught!', 'x', TRUE),
    


INSERT INTO MONSTER_ITEM () VALUES


COMMIT;