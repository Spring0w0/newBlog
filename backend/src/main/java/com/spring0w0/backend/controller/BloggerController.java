package com.spring0w0.backend.controller;

import com.spring0w0.backend.common.Result;
import com.spring0w0.backend.config.OpenApiConfig;
import com.spring0w0.backend.pojo.dto.BloggerWriteRequest;
import com.spring0w0.backend.pojo.vo.AdminBloggerVo;
import com.spring0w0.backend.pojo.vo.BloggerVo;
import com.spring0w0.backend.service.BloggerService;
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

/** 博主公开读取与管理员维护接口。 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "博主接口", description = "公开博主展示，以及管理员的博主维护")
public class BloggerController {

    private final BloggerService bloggerService;

    @GetMapping("/bloggers")
    @Operation(summary = "获取博主列表")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public Result<List<BloggerVo>> getBloggers() {
        log.info("查询博主列表，请求参数：无");
        List<BloggerVo> bloggers = bloggerService.getBloggers();
        log.info("查询博主列表完成，返回参数：博主数量={}", bloggers.size());
        return Result.success(bloggers);
    }

    @GetMapping("/admin/bloggers")
    @Operation(summary = "获取管理员博主列表")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "401", description = "未登录或 Token 无效"),
            @ApiResponse(responseCode = "403", description = "非管理员")
    })
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
    public Result<List<AdminBloggerVo>> getAdminBloggers() {
        log.info("查询管理员博主列表，请求参数：无");
        List<AdminBloggerVo> bloggers = bloggerService.getAdminBloggers();
        log.info("查询管理员博主列表完成，返回参数：博主数量={}", bloggers.size());
        return Result.success(bloggers);
    }

    @PostMapping("/admin/bloggers")
    @Operation(summary = "创建博主")
    @ApiResponse(responseCode = "200", description = "创建成功")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
    public Result<AdminBloggerVo> createBlogger(@Valid @RequestBody BloggerWriteRequest request) {
        log.info("创建博主，请求参数：名称={}，头像URL长度={}，站点URL={}，简介长度={}，星级={}，状态={}",
                request.name(), request.avatar().length(), request.url(), request.description().length(), request.stars(), request.status());
        AdminBloggerVo blogger = bloggerService.createBlogger(request);
        log.info("创建博主完成，返回参数：博主ID={}，名称={}", blogger.id(), blogger.name());
        return Result.success(blogger);
    }

    @PutMapping("/admin/bloggers/{id}")
    @Operation(summary = "更新博主")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
    public Result<AdminBloggerVo> updateBlogger(
            @PathVariable @Positive(message = "博主 ID 必须为正整数") Long id,
            @Valid @RequestBody BloggerWriteRequest request
    ) {
        log.info("更新博主，请求参数：博主ID={}，名称={}，头像URL长度={}，站点URL={}，简介长度={}，星级={}，状态={}",
                id, request.name(), request.avatar().length(), request.url(), request.description().length(), request.stars(), request.status());
        AdminBloggerVo blogger = bloggerService.updateBlogger(id, request);
        log.info("更新博主完成，返回参数：博主ID={}，名称={}", blogger.id(), blogger.name());
        return Result.success(blogger);
    }

    @DeleteMapping("/admin/bloggers/{id}")
    @Operation(summary = "删除博主", description = "删除博主记录，不会直接删除其头像文件实体")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
    public Result<Void> deleteBlogger(@PathVariable @Positive(message = "博主 ID 必须为正整数") Long id) {
        log.info("删除博主，请求参数：博主ID={}", id);
        bloggerService.deleteBlogger(id);
        log.info("删除博主完成，返回参数：博主ID={}", id);
        return Result.success();
    }
}
