package com.spring0w0.backend.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 文章正文图片与上传文件的引用关系。
 */
@Getter
@Setter
@TableName("blog_images")
public class BlogImage {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long blogId;
    private Long fileAssetId;
    private String url;
    private String storagePath;
    private String originalFilename;
    private String sha256;
    private Long fileSize;
    private Integer width;
    private Integer height;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
