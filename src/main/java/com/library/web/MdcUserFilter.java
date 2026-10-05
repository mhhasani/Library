package com.library.web;

import com.library.util.SecurityUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Adds the authenticated user's id to the logging MDC (registered inside the security chain). */
public class MdcUserFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId != null) {
            MDC.put("userId", String.valueOf(userId));
        }
        chain.doFilter(request, response);
    }
}
