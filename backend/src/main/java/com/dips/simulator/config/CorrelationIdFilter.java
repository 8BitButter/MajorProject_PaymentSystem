package com.dips.simulator.config;

import com.dips.simulator.logging.LogContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String incoming = request.getHeader(LogContext.CORRELATION_ID_HEADER);
        String correlationId = (incoming == null || incoming.isBlank())
                ? "corr-" + UUID.randomUUID()
                : incoming;

        LogContext.setCorrelationId(correlationId);
        response.setHeader(LogContext.CORRELATION_ID_HEADER, correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            LogContext.clearTransactionId();
            LogContext.clearCorrelationId();
        }
    }
}
