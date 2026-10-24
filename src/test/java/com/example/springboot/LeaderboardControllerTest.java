package com.example.springboot;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
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

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:leaderboardtest;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class LeaderboardControllerTest {

  // Use the in-memory test database to create leaderboard data without touching PostgreSQL.
  @Autowired
  private JdbcTemplate jdbcTemplate;

  // Sends requests through Spring MVC and checks the rendered HTML response.
  @Autowired
  private MockMvc mvc;

  @BeforeEach
  void setUpDatabase() {
    // Recreate minimal tables before each test so the test starts with a known database state.
    jdbcTemplate.execute("DROP TABLE IF EXISTS RUN");
    jdbcTemplate.execute("DROP TABLE IF EXISTS PLAYER");
    jdbcTemplate.execute(
        "CREATE TABLE PLAYER (player_id INTEGER PRIMARY KEY, number_of_runs INTEGER NOT NULL, "
            + "high_score_run_id INTEGER)");
    jdbcTemplate.execute(
        "CREATE TABLE RUN (run_id INTEGER PRIMARY KEY, player_id INTEGER NOT NULL, "
            + "steps INTEGER NOT NULL, score INTEGER NOT NULL, korokorobi BOOLEAN)");

    // Create 27 players with one best run each, enough to require a second page.
    for (int id = 1; id <= 27; id++) {
      jdbcTemplate.update(
          "INSERT INTO PLAYER (player_id, number_of_runs, high_score_run_id) VALUES (?, 1, ?)",
          id,
          id);
      jdbcTemplate.update(
          "INSERT INTO RUN (run_id, player_id, steps, score, korokorobi) VALUES (?, ?, ?, ?, TRUE)",
          id,
          id,
          id,
          1000 + id);
    }

    jdbcTemplate.update(
        "INSERT INTO RUN (run_id, player_id, steps, score, korokorobi) VALUES (28, 27, 4, 900, FALSE)");
  }

  @Test
  void paginatesLeaderboardResultsInScoreOrder() throws Exception {
    // Page one should contain the 25 highest scores, but not the 25th-place cutoff's next row.
    mvc.perform(get("/leaderboard"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Page 1 of 2")))
        .andExpect(content().string(containsString(">1027</td>")))
        .andExpect(content().string(containsString(">1003</td>")))
        .andExpect(content().string(not(containsString(">1002</td>"))));

    // Page two contains the remaining two rows and excludes the top score from page one.
    mvc.perform(get("/leaderboard").param("page", "2"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Page 2 of 2")))
        .andExpect(content().string(containsString(">1002</td>")))
        .andExpect(content().string(containsString(">1001</td>")))
        .andExpect(content().string(not(containsString(">1027</td>"))));
  }

  @Test
  void filtersToPlayersWhoHaveAtLeastOneWinningRun() throws Exception {
    jdbcTemplate.update("UPDATE RUN SET korokorobi = FALSE WHERE MOD(player_id, 2) = 0");

    mvc.perform(get("/leaderboard").param("hasWon", "false", "true"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Showing 14 of 14 results")))
        .andExpect(content().string(containsString(">27</td>")))
        .andExpect(content().string(not(containsString(">26</td>"))));

    mvc.perform(get("/leaderboard").param("hasWon", "false"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Showing 25 of 27 results")));
  }

  @Test
  void showsEveryRunWhenHighscoreOnlyIsUnchecked() throws Exception {
    mvc.perform(get("/leaderboard").param("hasWon", "false", "true"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Showing 25 of 27 results")));

    mvc.perform(get("/leaderboard")
        .param("hasWon", "false")
        .param("highscoreOnly", "false"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Showing 25 of 28 results")))
        .andExpect(content().string(containsString("Page 1 of 2")));

    mvc.perform(get("/leaderboard")
        .param("hasWon", "false")
        .param("highscoreOnly", "false")
        .param("page", "2"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Showing 3 of 28 results")))
        .andExpect(content().string(containsString(">900</td>")));
  }

  @Test
  void searchesPlayerIdsBySubstring() throws Exception {
    jdbcTemplate.update(
        "INSERT INTO PLAYER (player_id, number_of_runs, high_score_run_id) VALUES (12345, 1, 1001)");
    jdbcTemplate.update(
        "INSERT INTO RUN (run_id, player_id, steps, score, korokorobi) VALUES (1001, 12345, 10, 900, TRUE)");
    jdbcTemplate.update(
        "INSERT INTO PLAYER (player_id, number_of_runs, high_score_run_id) VALUES (23456, 1, 1002)");
    jdbcTemplate.update(
        "INSERT INTO RUN (run_id, player_id, steps, score, korokorobi) VALUES (1002, 23456, 9, 901, TRUE)");

    mvc.perform(get("/leaderboard")
        .param("searchString", "234")
        .param("hasWon", "false", "true"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Showing 2 of 2 results")))
        .andExpect(content().string(containsString(">12345</td>")))
        .andExpect(content().string(containsString(">23456</td>")))
        .andExpect(content().string(containsString("value=\"234\"")));
  }
}
