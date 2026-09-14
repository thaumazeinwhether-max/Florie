package com.florie.controller;

import com.florie.form.UserRegisterForm;
import com.florie.service.EmailAlreadyRegisteredException;
import com.florie.service.UserService;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {
    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("userRegisterForm", new UserRegisterForm());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute UserRegisterForm form, BindingResult errors) {
        if (errors.hasErrors()) {
            form.clearPasswords();
            return "register";
        }
        try {
            userService.registerUser(form);
        } catch (EmailAlreadyRegisteredException exception) {
            errors.rejectValue("email", "duplicate", exception.getMessage());
        } catch (DataIntegrityViolationException exception) {
            // 同時送信等によるDB制約違反でも、SQLや内部情報を画面に出さない。
            errors.reject("registrationFailed", "登録できませんでした。メールアドレスが既に使用されていないか確認してください。");
        } finally {
            form.clearPasswords();
        }
        if (errors.hasErrors()) {
            return "register";
        }
        return "redirect:/login?registered";
    }
}
