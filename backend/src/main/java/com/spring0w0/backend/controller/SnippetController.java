package com.spring0w0.backend.controller;

import com.spring0w0.backend.common.Result;
import com.spring0w0.backend.service.SnippetService;
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
 * 公开代码片段接口。
 */
@RestController
@RequestMapping("/api/snippets")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "代码片段公开接口", description = "无需登录即可读取的代码片段列表")
public class SnippetController {

    private final SnippetService snippetService;

    @GetMapping
    @Operation(summary = "获取代码片段列表")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public Result<List<String>> getSnippets() {
        log.info("查询代码片段列表，请求参数：无");
        List<String> snippets = snippetService.getSnippets();
        log.info("查询代码片段列表完成，返回参数：片段数量={}", snippets.size());
        return Result.success(snippets);
    }
}
