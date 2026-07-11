package com.spring0w0.backend.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("projects")
public class Project {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Integer projectYear;
    private String description;
    private String imageUrl;
    private String url;
    private String tags;
    private String githubUrl;
    private String npmUrl;
    private Integer sortOrder;
}
