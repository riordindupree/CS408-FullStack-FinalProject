package com.example.springboot;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Loads guide content from the database catalog tables.
 */
@Controller
public class GuideController {

  private final JdbcTemplate jdbcTemplate;

  public GuideController(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @GetMapping("/guide")
  public String guide(Model model) {
    List<GuideEntry> items = jdbcTemplate.query(
        "SELECT item_name, item_icon, item_desc "
            + "FROM ITEM ORDER BY item_id",
        (resultSet, rowNumber) -> new GuideEntry(
            resultSet.getString("item_name"),
            resultSet.getString("item_icon"),
            resultSet.getString("item_desc")));

    List<GuideEntry> monsters = jdbcTemplate.query(
        "SELECT monster_name, monster_icon, monster_desc "
            + "FROM MONSTER ORDER BY monster_id",
        (resultSet, rowNumber) -> new GuideEntry(
            resultSet.getString("monster_name"),
            resultSet.getString("monster_icon"),
            resultSet.getString("monster_desc")));

    model.addAttribute("items", items);
    model.addAttribute("monsters", monsters);
    return "guide";
  }

  public static class GuideEntry {
    private final String name;
    private final String icon;
    private final String description;

    public GuideEntry(String name, String icon, String description) {
      this.name = name;
      this.icon = icon;
      this.description = description;
    }

    public String getName() {
      return name;
    }

    public String getIcon() {
      return icon;
    }

    public String getDescription() {
      return description;
    }

  }
}
