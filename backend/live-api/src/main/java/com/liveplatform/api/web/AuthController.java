package com.liveplatform.api.web;

import com.liveplatform.api.service.AuthApplicationService;
import com.liveplatform.common.ApiResponse;
import com.liveplatform.user.api.LoginCodeDTO;
import com.liveplatform.user.api.UserBusinessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** 当前阶段直接验证 Bearer 会话，下一阶段再统一接入 Gateway 鉴权。 */
@RestController
@RequestMapping("/api")
public class AuthController {
    private final AuthApplicationService service;
    public AuthController(AuthApplicationService service) { this.service = service; }

    public record CodeRequest(String phone) { }
    public record LoginRequest(String phone, String code) { }
    public record LoginView(String token, String tokenType, long expiresInSeconds, UserView user) { }

    @PostMapping("/auth/code")
    public ResponseEntity<ApiResponse<LoginCodeDTO>> sendCode(@RequestBody CodeRequest request) {
        return response(service.sendCode(request.phone()));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<ApiResponse<LoginView>> login(@RequestBody LoginRequest request) {
        var result = service.login(request.phone(), request.code());
        return response(new LoginView(result.getToken(), "Bearer", result.getExpiresInSeconds(),
                UserView.from(result.getUser())));
    }

    @GetMapping("/users/me")
    public ResponseEntity<ApiResponse<UserView>> currentUser(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return response(UserView.from(service.currentUser(bearer(authorization))));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        service.logout(bearer(authorization));
        return response(null);
    }

    private String bearer(String authorization) {
        if (authorization == null || !authorization.matches("(?i:Bearer) [A-Za-z0-9_-]{43}")) {
            throw new UserBusinessException("AUTH_UNAUTHENTICATED", "请通过 Authorization: Bearer 携带登录凭证");
        }
        return authorization.substring(7);
    }

    private <T> ResponseEntity<ApiResponse<T>> response(T data) {
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(ApiResponse.success(data));
    }
}
