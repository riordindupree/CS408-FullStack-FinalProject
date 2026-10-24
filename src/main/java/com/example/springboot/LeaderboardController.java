package com.example.springboot;

import java.util.List;
import java.util.Objects;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.springboot.LeaderboardController.LeaderboardEntry;

@Controller
public class LeaderboardController {

  // Keep page size in one place so database results and navigation stay consistent.
  private static final int PAGE_SIZE = 25;

  private final JdbcTemplate jdbcTemplate;

  public LeaderboardController(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @GetMapping("/leaderboard")
  public String leaderboard(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(required = false) String searchString,
      @RequestParam(required = false) List<Boolean> hasWon,
      @RequestParam(required = false) List<Boolean> highscoreOnly,
      Model model) {
    // Get filter values and normalize an empty search to match all player IDs.
    boolean winnerOnly = isEnabled(hasWon, true);
    boolean hsOnly = isEnabled(highscoreOnly, true);
    String playerString = searchString == null ? "" : searchString.trim();
    String playerIdPattern = "%" + playerString + "%";

    // Count the rows that match the run mode, player ID search, and optional win filter.
    int totalResults = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM PLAYER p "
            + "JOIN RUN r ON r.player_id = p.player_id "
            + "WHERE (? = FALSE OR r.run_id = p.high_score_run_id) "
            + "AND (? = '' OR CAST(p.player_id AS VARCHAR) LIKE ?) "
            + "AND (? = FALSE OR r.korokorobi = TRUE)",
        Integer.class,
        hsOnly,
        playerString,
        playerIdPattern,
        winnerOnly);

    // Ensure there is at least one page and keep requested page numbers in range.
    int totalPages = Math.max(1, (int) Math.ceil((double) totalResults / PAGE_SIZE));
    int currentPage = Math.max(1, Math.min(page, totalPages));
    long offset = (long) (currentPage - 1) * PAGE_SIZE;

    // Select each player's saved best run, sort highest scores first, then fetch this page.
    List<LeaderboardEntry> entries = jdbcTemplate.query(
        "SELECT p.player_id, r.score, r.steps, p.number_of_runs "
            + "FROM PLAYER p "
            + "JOIN RUN r ON r.player_id = p.player_id "
            + "WHERE (? = FALSE OR r.run_id = p.high_score_run_id) "
            + "AND (? = '' OR CAST(p.player_id AS VARCHAR) LIKE ?) "
            + "AND (? = FALSE OR r.korokorobi = TRUE)"
            + "ORDER BY r.score DESC, p.player_id ASC "
            + "LIMIT ? OFFSET ?",
        // Convert each returned database row into an object Thymeleaf can display.
        (resultSet, rowNumber) -> new LeaderboardEntry(
            resultSet.getInt("player_id"),
            resultSet.getInt("score"),
            resultSet.getInt("steps"),
            resultSet.getInt("number_of_runs")),
        hsOnly,
        playerString,
        playerIdPattern,
        winnerOnly,
        PAGE_SIZE,
        offset);

    // Make the results and paging information available to leaderboard.html.
    model.addAttribute("entries", entries);
    model.addAttribute("currentPage", currentPage);
    model.addAttribute("totalPages", totalPages);
    model.addAttribute("totalResults", totalResults);
    model.addAttribute("hasWon", winnerOnly);
    model.addAttribute("highscoreOnly", hsOnly);
    model.addAttribute("searchString", playerString);
    return "leaderboard";
  }

  private static boolean isEnabled(List<Boolean> values, boolean defaultValue) {
    if (values == null) {
      return defaultValue;
    }
    return values.stream().filter(Objects::nonNull).anyMatch(Boolean::booleanValue);
  }

  // A simple view model containing the values displayed for one leaderboard row.
  public static class LeaderboardEntry {
    private final int playerId;
    private final int score;
    private final int stepsRemaining;
    private final int numberOfRuns;

    public LeaderboardEntry(int playerId, int score, int stepsRemaining, int numberOfRuns) {
      this.playerId = playerId;
      this.score = score;
      this.stepsRemaining = stepsRemaining;
      this.numberOfRuns = numberOfRuns;
    }

    public int getPlayerId() {
      return playerId;
    }

    public int getScore() {
      return score;
    }

    public int getStepsRemaining() {
      return stepsRemaining;
    }

    public int getNumberOfRuns() {
      return numberOfRuns;
    }
  }
}
