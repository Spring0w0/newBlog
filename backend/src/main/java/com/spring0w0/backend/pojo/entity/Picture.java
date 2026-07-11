package com.spring0w0.backend.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("pictures")
public class Picture {

    @TableId
    private String id;
    private String images;
    private String description;
    private Integer sortOrder;
    private LocalDateTime uploadedAt;
    private LocalDateTime createdAt;
}
