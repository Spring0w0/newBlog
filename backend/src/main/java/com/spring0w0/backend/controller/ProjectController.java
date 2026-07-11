package com.spring0w0.backend.controller;

import com.spring0w0.backend.common.Result;
import com.spring0w0.backend.pojo.vo.ProjectVo;
import com.spring0w0.backend.service.ProjectService;
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
 * 公开项目接口。
 */
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "项目公开接口", description = "无需登录即可读取的项目列表")
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    @Operation(summary = "获取项目列表")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public Result<List<ProjectVo>> getProjects() {
        log.info("查询项目列表，请求参数：无");
        List<ProjectVo> projects = projectService.getProjects();
        log.info("查询项目列表完成，返回参数：项目数量={}", projects.size());
        return Result.success(projects);
    }
}
