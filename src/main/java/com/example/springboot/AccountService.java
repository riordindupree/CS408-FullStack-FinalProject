package com.example.springboot;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {
  private static final long RESERVATION_LIFETIME_SECONDS = 24 * 60 * 60;

  private final JdbcTemplate jdbcTemplate;
  private final PasswordHasher passwordHasher;

  public record AccountReservation(int playerId, String token) {}

  public AccountService(JdbcTemplate jdbcTemplate, PasswordHasher passwordHasher) {
    this.jdbcTemplate = jdbcTemplate;
    this.passwordHasher = passwordHasher;
  }

  @Transactional
  public int createAccountAndSaveRun(
      String password,
      int steps,
      int score,
      boolean won,
      int[] inventoryIds) {
    int playerId = createAccount(password);
    saveRun(playerId, steps, score, won, inventoryIds);
    return playerId;
  }

  @Transactional
  public int createAccount(String password) {
    return insertPlayer(passwordHasher.hash(password));
  }

  @Transactional
  public AccountReservation reserveAccountId() {
    Timestamp expiration = Timestamp.from(
        Instant.now().minusSeconds(RESERVATION_LIFETIME_SECONDS));
    List<Integer> expiredPlayerIds = jdbcTemplate.query(
        "SELECT player_id FROM ACCOUNT_RESERVATION WHERE created_at < ?",
        (resultSet, rowNumber) -> resultSet.getInt("player_id"),
        expiration);
    for (int expiredPlayerId : expiredPlayerIds) {
      jdbcTemplate.update("DELETE FROM PLAYER WHERE player_id = ?", expiredPlayerId);
    }

    String token = UUID.randomUUID().toString();
    int playerId = insertPlayer(passwordHasher.hash(UUID.randomUUID().toString()));
    jdbcTemplate.update(
        "INSERT INTO ACCOUNT_RESERVATION (player_id, reservation_token) VALUES (?, ?)",
        playerId,
        token);
    return new AccountReservation(playerId, token);
  }

  @Transactional
  public void completeAccountReservation(int playerId, String token, String password) {
    updateReservedAccountPassword(playerId, token, password);
  }

  @Transactional
  public int completeAccountReservationAndSaveRun(
      int playerId,
      String token,
      String password,
      int steps,
      int score,
      boolean won,
      int[] inventoryIds) {
    updateReservedAccountPassword(playerId, token, password);
    saveRun(playerId, steps, score, won, inventoryIds);
    return playerId;
  }

  private void updateReservedAccountPassword(int playerId, String token, String password) {
    int updated = jdbcTemplate.update(
        "UPDATE PLAYER SET password_hash = ? WHERE player_id = ? "
            + "AND EXISTS (SELECT 1 FROM ACCOUNT_RESERVATION "
            + "WHERE player_id = ? AND reservation_token = ?)",
        passwordHasher.hash(password),
        playerId,
        playerId,
        token);
    if (updated != 1) {
      throw new IllegalArgumentException("The account ID reservation is no longer valid.");
    }
    jdbcTemplate.update("DELETE FROM ACCOUNT_RESERVATION WHERE player_id = ?", playerId);
  }

  public boolean hasAccountReservation(int playerId, String token) {
    Integer count = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM ACCOUNT_RESERVATION WHERE player_id = ? AND reservation_token = ?",
        Integer.class,
        playerId,
        token);
    return count != null && count > 0;
  }

  public boolean authenticate(int playerId, String password) {
    List<String> passwordHashes = jdbcTemplate.query(
        "SELECT password_hash FROM PLAYER p WHERE player_id = ? "
            + "AND NOT EXISTS (SELECT 1 FROM ACCOUNT_RESERVATION r "
            + "WHERE r.player_id = p.player_id)",
        (resultSet, rowNumber) -> resultSet.getString("password_hash"),
        playerId);
    return !passwordHashes.isEmpty()
        && passwordHasher.matches(password, passwordHashes.get(0));
  }

  @Transactional
  public void deleteAccount(int playerId) {
    int playerExists = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM PLAYER WHERE player_id = ?",
        Integer.class,
        playerId);
    if (playerExists == 0) {
      throw new IllegalArgumentException("The player account does not exist.");
    }

    jdbcTemplate.update(
        "UPDATE PLAYER SET high_score_run_id = NULL WHERE player_id = ?",
        playerId);
    jdbcTemplate.update(
        "UPDATE RUN SET player_id = NULL WHERE player_id = ?",
        playerId);
    jdbcTemplate.update("DELETE FROM PLAYER WHERE player_id = ?", playerId);
  }

  private int insertPlayer(String passwordHash) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(connection -> {
      var statement = connection.prepareStatement(
          "INSERT INTO PLAYER (password_hash, number_of_runs) VALUES (?, 0)",
          new String[] {"player_id"});
      statement.setString(1, passwordHash);
      return statement;
    }, keyHolder);

    Number generatedId = keyHolder.getKey();
    if (generatedId == null) {
      throw new IllegalStateException("The database did not generate a player ID.");
    }
    return generatedId.intValue();
  }

  @Transactional
  public void saveRun(
      int playerId,
      int steps,
      int score,
      boolean won,
      int[] inventoryIds) {
    List<Integer> previousHighScores = jdbcTemplate.query(
        "SELECT r.score FROM PLAYER p "
            + "LEFT JOIN RUN r ON r.run_id = p.high_score_run_id "
            + "WHERE p.player_id = ?",
        (resultSet, rowNumber) -> resultSet.getObject("score", Integer.class),
        playerId);
    if (previousHighScores.isEmpty()) {
      throw new IllegalArgumentException("The player account does not exist.");
    }

    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(connection -> {
      var statement = connection.prepareStatement(
          "INSERT INTO RUN (player_id, steps, score, korokorobi) VALUES (?, ?, ?, ?)",
          new String[] {"run_id"});
      statement.setInt(1, playerId);
      statement.setInt(2, steps);
      statement.setInt(3, score);
      statement.setBoolean(4, won);
      return statement;
    }, keyHolder);

    Number generatedRunId = keyHolder.getKey();
    if (generatedRunId == null) {
      throw new IllegalStateException("The database did not generate a run ID.");
    }
    int runId = generatedRunId.intValue();
    saveInventory(runId, inventoryIds);

    Integer previousHighScore = previousHighScores.get(0);
    if (previousHighScore == null || score > previousHighScore) {
      jdbcTemplate.update(
          "UPDATE PLAYER SET high_score_run_id = ? WHERE player_id = ?",
          runId,
          playerId);
    }
    jdbcTemplate.update(
        "UPDATE PLAYER SET number_of_runs = number_of_runs + 1 WHERE player_id = ?",
        playerId);
  }

  private void saveInventory(int runId, int[] inventoryIds) {
    Map<Integer, Integer> items = new HashMap<>();
    Map<Integer, Integer> monsters = new HashMap<>();
    for (int inventoryId : inventoryIds) {
      if (inventoryId > 0) {
        items.merge(inventoryId, 1, Integer::sum);
      } else if (inventoryId < 0) {
        monsters.merge(-inventoryId, 1, Integer::sum);
      }
    }
    insertGained("GAINED_ITEM", "item_id", runId, items);
    insertGained("GAINED_MONSTER", "monster_id", runId, monsters);
  }

  private void insertGained(String table, String idColumn, int runId, Map<Integer, Integer> gained) {
    if (gained.isEmpty()) {
      return;
    }
    List<Object[]> rows = new ArrayList<>();
    gained.forEach((id, amount) -> rows.add(new Object[] {id, runId, amount}));
    jdbcTemplate.batchUpdate(
        "INSERT INTO " + table + " (" + idColumn + ", run_id, amount) VALUES (?, ?, ?)",
        rows);
  }
}
