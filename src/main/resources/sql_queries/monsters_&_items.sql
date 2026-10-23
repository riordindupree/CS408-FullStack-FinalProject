-- Seed data from the monsters and items listed in guide.html.
BEGIN;

DELETE FROM GAINED_ITEM
WHERE item_id IN (
    SELECT item_id FROM ITEM WHERE item_name IN ('Berry', 'Key 1', 'Key 2')
);
DELETE FROM GAINED_MONSTER
WHERE monster_id IN (
    SELECT monster_id
    FROM MONSTER
    WHERE monster_name IN ('Molynx', 'Iktorb', 'Lappup', 'Quapok', 'Fleastrike', 'Korokorobi')
);
DELETE FROM ITEM WHERE item_name IN ('Berry', 'Key 1', 'Key 2');
DELETE FROM MONSTER
WHERE monster_name IN ('Molynx', 'Iktorb', 'Lappup', 'Quapok', 'Fleastrike', 'Korokorobi');

INSERT INTO MONSTER (monster_name, monster_desc, monster_icon, monster_score) VALUES
    ('Molynx', 'A somewhat rodent-like creature. They are very opportunistic hunters.', 'ᘛ⁐̤ᕐᐷ', 200),
    ('Iktorb', 'Spider-like creatures used to capture certain creatures with its sticky webs.', 'ᕐ⚗︎', 400),
    ('Lappup', 'Dog-like creatures used to capture certain creatures by swallowing them whole.', '૮(´ ཀ `)ა', 300),
    ('Quapok', 'Large duck-like creature. Great at attacking other creatures.', E'⊃ভ了\n⋂', 5500),
    ('Fleastrike', 'Wool-covered parasites. Its fluffy body makes it hard for other creatures to swallow.', '☁︎੬ཐི༏ཋྀ3', 50),
    ('Korokorobi', 'Final boss. Defeating and capturing it is the goal of the game.', E'(｡෴｡)\n(｡෴｡)\n(ᄋ)(｡෴｡)(ᄋ)', 7800);

INSERT INTO ITEM (item_name, item_desc, item_icon, item_score) VALUES
    ('Berry', 'Liked by different types of enemies. Can be used to befriend monsters.', 'ጬ', 100),
    ('Berry', 'Liked by different types of enemies. Can be used to befriend monsters.', 'ଟ', 100),
    ('Berry', 'Liked by different types of enemies. Can be used to befriend monsters.', '☊', 100),
    ('Key 1', 'Used to unlock door 1.', '♀', 500),
    ('Key 2', 'Used to unlock door 2.', '♂', 500);

COMMIT;