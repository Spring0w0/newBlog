package com.spring0w0.backend.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "管理端友链")
public record AdminShareVo(Long id, String name, String logo, String url, String description, List<String> tags, int stars) {
}
