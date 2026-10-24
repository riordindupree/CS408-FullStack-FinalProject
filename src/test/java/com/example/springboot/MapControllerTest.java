package com.example.springboot;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:maptest;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class MapControllerTest {

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private MockMvc mvc;

  @BeforeEach
  void setUpItems() {
    jdbcTemplate.execute("DROP TABLE IF EXISTS ITEM");
    jdbcTemplate.execute(
        "CREATE TABLE ITEM (item_id INTEGER PRIMARY KEY, item_icon VARCHAR(16))");
    for (int itemId = 1; itemId <= 5; itemId++) {
      jdbcTemplate.update(
          "INSERT INTO ITEM (item_id, item_icon) VALUES (?, ?)",
          itemId,
          "item-icon-" + itemId);
    }
  }

  @Test
  void rendersTheSelectedGridFloorAndDatabaseItemIcons() throws Exception {
    mvc.perform(get("/map").param("floor", "1"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Floor 1")))
        .andExpect(content().string(containsString("item-icon-1")))
        .andExpect(content().string(containsString("item-icon-2")))
        .andExpect(content().string(containsString("item-icon-3")));
  }
}
