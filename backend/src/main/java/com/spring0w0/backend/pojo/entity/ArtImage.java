package com.spring0w0.backend.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/** 首页 Art 图片关系记录。 */
@Getter
@Setter
@TableName("art_images")
public class ArtImage {

    @TableId
    private String id;
    private String url;
    private String description;
    private Integer sortOrder;
}
