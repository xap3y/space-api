package me.xap3y.space.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import me.xap3y.space.api.exception.BadRequestException;
import me.xap3y.space.api.iface.RequiresApiKey;
import me.xap3y.space.entity.User;
import me.xap3y.space.model.request.ResourcePreflightRequest;
import me.xap3y.space.model.response.DefaultResponse;
import me.xap3y.space.service.ResourceLimitService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/limits")
@RequiredArgsConstructor
public class ResourceQuotaPreflightController {

    private final ResourceLimitService resourceLimitService;

    @GetMapping("/file-pack")
    @RequiresApiKey
    public ResponseEntity<DefaultResponse> filePackLimits(HttpServletRequest request) {
        User uploader = (User) request.getAttribute("uploader");
        return ResponseEntity.ok(new DefaultResponse(false, resourceLimitService.getEffectiveFilePackLimits(uploader)));
    }

    @PostMapping("/preflight")
    @RequiresApiKey
    public ResponseEntity<DefaultResponse> preflight(
            HttpServletRequest request,
            @RequestBody ResourcePreflightRequest body
    ) {
        if (body == null || body.type() == null) throw new BadRequestException("Resource type is required");
        long count = body.count() == null ? 0 : body.count();
        long bytes = body.bytes() == null ? 0 : body.bytes();
        if (count <= 0) throw new BadRequestException("Resource count must be greater than 0");
        if (bytes < 0) throw new BadRequestException("Resource size cannot be negative");

        User uploader = (User) request.getAttribute("uploader");
        resourceLimitService.assertCanCreate(uploader, body.type(), count, bytes);
        return ResponseEntity.ok(new DefaultResponse(false, "Upload allowed"));
    }
}
