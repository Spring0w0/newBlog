package com.spring0w0.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.spring0w0.backend.common.Result;
import com.spring0w0.backend.config.OpenApiConfig;
import com.spring0w0.backend.pojo.dto.SiteSettingsSaveRequest;
import com.spring0w0.backend.pojo.vo.SiteSettingsVo;
import com.spring0w0.backend.service.SiteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 公开站点配置读取与管理员首页设置维护接口。 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "站点接口", description = "公开读取站点与首页配置，以及管理员保存站点设置")
public class SiteController {

    private final SiteService siteService;

    @GetMapping("/site/config")
    @Operation(summary = "获取站点配置", description = "返回与前端 site-content 配置兼容的 JSON 对象。")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public Result<JsonNode> getSiteConfig() {
        log.info("查询站点配置，请求参数：无");
        JsonNode siteConfig = siteService.getSiteConfig();
        log.info("查询站点配置完成，返回参数：字段数量={}", siteConfig.size());
        return Result.success(siteConfig);
    }

    @GetMapping("/site/card-styles")
    @Operation(summary = "获取卡片样式", description = "返回以卡片 key 为属性名的样式对象。")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public Result<JsonNode> getCardStyles() {
        log.info("查询卡片样式，请求参数：无");
        JsonNode cardStyles = siteService.getCardStyles();
        log.info("查询卡片样式完成，返回参数：卡片数量={}", cardStyles.size());
        return Result.success(cardStyles);
    }

    @PutMapping("/admin/site/config")
    @Operation(summary = "保存完整站点配置", description = "请求体与公开站点配置结构相同；会同步保存图片、背景与社交按钮引用。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "保存成功"),
            @ApiResponse(responseCode = "400", description = "配置字段、颜色、卡片或图片列表参数不合法"),
            @ApiResponse(responseCode = "401", description = "未登录或 Token 无效"),
            @ApiResponse(responseCode = "403", description = "非管理员"),
            @ApiResponse(responseCode = "422", description = "受管图片不存在或 scope 不匹配")
    })
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
    public Result<JsonNode> saveSiteConfig(@RequestBody JsonNode request) {
        log.info("保存站点配置，请求参数：字段数量={}", request == null ? 0 : request.size());
        JsonNode siteConfig = siteService.saveSiteConfig(request);
        log.info("保存站点配置完成，返回参数：字段数量={}", siteConfig.size());
        return Result.success(siteConfig);
    }

    @PutMapping("/admin/site/card-styles")
    @Operation(summary = "保存完整卡片样式", description = "请求体与公开卡片样式结构相同。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "保存成功"),
            @ApiResponse(responseCode = "400", description = "卡片 key 或样式参数不合法"),
            @ApiResponse(responseCode = "401", description = "未登录或 Token 无效"),
            @ApiResponse(responseCode = "403", description = "非管理员")
    })
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
    public Result<JsonNode> saveCardStyles(@RequestBody JsonNode request) {
        log.info("保存卡片样式，请求参数：卡片数量={}", request == null ? 0 : request.size());
        JsonNode cardStyles = siteService.saveCardStyles(request);
        log.info("保存卡片样式完成，返回参数：卡片数量={}", cardStyles.size());
        return Result.success(cardStyles);
    }

    @PutMapping("/admin/site/settings")
    @Operation(summary = "原子保存站点设置", description = "一次性保存站点配置和卡片样式；配置弹窗默认使用此接口，避免部分保存。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "保存成功"),
            @ApiResponse(responseCode = "400", description = "配置字段、颜色、卡片或图片列表参数不合法"),
            @ApiResponse(responseCode = "401", description = "未登录或 Token 无效"),
            @ApiResponse(responseCode = "403", description = "非管理员"),
            @ApiResponse(responseCode = "422", description = "受管图片不存在或 scope 不匹配")
    })
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
    public Result<SiteSettingsVo> saveSiteSettings(@Valid @RequestBody SiteSettingsSaveRequest request) {
        log.info("原子保存站点设置，请求参数：站点字段数量={}，卡片数量={}", request.config().size(), request.cardStyles().size());
        SiteSettingsVo settings = siteService.saveSiteSettings(request);
        log.info("原子保存站点设置完成，返回参数：站点字段数量={}，卡片数量={}", settings.config().size(), settings.cardStyles().size());
        return Result.success(settings);
    }
}
