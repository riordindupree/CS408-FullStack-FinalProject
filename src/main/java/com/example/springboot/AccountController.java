package com.example.springboot;

import jakarta.servlet.http.HttpSession;

import com.example.springboot.game_components.GameGrid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AccountController {
  private static final String PLAYER_ID_SESSION_ATTRIBUTE = "playerId";

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
    return "run-complete";
  }

  @PostMapping("/run/complete/create")
  public String chooseAccountCreation() {
    return gameGrid.isRunComplete()
        ? "redirect:/run/complete?create=true"
        : "redirect:/start";
  }

  @PostMapping("/account/create")
  public String createAccount(
      @RequestParam(required = false) String password,
      HttpSession session) {
    if (!gameGrid.isRunComplete()) {
      return "redirect:/start";
    }
    if (gameGrid.isRunSaved()) {
      return "redirect:/run/complete";
    }
    if (password == null || password.length() < 8) {
      return "redirect:/run/complete?create=true&passwordError=true";
    }

    int playerId = accountService.createAccountAndSaveRun(
        password,
        gameGrid.getCompletedStepsRemaining(),
        gameGrid.getCompletedScore(),
        gameGrid.isRunWon(),
        gameGrid.getInventoryIds());
    session.setAttribute(PLAYER_ID_SESSION_ATTRIBUTE, playerId);
    gameGrid.markRunSaved();
    return "redirect:/run/complete?playerId=" + playerId + "&saved=true";
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
}
