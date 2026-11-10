package com.liveplatform.api.web;

import com.liveplatform.api.service.UserApplicationService;
import com.liveplatform.common.ApiResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** 指定用户 ID 的调试操作仅在显式启用 local 配置时开放，不代表当前登录用户。 */
@Profile("local")
@RestController
@RequestMapping("/api/dev/users")
public class DevUserController {
    private final UserApplicationService service;
    public DevUserController(UserApplicationService service) { this.service = service; }

    public record CreateRequest(String nickname, String avatarUrl) { }
    public record UpdateRequest(String nickname, String avatarUrl, Long version) { }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserView> create(@RequestBody CreateRequest request) {
        return ApiResponse.success(UserView.from(service.create(request.nickname(), request.avatarUrl())));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserView> get(@PathVariable Long id) {
        return ApiResponse.success(UserView.from(service.getById(id)));
    }

    @PutMapping("/{id}/profile")
    public ApiResponse<UserView> update(@PathVariable Long id, @RequestBody UpdateRequest request) {
        return ApiResponse.success(UserView.from(service.update(id, request.nickname(), request.avatarUrl(), request.version())));
    }
}
