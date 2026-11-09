package com.liveplatform.api.web;

import com.liveplatform.api.service.SystemProbeService;
import com.liveplatform.common.ApiResponse;
import com.liveplatform.user.api.UserProbeResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class SystemController {
    private final SystemProbeService probeService;

    public SystemController(SystemProbeService probeService) {
        this.probeService = probeService;
    }

    @GetMapping("/user-service")
    public ApiResponse<UserProbeResult> userService() {
        return ApiResponse.success(probeService.probe());
    }
}
