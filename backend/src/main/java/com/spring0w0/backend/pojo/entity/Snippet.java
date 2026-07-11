package com.spring0w0.backend.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("snippets")
public class Snippet {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String content;
    private Integer sortOrder;
}
