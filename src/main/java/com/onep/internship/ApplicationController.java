package com.onep.internship;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ApplicationController {

    @GetMapping("/apply")
    public String apply_redirect() {
        return "redirect:/applicant/apply";
    }

    @GetMapping("/success")
    public String success() {
        return "success";
    }
}