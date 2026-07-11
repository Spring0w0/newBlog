package com.spring0w0.backend.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("shares")
public class Share {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String url;
    private String logoUrl;
    private String description;
    private String tags;
    private Integer stars;
    private Integer sortOrder;
}
