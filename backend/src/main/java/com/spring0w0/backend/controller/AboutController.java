package com.spring0w0.backend.controller;

import com.spring0w0.backend.common.Result;
import com.spring0w0.backend.pojo.vo.AboutVo;
import com.spring0w0.backend.service.AboutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公开关于页面接口。
 */
@RestController
@RequestMapping("/api/about")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "关于公开接口", description = "无需登录即可读取的关于页面内容")
public class AboutController {

    private final AboutService aboutService;

    @GetMapping
    @Operation(summary = "获取关于页面内容")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public Result<AboutVo> getAbout() {
        log.info("查询关于页面内容，请求参数：无");
        AboutVo about = aboutService.getAbout();
        log.info("查询关于页面内容完成，返回参数：标题长度={}", about.title() == null ? 0 : about.title().length());
        return Result.success(about);
    }
}
