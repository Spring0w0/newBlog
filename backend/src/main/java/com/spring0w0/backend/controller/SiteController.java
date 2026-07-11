package com.spring0w0.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.spring0w0.backend.common.Result;
import com.spring0w0.backend.service.SiteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公开站点配置接口。
 */
@RestController
@RequestMapping("/api/site")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "站点公开接口", description = "无需登录即可读取的站点与首页配置")
public class SiteController {

    private final SiteService siteService;

    @GetMapping("/config")
    @Operation(summary = "获取站点配置", description = "返回与前端 site-content 配置兼容的 JSON 对象。")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public Result<JsonNode> getSiteConfig() {
        log.info("查询站点配置，请求参数：无");
        JsonNode siteConfig = siteService.getSiteConfig();
        log.info("查询站点配置完成，返回参数：字段数量={}", siteConfig.size());
        return Result.success(siteConfig);
    }

    @GetMapping("/card-styles")
    @Operation(summary = "获取卡片样式", description = "返回以卡片 key 为属性名的样式对象。")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public Result<JsonNode> getCardStyles() {
        log.info("查询卡片样式，请求参数：无");
        JsonNode cardStyles = siteService.getCardStyles();
        log.info("查询卡片样式完成，返回参数：卡片数量={}", cardStyles.size());
        return Result.success(cardStyles);
    }
}
