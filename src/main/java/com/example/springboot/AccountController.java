package com.example.springboot;

import jakarta.servlet.http.HttpSession;

import java.util.regex.Pattern;

import com.example.springboot.game_components.GameGrid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AccountController {
  private static final String PLAYER_ID_SESSION_ATTRIBUTE = "playerId";
  private static final String RESERVED_PLAYER_ID_SESSION_ATTRIBUTE = "reservedPlayerId";
  private static final String RESERVATION_TOKEN_SESSION_ATTRIBUTE = "accountReservationToken";
  private static final Pattern STRONG_PASSWORD = Pattern.compile(
      "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$");

  private final AccountService accountService;
  private final GameGrid gameGrid;

  public AccountController(AccountService accountService, GameGrid gameGrid) {
    this.accountService = accountService;
    this.gameGrid = gameGrid;
  }

  @GetMapping("/login")
  public String login() {
    return "login";
  }

  @GetMapping("/account/create")
  public String createAccountPage(HttpSession session, Model model) {
    AccountService.AccountReservation reservation = reservationFor(session);
    model.addAttribute("reservedAccountId", reservation.playerId());
    return "create-account";
  }

  @PostMapping("/logout")
  public String logout(HttpSession session) {
    session.removeAttribute(PLAYER_ID_SESSION_ATTRIBUTE);
    return "redirect:/";
  }

  @PostMapping("/login")
  public String authenticate(
      @RequestParam(required = false) Integer playerId,
      @RequestParam(required = false) String password,
      @RequestParam(defaultValue = "false") boolean runComplete,
      HttpSession session) {
    if (playerId == null || password == null || password.isBlank()
        || !accountService.authenticate(playerId, password)) {
      return runComplete
          ? "redirect:/login?error=true&runComplete=true"
          : "redirect:/login?error=true";
    }

    session.setAttribute(PLAYER_ID_SESSION_ATTRIBUTE, playerId);
    if (gameGrid.isRunComplete() && !gameGrid.isRunSaved()) {
      saveCompletedRun(playerId);
      return "redirect:/run/complete?saved=true";
    }
    return gameGrid.isRunComplete()
        ? "redirect:/run/complete"
        : "redirect:/start";
  }

  @GetMapping("/run/complete")
  public String runComplete(
      @RequestParam(required = false) Integer playerId,
      @RequestParam(required = false) boolean saved,
      @RequestParam(required = false) boolean passwordError,
      @RequestParam(required = false) boolean create,
      HttpSession session,
      Model model) {
    if (!gameGrid.isRunComplete()) {
      return "redirect:/start";
    }
    model.addAttribute("score", gameGrid.getCompletedScore());
    model.addAttribute("won", gameGrid.isRunWon());
    model.addAttribute("saved", gameGrid.isRunSaved() || saved);
    model.addAttribute("playerId", playerId);
    model.addAttribute("passwordError", passwordError);
    model.addAttribute("create", create);
    if (create && session.getAttribute(PLAYER_ID_SESSION_ATTRIBUTE) == null) {
      AccountService.AccountReservation reservation = reservationFor(session);
      model.addAttribute("reservedAccountId", reservation.playerId());
    }
    return "run-complete";
  }

  @PostMapping("/run/complete/create")
  public String chooseAccountCreation(HttpSession session) {
    if (!gameGrid.isRunComplete()) {
      return "redirect:/start";
    }
    if (session.getAttribute(PLAYER_ID_SESSION_ATTRIBUTE) == null) {
      reservationFor(session);
    }
    return "redirect:/run/complete?create=true";
  }

  @PostMapping("/account/create")
  public String createAccount(
      @RequestParam(required = false) String password,
      HttpSession session) {
    boolean runComplete = gameGrid.isRunComplete();
    if (runComplete && gameGrid.isRunSaved()) {
      return "redirect:/run/complete";
    }
    if (password == null || !STRONG_PASSWORD.matcher(password).matches()) {
      return runComplete
          ? "redirect:/run/complete?create=true&passwordError=true"
          : "redirect:/account/create?error=true";
    }

    Integer reservedPlayerId = (Integer) session.getAttribute(
        RESERVED_PLAYER_ID_SESSION_ATTRIBUTE);
    String reservationToken = (String) session.getAttribute(RESERVATION_TOKEN_SESSION_ATTRIBUTE);
    boolean hasReservation = reservedPlayerId != null
        && reservationToken != null
        && accountService.hasAccountReservation(reservedPlayerId, reservationToken);
    int playerId;
    if (runComplete) {
      playerId = hasReservation
          ? accountService.completeAccountReservationAndSaveRun(
              reservedPlayerId,
              reservationToken,
              password,
              gameGrid.getCompletedStepsRemaining(),
              gameGrid.getCompletedScore(),
              gameGrid.isRunWon(),
              gameGrid.getInventoryIds())
          : accountService.createAccountAndSaveRun(
              password,
              gameGrid.getCompletedStepsRemaining(),
              gameGrid.getCompletedScore(),
              gameGrid.isRunWon(),
              gameGrid.getInventoryIds());
      gameGrid.markRunSaved();
    } else {
      if (hasReservation) {
        accountService.completeAccountReservation(reservedPlayerId, reservationToken, password);
        playerId = reservedPlayerId;
      } else {
        playerId = accountService.createAccount(password);
      }
    }
    session.removeAttribute(RESERVED_PLAYER_ID_SESSION_ATTRIBUTE);
    session.removeAttribute(RESERVATION_TOKEN_SESSION_ATTRIBUTE);
    session.setAttribute(PLAYER_ID_SESSION_ATTRIBUTE, playerId);
    return runComplete
        ? "redirect:/run/complete?playerId=" + playerId + "&saved=true"
        : "redirect:/account/created?playerId=" + playerId;
  }

  @GetMapping("/account/created")
  public String accountCreated(@RequestParam int playerId, Model model) {
    model.addAttribute("playerId", playerId);
    return "account-created";
  }

  @PostMapping("/delete")
  public String deleteAccount(
      @RequestParam(required = false) Integer playerId,
      @RequestParam(required = false) String password,
      @RequestParam(defaultValue = "false") boolean confirmDelete,
      HttpSession session) {
    if (playerId == null || password == null || password.isBlank()
        || !confirmDelete || !accountService.authenticate(playerId, password)) {
      return "redirect:/delete?error=true";
    }

    accountService.deleteAccount(playerId);
    if (playerId.equals(session.getAttribute(PLAYER_ID_SESSION_ATTRIBUTE))) {
      session.removeAttribute(PLAYER_ID_SESSION_ATTRIBUTE);
    }
    return "redirect:/delete?deleted=true";
  }

  private void saveCompletedRun(int playerId) {
    accountService.saveRun(
        playerId,
        gameGrid.getCompletedStepsRemaining(),
        gameGrid.getCompletedScore(),
        gameGrid.isRunWon(),
        gameGrid.getInventoryIds());
    gameGrid.markRunSaved();
  }

  private AccountService.AccountReservation reservationFor(HttpSession session) {
    Integer playerId = (Integer) session.getAttribute(RESERVED_PLAYER_ID_SESSION_ATTRIBUTE);
    String token = (String) session.getAttribute(RESERVATION_TOKEN_SESSION_ATTRIBUTE);
    if (playerId != null && token != null && accountService.hasAccountReservation(playerId, token)) {
      return new AccountService.AccountReservation(playerId, token);
    }

    AccountService.AccountReservation reservation = accountService.reserveAccountId();
    session.setAttribute(RESERVED_PLAYER_ID_SESSION_ATTRIBUTE, reservation.playerId());
    session.setAttribute(RESERVATION_TOKEN_SESSION_ATTRIBUTE, reservation.token());
    return reservation;
  }
}
