package com.cloudopsguard.observability;

import com.cloudopsguard.security.AppUserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

/**
 * 1リクエストを横断追跡するための相関 ID を MDC に載せる（B2・共通設計方針）。
 *
 * <ul>
 *   <li>{@code requestId}：受信した {@code X-Request-Id} を尊重し、無ければ UUID を採番。レスポンスにも返す。</li>
 *   <li>{@code userId}：検証済み {@link AppUserPrincipal} から導出（未認証なら付けない）。</li>
 * </ul>
 *
 * <p>{@link com.cloudopsguard.security.JwtCookieAuthFilter} の後段に挿す（SecurityContext 確立後）。
 * 機密（パスワード・トークン）は MDC に載せない。
 */
@Component
public class MdcLoggingFilter extends OncePerRequestFilter {

    static final String REQUEST_ID = "requestId";
    static final String USER_ID = "userId";
    static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = resolveRequestId(request);
        try {
            MDC.put(REQUEST_ID, requestId);
            response.setHeader(REQUEST_ID_HEADER, requestId);
            currentUserId().ifPresent(id -> MDC.put(USER_ID, id));
            filterChain.doFilter(request, response);
        } finally {
            // スレッドプール再利用で次リクエストに漏れないよう必ず掃除する。
            MDC.remove(REQUEST_ID);
            MDC.remove(USER_ID);
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        String incoming = request.getHeader(REQUEST_ID_HEADER);
        return StringUtils.hasText(incoming) ? incoming : UUID.randomUUID().toString();
    }

    private Optional<String> currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AppUserPrincipal principal) {
            return Optional.of(String.valueOf(principal.userId()));
        }
        return Optional.empty();
    }
}
