package com.onep.internship.config;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxUploadSizeExceeded(RedirectAttributes redirectAttrs) {
        redirectAttrs.addFlashAttribute("error", "Le fichier dépasse la limite de 10 Mo. Veuillez choisir un fichier plus léger.");
        return "redirect:/apply";
    }
}
