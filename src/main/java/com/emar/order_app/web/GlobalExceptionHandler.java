package com.emar.order_app.web;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDenied(AccessDeniedException ex, HttpServletRequest req, HttpServletResponse res) {
        res.setStatus(403);
        req.setAttribute("errorMessage", ex.getMessage());
        return "error/403";
    }

    @ExceptionHandler({IllegalStateException.class, IllegalArgumentException.class})
    public String handleBadRequest(RuntimeException ex, HttpServletRequest req, HttpServletResponse res) {
        res.setStatus(400);
        req.setAttribute("errorMessage", ex.getMessage());
        return "error/400";
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneric(Exception ex, HttpServletRequest req, HttpServletResponse res) {
        res.setStatus(500);
        req.setAttribute("errorMessage", ex.getMessage());
        return "error/500";
    }
}
