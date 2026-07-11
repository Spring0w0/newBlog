package com.spring0w0.backend.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.spring0w0.backend.common.JsonContentReader;
import com.spring0w0.backend.mapper.ProjectMapper;
import com.spring0w0.backend.pojo.entity.Project;
import com.spring0w0.backend.pojo.vo.ProjectVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 项目列表查询服务。
 */
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectMapper projectMapper;
    private final JsonContentReader jsonContentReader;

    public List<ProjectVo> getProjects() {
        return projectMapper.selectList(Wrappers.<Project>lambdaQuery()
                        .orderByAsc(Project::getSortOrder)
                        .orderByAsc(Project::getId))
                .stream()
                .map(project -> new ProjectVo(
                        project.getName(),
                        project.getProjectYear(),
                        project.getDescription(),
                        project.getImageUrl(),
                        project.getUrl(),
                        jsonContentReader.readStringList(project.getTags()),
                        project.getGithubUrl(),
                        project.getNpmUrl()
                ))
                .toList();
    }
}
