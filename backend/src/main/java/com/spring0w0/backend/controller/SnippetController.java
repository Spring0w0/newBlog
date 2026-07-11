package com.spring0w0.backend.controller;

import com.spring0w0.backend.common.Result;
import com.spring0w0.backend.config.OpenApiConfig;
import com.spring0w0.backend.pojo.dto.SnippetSaveRequest;
import com.spring0w0.backend.service.SnippetService;
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

import java.util.List;

/**
 * 公开代码片段接口。
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "代码片段公开接口", description = "无需登录即可读取的代码片段列表")
public class SnippetController {

    private final SnippetService snippetService;

    @GetMapping("/snippets")
    @Operation(summary = "获取代码片段列表")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public Result<List<String>> getSnippets() {
        log.info("查询代码片段列表，请求参数：无");
        List<String> snippets = snippetService.getSnippets();
        log.info("查询代码片段列表完成，返回参数：片段数量={}", snippets.size());
        return Result.success(snippets);
    }

    @PutMapping("/admin/snippets")
    @Operation(summary = "保存代码片段列表", description = "整体替换片段列表；请求顺序即公开展示顺序。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "保存成功"),
            @ApiResponse(responseCode = "400", description = "请求参数不合法"),
            @ApiResponse(responseCode = "401", description = "未登录或 Token 无效"),
            @ApiResponse(responseCode = "403", description = "非管理员")
    })
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
    public Result<List<String>> saveSnippets(@Valid @RequestBody SnippetSaveRequest request) {
        log.info("保存代码片段列表，请求参数：片段数量={}", request.items().size());
        List<String> snippets = snippetService.saveSnippets(request);
        log.info("保存代码片段列表完成，返回参数：片段数量={}", snippets.size());
        return Result.success(snippets);
    }
}
