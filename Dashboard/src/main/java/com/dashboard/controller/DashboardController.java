package com.dashboard.controller;

import com.dashboard.service.BatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class DashboardController {

    @Autowired
    private BatchService batchService;

    @GetMapping("/")
    public String dashboard(Model model) {
        return "dashboard";
    }

    @PostMapping("/run-batch")
    public String runBatch(@RequestParam("batchType") String batchType, RedirectAttributes redirectAttributes) {
        try {
            batchService.runBatchOnEKS(batchType);
            redirectAttributes.addFlashAttribute("message", "Batch " + batchType + " started successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to start batch: " + e.getMessage());
        }
        return "redirect:/";
    }
}