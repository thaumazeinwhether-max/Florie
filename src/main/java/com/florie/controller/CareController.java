package com.florie.controller;

import com.florie.service.CareService;
import com.florie.service.CareCompletionException;
import java.security.Principal;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CareController {
    private final CareService careService;

    public CareController(CareService careService) {
        this.careService = careService;
    }

    @PostMapping("/care-tasks/{taskId}/complete")
    public String completeCare(@PathVariable Long taskId, Principal principal, RedirectAttributes redirect) {
        try {
            careService.completeCare(principal.getName(), taskId);
            redirect.addFlashAttribute("careCompleted", "お世話を記録しました。");
        } catch (CareCompletionException exception) {
            redirect.addFlashAttribute("careError", exception.getMessage());
        } catch (DataAccessException exception) {
            redirect.addFlashAttribute("careError", "記録できませんでした。ホームを確認してから、もう一度お試しください。");
        }
        return "redirect:/home";
    }
}
