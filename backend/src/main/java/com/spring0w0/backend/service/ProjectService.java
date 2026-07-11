package com.spring0w0.backend.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.spring0w0.backend.common.ImageUploadScope;
import com.spring0w0.backend.common.JsonContentReader;
import com.spring0w0.backend.common.ResultCode;
import com.spring0w0.backend.exception.BusinessException;
import com.spring0w0.backend.mapper.ProjectMapper;
import com.spring0w0.backend.pojo.dto.ProjectWriteRequest;
import com.spring0w0.backend.pojo.entity.Project;
import com.spring0w0.backend.pojo.vo.AdminProjectVo;
import com.spring0w0.backend.pojo.vo.ProjectVo;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;

/** 项目公开展示与后台维护服务。 */
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectMapper projectMapper;
    private final JsonContentReader jsonContentReader;
    private final ManagedImageUrlService managedImageUrlService;

    @Cacheable(cacheNames = "projects", key = "'all'")
    public List<ProjectVo> getProjects() {
        return listProjects().stream().map(this::toPublicVo).toList();
    }

    public List<AdminProjectVo> getAdminProjects() {
        return listProjects().stream().map(this::toAdminVo).toList();
    }

    @Transactional
    @CacheEvict(cacheNames = "projects", allEntries = true)
    public AdminProjectVo createProject(ProjectWriteRequest request) {
        Project project = new Project();
        applyRequest(project, request);
        project.setSortOrder(Math.toIntExact(projectMapper.selectCount(Wrappers.emptyWrapper())));
        if (projectMapper.insert(project) != 1) {
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "项目保存失败");
        }
        return toAdminVo(project);
    }

    @Transactional
    @CacheEvict(cacheNames = "projects", allEntries = true)
    public AdminProjectVo updateProject(Long id, ProjectWriteRequest request) {
        Project project = getRequiredProject(id);
        applyRequest(project, request);
        if (projectMapper.updateById(project) != 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "项目不存在");
        }
        return toAdminVo(project);
    }

    @Transactional
    @CacheEvict(cacheNames = "projects", allEntries = true)
    public void deleteProject(Long id) {
        getRequiredProject(id);
        if (projectMapper.deleteById(id) != 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "项目不存在");
        }
    }

    private List<Project> listProjects() {
        return projectMapper.selectList(Wrappers.<Project>lambdaQuery()
                .orderByAsc(Project::getSortOrder)
                .orderByAsc(Project::getId));
    }

    private Project getRequiredProject(Long id) {
        Project project = projectMapper.selectById(id);
        if (project == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "项目不存在");
        }
        return project;
    }

    private void applyRequest(Project project, ProjectWriteRequest request) {
        ManagedImageUrlService.ManagedImageReference image = managedImageUrlService
                .normalizeAndValidate(request.image(), ImageUploadScope.PROJECTS);
        project.setName(request.name().trim());
        project.setProjectYear(request.year());
        project.setDescription(request.description().trim());
        project.setImageUrl(image.storedUrl());
        project.setImageFileAssetId(image.fileAssetId());
        project.setUrl(request.url().trim());
        project.setTags(jsonContentReader.writeStringList(normalizeTags(request.tags())));
        project.setGithubUrl(trimToNull(request.github()));
        project.setNpmUrl(trimToNull(request.npm()));
    }

    private ProjectVo toPublicVo(Project project) {
        return new ProjectVo(
                project.getName(),
                project.getProjectYear(),
                project.getDescription(),
                managedImageUrlService.toPublicUrl(project.getImageUrl(), ImageUploadScope.PROJECTS),
                project.getUrl(),
                jsonContentReader.readStringList(project.getTags()),
                project.getGithubUrl(),
                project.getNpmUrl()
        );
    }

    private AdminProjectVo toAdminVo(Project project) {
        return new AdminProjectVo(
                project.getId(),
                project.getName(),
                project.getProjectYear(),
                project.getDescription(),
                managedImageUrlService.toPublicUrl(project.getImageUrl(), ImageUploadScope.PROJECTS),
                project.getUrl(),
                jsonContentReader.readStringList(project.getTags()),
                project.getGithubUrl(),
                project.getNpmUrl()
        );
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null) {
            return List.of();
        }
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (String tag : tags) {
            String normalized = trimToNull(tag);
            if (normalized != null) {
                values.add(normalized);
            }
        }
        return List.copyOf(values);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
