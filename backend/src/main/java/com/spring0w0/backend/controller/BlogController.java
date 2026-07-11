package com.spring0w0.backend.controller;

import com.spring0w0.backend.common.Result;
import com.spring0w0.backend.common.ResultCode;
import com.spring0w0.backend.exception.BusinessException;
import com.spring0w0.backend.pojo.vo.BlogDetailVo;
import com.spring0w0.backend.pojo.vo.BlogSummaryVo;
import com.spring0w0.backend.service.BlogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 公开文章和分类接口。
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "文章公开接口", description = "无需登录即可读取公开文章与分类")
public class BlogController {

    private final BlogService blogService;

    @GetMapping("/blogs")
    @Operation(summary = "获取公开文章列表", description = "按发布时间倒序返回所有公开文章。")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public Result<List<BlogSummaryVo>> getBlogs() {
        log.info("查询公开文章列表，请求参数：无");
        List<BlogSummaryVo> blogs = blogService.getPublishedBlogs();
        log.info("查询公开文章列表完成，返回参数：文章数量={}", blogs.size());
        return Result.success(blogs);
    }

    @GetMapping("/blogs/{slug}")
    @Operation(summary = "获取公开文章详情", description = "隐藏文章或不存在的文章统一返回 404。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "404", description = "文章不存在或不可公开访问")
    })
    public Result<BlogDetailVo> getBlog(
            @Parameter(description = "文章 URL 标识", example = "auto-tool", required = true)
            @PathVariable String slug
    ) {
        log.info("查询公开文章详情，请求参数：slug={}", slug);
        BlogDetailVo blog = blogService.getPublishedBlog(slug)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "文章不存在"));
        log.info("查询公开文章详情完成，返回参数：slug={}", slug);
        return Result.success(blog);
    }

    @GetMapping("/categories")
    @Operation(summary = "获取文章分类列表", description = "按分类排序值升序返回分类名称。")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public Result<List<String>> getCategories() {
        log.info("查询文章分类列表，请求参数：无");
        List<String> categories = blogService.getCategories();
        log.info("查询文章分类列表完成，返回参数：分类数量={}", categories.size());
        return Result.success(categories);
    }
}
