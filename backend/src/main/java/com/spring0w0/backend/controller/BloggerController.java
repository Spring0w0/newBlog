package com.spring0w0.backend.controller;

import com.spring0w0.backend.common.Result;
import com.spring0w0.backend.pojo.vo.BloggerVo;
import com.spring0w0.backend.service.BloggerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 公开博主接口。
 */
@RestController
@RequestMapping("/api/bloggers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "博主公开接口", description = "无需登录即可读取的博主列表")
public class BloggerController {

    private final BloggerService bloggerService;

    @GetMapping
    @Operation(summary = "获取博主列表")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public Result<List<BloggerVo>> getBloggers() {
        log.info("查询博主列表，请求参数：无");
        List<BloggerVo> bloggers = bloggerService.getBloggers();
        log.info("查询博主列表完成，返回参数：博主数量={}", bloggers.size());
        return Result.success(bloggers);
    }
}
