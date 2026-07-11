package com.spring0w0.backend.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "管理端项目")
public record AdminProjectVo(Long id, String name, Integer year, String description, String image, String url, List<String> tags, String github, String npm) {
}
