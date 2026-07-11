package com.spring0w0.backend.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("bloggers")
public class Blogger {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String avatarUrl;
    private String url;
    private String description;
    private Integer stars;
    private String status;
    private Integer sortOrder;
}
