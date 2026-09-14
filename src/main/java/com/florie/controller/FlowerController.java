package com.florie.controller;

import com.florie.form.FlowerRegisterForm;
import com.florie.service.FlowerRegistrationException;
import com.florie.service.FlowerService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class FlowerController {
    private final FlowerService flowerService;

    public FlowerController(FlowerService flowerService) {
        this.flowerService = flowerService;
    }

    @InitBinder("flowerRegisterForm")
    public void configureForm(WebDataBinder binder) {
        // ユーザーIDや登録日・状態は、リクエストから受け取らない。
        binder.setAllowedFields("flowerTypeId", "flowerNickname");
    }

    @GetMapping("/flowers/register")
    public String showRegisterForm(Principal principal, Model model) {
        if (flowerService.getCurrentFlower(principal.getName()).isPresent()) {
            return "redirect:/home?alreadyRegistered";
        }
        model.addAttribute("flowerRegisterForm", new FlowerRegisterForm());
        model.addAttribute("flowerTypes", flowerService.getFlowerTypes());
        return "flower-register";
    }

    @PostMapping("/flowers")
    public String registerFlower(Principal principal,
                                 @Valid @ModelAttribute FlowerRegisterForm form,
                                 BindingResult errors, Model model) {
        if (!errors.hasErrors()) {
            try {
                flowerService.registerFlower(principal.getName(), form);
                return "redirect:/home?flowerRegistered";
            } catch (FlowerRegistrationException exception) {
                errors.reject("registrationFailed", exception.getMessage());
            } catch (DataAccessException exception) {
                // ロック待ちや保存失敗でも、内部情報やSQLを表示しない。
                errors.reject("saveFailed", "登録できませんでした。ホームでお花の状態を確認してから、もう一度お試しください。");
            }
        }
        model.addAttribute("flowerTypes", flowerService.getFlowerTypes());
        return "flower-register";
    }
}
