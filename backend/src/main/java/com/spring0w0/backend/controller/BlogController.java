package com.spring0w0.backend.controller;

import com.spring0w0.backend.common.Result;
import com.spring0w0.backend.common.ResultCode;
import com.spring0w0.backend.config.OpenApiConfig;
import com.spring0w0.backend.exception.BusinessException;
import com.spring0w0.backend.pojo.dto.BatchDeleteRequest;
import com.spring0w0.backend.pojo.dto.BlogWriteRequest;
import com.spring0w0.backend.pojo.dto.CategorySaveRequest;
import com.spring0w0.backend.pojo.vo.AdminBlogSummaryVo;
import com.spring0w0.backend.pojo.vo.AdminBlogVo;
import com.spring0w0.backend.pojo.vo.BlogDetailVo;
import com.spring0w0.backend.pojo.vo.BlogSummaryVo;
import com.spring0w0.backend.pojo.vo.PageVo;
import com.spring0w0.backend.service.BlogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 文章公开读取和管理员内容管理接口。
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "文章接口", description = "公开文章读取，以及管理员文章与分类管理")
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

    @GetMapping("/admin/blogs")
    @Operation(summary = "分页获取管理端文章列表", description = "返回文章数据库 ID 和编辑所需摘要；可用 slug 精确定位单篇文章。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "401", description = "未登录或 Token 无效"),
            @ApiResponse(responseCode = "403", description = "非管理员")
    })
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
    public Result<PageVo<AdminBlogSummaryVo>> getAdminBlogs(
            @Parameter(description = "页码，从 1 开始", example = "1")
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于等于 1") long page,
            @Parameter(description = "每页数量，最大 100", example = "20")
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "每页数量必须大于等于 1") @Max(value = 100, message = "每页数量不能超过 100") long pageSize,
            @Parameter(description = "可选，按 slug 精确筛选", example = "my-first-post")
            @RequestParam(required = false) @Size(max = 120, message = "文章 slug 不能超过 120 个字符") String slug
    ) {
        log.info("查询管理端文章列表，请求参数：page={}，pageSize={}，slug={}", page, pageSize, slug);
        PageVo<AdminBlogSummaryVo> result = blogService.getAdminBlogs(page, pageSize, slug);
        log.info("查询管理端文章列表完成，返回参数：文章数量={}，总数={}", result.items().size(), result.total());
        return Result.success(result);
    }

    @GetMapping("/admin/blogs/{id}")
    @Operation(summary = "获取管理端文章详情", description = "返回文章正文、受管正文图片以及数据库 ID，供编辑器加载。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "401", description = "未登录或 Token 无效"),
            @ApiResponse(responseCode = "403", description = "非管理员"),
            @ApiResponse(responseCode = "404", description = "文章不存在")
    })
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
    public Result<AdminBlogVo> getAdminBlog(
            @Parameter(description = "文章数据库 ID", example = "12", required = true)
            @PathVariable @Positive(message = "文章 ID 必须为正整数") Long id
    ) {
        log.info("查询管理端文章详情，请求参数：文章ID={}", id);
        AdminBlogVo result = blogService.getAdminBlog(id);
        log.info("查询管理端文章详情完成，返回参数：文章ID={}，slug={}", result.id(), result.slug());
        return Result.success(result);
    }

    @PostMapping("/admin/blogs")
    @Operation(summary = "创建文章", description = "slug 必须唯一；分类非空时必须已经存在。Markdown 中的受管文章图片会自动建立文件引用。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "创建成功"),
            @ApiResponse(responseCode = "400", description = "请求参数不合法"),
            @ApiResponse(responseCode = "401", description = "未登录或 Token 无效"),
            @ApiResponse(responseCode = "403", description = "非管理员"),
            @ApiResponse(responseCode = "409", description = "slug 已存在"),
            @ApiResponse(responseCode = "422", description = "分类或受管图片引用不合法")
    })
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
    public Result<AdminBlogVo> createBlog(@Valid @RequestBody BlogWriteRequest request) {
        log.info("创建文章，请求参数：slug={}，title={}，分类={}，标签数量={}，隐藏={}，正文长度={}",
                request.slug(), request.title(), request.category(), request.tags() == null ? 0 : request.tags().size(),
                request.hidden(), request.markdown().length());
        AdminBlogVo result = blogService.createBlog(request);
        log.info("创建文章完成，返回参数：文章ID={}，slug={}", result.id(), result.slug());
        return Result.success(result);
    }

    @PutMapping("/admin/blogs/{id}")
    @Operation(summary = "更新文章", description = "使用数据库 ID 更新文章；可更新 slug，但 slug 仍必须全局唯一。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "400", description = "请求参数不合法"),
            @ApiResponse(responseCode = "401", description = "未登录或 Token 无效"),
            @ApiResponse(responseCode = "403", description = "非管理员"),
            @ApiResponse(responseCode = "404", description = "文章不存在"),
            @ApiResponse(responseCode = "409", description = "slug 已存在"),
            @ApiResponse(responseCode = "422", description = "分类或受管图片引用不合法")
    })
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
    public Result<AdminBlogVo> updateBlog(
            @PathVariable @Positive(message = "文章 ID 必须为正整数") Long id,
            @Valid @RequestBody BlogWriteRequest request
    ) {
        log.info("更新文章，请求参数：文章ID={}，slug={}，title={}，分类={}，标签数量={}，隐藏={}，正文长度={}",
                id, request.slug(), request.title(), request.category(), request.tags() == null ? 0 : request.tags().size(),
                request.hidden(), request.markdown().length());
        AdminBlogVo result = blogService.updateBlog(id, request);
        log.info("更新文章完成，返回参数：文章ID={}，slug={}", result.id(), result.slug());
        return Result.success(result);
    }

    @DeleteMapping("/admin/blogs/{id}")
    @Operation(summary = "删除单篇文章", description = "删除文章及其正文图片引用，不会直接删除上传文件实体。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "401", description = "未登录或 Token 无效"),
            @ApiResponse(responseCode = "403", description = "非管理员"),
            @ApiResponse(responseCode = "404", description = "文章不存在")
    })
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
    public Result<Void> deleteBlog(
            @PathVariable @Positive(message = "文章 ID 必须为正整数") Long id
    ) {
        log.info("删除文章，请求参数：文章ID={}", id);
        blogService.deleteBlog(id);
        log.info("删除文章完成，返回参数：文章ID={}", id);
        return Result.success();
    }

    @DeleteMapping("/admin/blogs/batch")
    @Operation(summary = "批量删除文章", description = "单次最多删除 100 篇；任一文章不存在时整个操作回滚。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "400", description = "请求参数不合法"),
            @ApiResponse(responseCode = "401", description = "未登录或 Token 无效"),
            @ApiResponse(responseCode = "403", description = "非管理员"),
            @ApiResponse(responseCode = "404", description = "待删除文章中存在不存在的记录")
    })
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
    public Result<Void> deleteBlogs(@Valid @RequestBody BatchDeleteRequest request) {
        log.info("批量删除文章，请求参数：文章ID数量={}，文章ID={}", request.ids().size(), request.ids());
        blogService.deleteBlogs(request.ids());
        log.info("批量删除文章完成，返回参数：文章ID数量={}", request.ids().size());
        return Result.success();
    }

    @PutMapping("/admin/categories")
    @Operation(summary = "保存文章分类与排序", description = "请求中的分类列表即最终列表；仍被文章引用的分类不能删除。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "保存成功"),
            @ApiResponse(responseCode = "400", description = "请求参数不合法或分类重复"),
            @ApiResponse(responseCode = "401", description = "未登录或 Token 无效"),
            @ApiResponse(responseCode = "403", description = "非管理员"),
            @ApiResponse(responseCode = "422", description = "待删除分类仍被文章引用")
    })
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
    public Result<List<String>> saveCategories(@Valid @RequestBody CategorySaveRequest request) {
        log.info("保存文章分类与排序，请求参数：分类数量={}，分类={}", request.categories().size(), request.categories());
        List<String> categories = blogService.saveCategories(request);
        log.info("保存文章分类与排序完成，返回参数：分类数量={}", categories.size());
        return Result.success(categories);
    }
}
