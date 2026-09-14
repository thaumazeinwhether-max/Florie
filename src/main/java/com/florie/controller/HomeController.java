package com.florie.controller;

import com.florie.service.UserService;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    private final UserService userService;

    public HomeController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/home")
    public String showHome(Principal principal, Model model) {
        model.addAttribute("nickname", userService.getNickname(principal.getName()));
        return "home";
    }
}
