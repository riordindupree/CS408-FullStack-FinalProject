package com.example.springboot.game_components;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MonsterRepository {
    // Shares the application's JDBC connection pool for catalog and interaction queries.
    private final JdbcTemplate jdbcTemplate;

    // Receives the configured database helper from Spring.
    public MonsterRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Loads a monster's display name for combat messages and inventory labels.
    public String findNameById(int monsterId) {
        return jdbcTemplate.queryForObject(
            "SELECT monster_name FROM MONSTER WHERE monster_id = ?",
            String.class,
            monsterId
        );
    }

    // Loads the monster art used by the fight board and inventory.
    public String findIconById(int monsterId) {
        return jdbcTemplate.queryForObject(
            "SELECT monster_icon FROM MONSTER WHERE monster_id = ?",
            String.class,
            monsterId
        );
    }

    // Loads the score awarded when a defeated monster is collected.
    public int findScoreById(int monsterId) {
        return jdbcTemplate.queryForObject(
            "SELECT monster_score FROM MONSTER WHERE monster_id = ?",
            Integer.class,
            monsterId
        );
    }

    // Looks up a monster-versus-monster rule for the current enemy state.
    public Optional<Interaction> findMonsterInteraction(
        int enemyId,
        int playerMonsterId,
        char currentState
    ) {
        return findInteraction(
            "MONSTER_MONSTER",
            "player_monster",
            enemyId,
            playerMonsterId,
            currentState
        );
    }

    // Looks up a monster-versus-item rule for the current enemy state.
    public Optional<Interaction> findItemInteraction(
        int enemyId,
        int playerItemId,
        char currentState
    ) {
        return findInteraction(
            "MONSTER_ITEM",
            "player_item",
            enemyId,
            playerItemId,
            currentState
        );
    }

    // Reads a specific-state rule first, then the table's '0' wildcard rule.
    private Optional<Interaction> findInteraction(
        String table,
        String participantColumn,
        int enemyId,
        int participantId,
        char currentState
    ) {
        // Table and column names are selected only by the two fixed callers above.
        String sql = "SELECT result, new_state, lose FROM " + table
            + " WHERE enemy_monster = ? AND " + participantColumn
            + " = ? AND current_state IN (?, '0')"
            + " ORDER BY CASE WHEN current_state = ? THEN 0 ELSE 1 END"
            + " LIMIT 1";

        // Use a list so a missing rule is represented as Optional.empty().
        List<Interaction> interactions = jdbcTemplate.query(
            sql,
            (resultSet, rowNumber) -> {
                String newState = resultSet.getString("new_state");
                boolean lose = resultSet.getBoolean("lose");
                if (resultSet.wasNull()) {
                    lose = false;
                }
                return new Interaction(
                    resultSet.getString("result"),
                    newState == null ? null : newState.charAt(0),
                    lose
                );
            },
            enemyId,
            participantId,
            String.valueOf(currentState),
            String.valueOf(currentState)
        );

        return interactions.stream().findFirst();
    }

    // Holds the outcome fields returned by either interaction table.
    public record Interaction(String result, Character newState, boolean lose) {
    }
}
