package com.example.blog.shared.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RequestTraceFilter extends OncePerRequestFilter {

    public static final String TRACE_ID = "traceId";
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestTraceFilter.class);

    private final SecretKey secretKey;

    public RequestTraceFilter(SecretKey secretKey) {
        this.secretKey = secretKey;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {
        String traceId = validTraceId(request.getHeader("X-Trace-ID"));
        long started = System.nanoTime();
        MDC.put(TRACE_ID, traceId);
        request.setAttribute(TRACE_ID, traceId);
        response.setHeader("X-Trace-ID", traceId);
        try {
            chain.doFilter(request, response);
        } finally {
            long elapsedMillis = (System.nanoTime() - started) / 1_000_000;
            LOGGER.info(
                    "request method={} path={} status={} durationMs={} client={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    elapsedMillis,
                    dailyClientHash(request)
            );
            MDC.remove(TRACE_ID);
        }
    }

    private String validTraceId(String value) {
        if (value != null && value.matches("[A-Za-z0-9_-]{8,64}")) return value;
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String dailyClientHash(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        String ip = forwarded == null || forwarded.isBlank()
                ? request.getRemoteAddr()
                : forwarded.split(",")[0].trim();
        String value = LocalDate.now(ZoneOffset.UTC) + ":" + (ip == null ? "unknown" : ip);
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(secretKey);
            return HexFormat.of().formatHex(
                    mac.doFinal(value.getBytes(StandardCharsets.UTF_8))
            ).substring(0, 16);
        } catch (Exception exception) {
            return "unavailable";
        }
    }
}
