package com.weightloss.planner.controller;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.weightloss.planner.model.PlanRequest;
import com.weightloss.planner.model.WeeklyPlan;
import com.weightloss.planner.service.PlannerService;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;

@Controller
public class PlannerController {
    private final PlannerService plannerService;
    private final TemplateEngine templateEngine;

    public PlannerController(PlannerService plannerService, TemplateEngine templateEngine) {
        this.plannerService = plannerService;
        this.templateEngine = templateEngine;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("request", new PlanRequest());
        return "index";
    }

    @PostMapping("/plan")
    public String createPlan(@ModelAttribute PlanRequest request, Model model) {
        try {
            model.addAttribute("plan", plannerService.generate(request));
        } catch (IllegalArgumentException exception) {
            model.addAttribute("error", exception.getMessage());
        }
        model.addAttribute("request", request);
        return "index";
    }

    @PostMapping("/plan/replace-meal")
    public String replaceMeal(@ModelAttribute PlanRequest request, @RequestParam int dayIndex,
                              @RequestParam int mealIndex, Model model) {
        model.addAttribute("plan", plannerService.replaceMeal(plannerService.generate(request), dayIndex, mealIndex));
        model.addAttribute("request", request);
        return "index";
    }

    @PostMapping("/plan/pdf")
    public ResponseEntity<byte[]> downloadPdf(@ModelAttribute PlanRequest request) throws Exception {
        WeeklyPlan plan = plannerService.generate(request);
        Context context = new Context();
        context.setVariable("plan", plan);
        String html = templateEngine.process("plan-pdf", context);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        new PdfRendererBuilder().withHtmlContent(html, null).toStream(output).run();
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=weekly-plan.pdf")
                .body(output.toByteArray());
    }
}
