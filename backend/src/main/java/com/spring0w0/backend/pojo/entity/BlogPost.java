package com.spring0w0.backend.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("blog_posts")
public class BlogPost {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String slug;
    private String title;
    private String markdown;
    private String summary;
    private String tags;
    private String category;
    private String coverUrl;
    private Boolean hidden;
    private Long viewCount;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
