package com.exam.gateway.filter;

import com.exam.common.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 1. 放行登录注册接口
        if (path.contains("/api/auth/login") || path.contains("/api/auth/register") || path.contains("/doc.html") || path.contains("/v2/api-docs")) {
            return chain.filter(exchange);
        }

        // 2. 获取 Authorization Header
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorizedResponse(exchange);
        }

        String token = authHeader.substring(7);
        // 3. 校验 JWT Token
        if (!JwtUtil.validateToken(token)) {
            return unauthorizedResponse(exchange);
        }

        try {
            Claims claims = JwtUtil.parseToken(token);
            String role = (String) claims.get("role");
            String username = (String) claims.get("username");
            Long userId = ((Number) claims.get("userId")).longValue();

            // 4. 网关角色控制
            if (path.contains("/api/exam/create") || path.contains("/api/question/add") || path.contains("/api/exam/publish")) {
                if (!"TEACHER".equalsIgnoreCase(role)) {
                    ServerHttpResponse response = exchange.getResponse();
                    response.setStatusCode(HttpStatus.FORBIDDEN);
                    return response.setComplete();
                }
            }

            // 5. 将解析出的用户信息透传给子服务
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id", userId.toString())
                    .header("X-User-Name", username)
                    .header("X-User-Role", role)
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        } catch (Exception e) {
            return unauthorizedResponse(exchange);
        }
    }

    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return response.setComplete();
    }

    @Override
    public int getOrder() {
        return -1; // 优先级最高
    }
}
