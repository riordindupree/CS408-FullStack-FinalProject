package com.example.springboot;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class AccountControllerAdvice {
  @ModelAttribute("loggedInPlayerId")
  public Integer loggedInPlayerId(HttpSession session) {
    Object playerId = session.getAttribute("playerId");
    return playerId instanceof Integer id ? id : null;
  }
}
