package com.spring0w0.backend.controller;

import com.spring0w0.backend.common.Result;
import com.spring0w0.backend.config.OpenApiConfig;
import com.spring0w0.backend.pojo.dto.AboutSaveRequest;
import com.spring0w0.backend.pojo.vo.AboutVo;
import com.spring0w0.backend.service.AboutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公开关于页面接口。
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "关于公开接口", description = "无需登录即可读取的关于页面内容")
public class AboutController {

    private final AboutService aboutService;

    @GetMapping("/about")
    @Operation(summary = "获取关于页面内容")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public Result<AboutVo> getAbout() {
        log.info("查询关于页面内容，请求参数：无");
        AboutVo about = aboutService.getAbout();
        log.info("查询关于页面内容完成，返回参数：标题长度={}", about.title() == null ? 0 : about.title().length());
        return Result.success(about);
    }

    @PutMapping("/admin/about")
    @Operation(summary = "保存关于页面", description = "整体替换关于页标题、简介和 Markdown 正文。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "保存成功"),
            @ApiResponse(responseCode = "400", description = "请求参数不合法"),
            @ApiResponse(responseCode = "401", description = "未登录或 Token 无效"),
            @ApiResponse(responseCode = "403", description = "非管理员")
    })
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
    public Result<AboutVo> saveAbout(@Valid @RequestBody AboutSaveRequest request) {
        log.info("保存关于页面，请求参数：标题长度={}，简介长度={}，正文长度={}",
                request.title().length(), request.description().length(), request.content().length());
        AboutVo about = aboutService.saveAbout(request);
        log.info("保存关于页面完成，返回参数：标题长度={}，简介长度={}，正文长度={}",
                about.title().length(), about.description().length(), about.content().length());
        return Result.success(about);
    }
}
