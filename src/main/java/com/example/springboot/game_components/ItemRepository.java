package com.example.springboot.game_components;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;


/**
 * Used to return the icon of a given item based on the items ID. 
 * ItemRepository is used in Action's getIcon function.
 * ItemRepository
 */
@Repository
public class ItemRepository {
    private final JdbcTemplate jdbcTemplate;

    public ItemRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String findIconById(int itemId) {
        return jdbcTemplate.queryForObject(
            "SELECT item_icon FROM ITEM WHERE item_id = ?",
            String.class,
            itemId
        );
    }

    public String findNameById(int itemId) {
        return jdbcTemplate.queryForObject(
            "SELECT item_name FROM ITEM WHERE item_id = ?",
            String.class,
            itemId
        );
    }

    public int findScoreById(int itemId) {
        return jdbcTemplate.queryForObject(
            "SELECT item_score FROM ITEM WHERE item_id = ?",
            Integer.class,
            itemId
        );
    }
}