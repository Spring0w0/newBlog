package com.spring0w0.backend.controller;

import com.spring0w0.backend.common.Result;
import com.spring0w0.backend.config.OpenApiConfig;
import com.spring0w0.backend.pojo.dto.ProjectWriteRequest;
import com.spring0w0.backend.pojo.vo.AdminProjectVo;
import com.spring0w0.backend.pojo.vo.ProjectVo;
import com.spring0w0.backend.service.ProjectService;
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

/** 项目公开读取与管理员维护接口。 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "项目接口", description = "公开项目展示，以及管理员的项目维护")
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping("/projects")
    @Operation(summary = "获取项目列表")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public Result<List<ProjectVo>> getProjects() {
        log.info("查询项目列表，请求参数：无");
        List<ProjectVo> projects = projectService.getProjects();
        log.info("查询项目列表完成，返回参数：项目数量={}", projects.size());
        return Result.success(projects);
    }

    @GetMapping("/admin/projects")
    @Operation(summary = "获取管理员项目列表")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "401", description = "未登录或 Token 无效"),
            @ApiResponse(responseCode = "403", description = "非管理员")
    })
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
    public Result<List<AdminProjectVo>> getAdminProjects() {
        log.info("查询管理员项目列表，请求参数：无");
        List<AdminProjectVo> projects = projectService.getAdminProjects();
        log.info("查询管理员项目列表完成，返回参数：项目数量={}", projects.size());
        return Result.success(projects);
    }

    @PostMapping("/admin/projects")
    @Operation(summary = "创建项目")
    @ApiResponse(responseCode = "200", description = "创建成功")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
    public Result<AdminProjectVo> createProject(@Valid @RequestBody ProjectWriteRequest request) {
        log.info("创建项目，请求参数：名称={}，年份={}，图片URL长度={}，访问URL={}，标签数量={}，简介长度={}",
                request.name(), request.year(), request.image().length(), request.url(),
                request.tags() == null ? 0 : request.tags().size(), request.description().length());
        AdminProjectVo project = projectService.createProject(request);
        log.info("创建项目完成，返回参数：项目ID={}，名称={}", project.id(), project.name());
        return Result.success(project);
    }

    @PutMapping("/admin/projects/{id}")
    @Operation(summary = "更新项目")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
    public Result<AdminProjectVo> updateProject(
            @PathVariable @Positive(message = "项目 ID 必须为正整数") Long id,
            @Valid @RequestBody ProjectWriteRequest request
    ) {
        log.info("更新项目，请求参数：项目ID={}，名称={}，年份={}，图片URL长度={}，访问URL={}，标签数量={}，简介长度={}",
                id, request.name(), request.year(), request.image().length(), request.url(),
                request.tags() == null ? 0 : request.tags().size(), request.description().length());
        AdminProjectVo project = projectService.updateProject(id, request);
        log.info("更新项目完成，返回参数：项目ID={}，名称={}", project.id(), project.name());
        return Result.success(project);
    }

    @DeleteMapping("/admin/projects/{id}")
    @Operation(summary = "删除项目", description = "删除项目记录，不会直接删除其图片文件实体")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
    public Result<Void> deleteProject(@PathVariable @Positive(message = "项目 ID 必须为正整数") Long id) {
        log.info("删除项目，请求参数：项目ID={}", id);
        projectService.deleteProject(id);
        log.info("删除项目完成，返回参数：项目ID={}", id);
        return Result.success();
    }
}
