package com.onep.internship.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxUploadSizeExceeded(RedirectAttributes redirectAttrs) {
        redirectAttrs.addFlashAttribute("error", "Le fichier dépasse la limite de 10 Mo. Veuillez choisir un fichier plus léger.");
        return "redirect:/applicant/apply";
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneric(Exception e, RedirectAttributes redirectAttrs) {
        log.warn("Unhandled exception redirected to error page: {}", e.getClass().getSimpleName());
        return "redirect:/home/login?error";
    }
}
