package com.spring0w0.backend.controller;

import com.spring0w0.backend.common.Result;
import com.spring0w0.backend.config.OpenApiConfig;
import com.spring0w0.backend.pojo.dto.ShareWriteRequest;
import com.spring0w0.backend.pojo.vo.AdminShareVo;
import com.spring0w0.backend.pojo.vo.ShareVo;
import com.spring0w0.backend.service.ShareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 友链公开读取与管理员维护接口。 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "友链接口", description = "公开友链展示，以及管理员的友链维护")
public class ShareController {

    private final ShareService shareService;

    @GetMapping("/shares")
    @Operation(summary = "获取友链列表")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public Result<List<ShareVo>> getShares() {
        log.info("查询友链列表，请求参数：无");
        List<ShareVo> shares = shareService.getShares();
        log.info("查询友链列表完成，返回参数：友链数量={}", shares.size());
        return Result.success(shares);
    }

    @GetMapping("/admin/shares")
    @Operation(summary = "获取管理员友链列表")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "401", description = "未登录或 Token 无效"),
            @ApiResponse(responseCode = "403", description = "非管理员")
    })
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
    public Result<List<AdminShareVo>> getAdminShares() {
        log.info("查询管理员友链列表，请求参数：无");
        List<AdminShareVo> shares = shareService.getAdminShares();
        log.info("查询管理员友链列表完成，返回参数：友链数量={}", shares.size());
        return Result.success(shares);
    }

    @PostMapping("/admin/shares")
    @Operation(summary = "创建友链")
    @ApiResponse(responseCode = "200", description = "创建成功")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
    public Result<AdminShareVo> createShare(@Valid @RequestBody ShareWriteRequest request) {
        log.info("创建友链，请求参数：名称={}，LogoURL长度={}，站点URL={}，标签数量={}，简介长度={}，星级={}",
                request.name(), request.logo().length(), request.url(),
                request.tags() == null ? 0 : request.tags().size(), request.description().length(), request.stars());
        AdminShareVo share = shareService.createShare(request);
        log.info("创建友链完成，返回参数：友链ID={}，名称={}", share.id(), share.name());
        return Result.success(share);
    }

    @PutMapping("/admin/shares/{id}")
    @Operation(summary = "更新友链")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
    public Result<AdminShareVo> updateShare(
            @PathVariable @Positive(message = "友链 ID 必须为正整数") Long id,
            @Valid @RequestBody ShareWriteRequest request
    ) {
        log.info("更新友链，请求参数：友链ID={}，名称={}，LogoURL长度={}，站点URL={}，标签数量={}，简介长度={}，星级={}",
                id, request.name(), request.logo().length(), request.url(),
                request.tags() == null ? 0 : request.tags().size(), request.description().length(), request.stars());
        AdminShareVo share = shareService.updateShare(id, request);
        log.info("更新友链完成，返回参数：友链ID={}，名称={}", share.id(), share.name());
        return Result.success(share);
    }

    @DeleteMapping("/admin/shares/{id}")
    @Operation(summary = "删除友链", description = "删除友链记录，不会直接删除其 Logo 文件实体")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
    public Result<Void> deleteShare(@PathVariable @Positive(message = "友链 ID 必须为正整数") Long id) {
        log.info("删除友链，请求参数：友链ID={}", id);
        shareService.deleteShare(id);
        log.info("删除友链完成，返回参数：友链ID={}", id);
        return Result.success();
    }
}
