package com.library.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.dto.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Central guard for paging parameters on every endpoint: "page" must be a non-negative
 * integer, "size"/"limit" must be within [1, app.security.max-page-size]. Rejecting
 * oversized pages up front keeps a single request from loading whole tables into memory.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class PaginationGuardFilter extends OncePerRequestFilter {

    private final int maxPageSize;
    private final ObjectMapper objectMapper;

    public PaginationGuardFilter(@Value("${app.security.max-page-size:100}") int maxPageSize,
                                 ObjectMapper objectMapper) {
        this.maxPageSize = maxPageSize;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String error = check(request.getParameter("page"), 0, Integer.MAX_VALUE, "page");
        if (error == null) error = check(request.getParameter("size"), 1, maxPageSize, "size");
        if (error == null) error = check(request.getParameter("limit"), 1, maxPageSize, "limit");
        if (error != null) {
            log.warn("Rejected request with invalid paging parameter: {} {}", request.getRequestURI(), error);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            objectMapper.writeValue(response.getOutputStream(),
                    ApiResponse.error("درخواست نامعتبر", "پارامترهای صفحه‌بندی نامعتبر است"));
            return;
        }
        chain.doFilter(request, response);
    }

    private static String check(String raw, int min, int max, String name) {
        if (raw == null) return null;
        try {
            int value = Integer.parseInt(raw.trim());
            return (value < min || value > max) ? name + " out of range" : null;
        } catch (NumberFormatException e) {
            return name + " not a number";
        }
    }
}
