package com.spring0w0.backend.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/** 首页背景图片关系记录。 */
@Getter
@Setter
@TableName("background_images")
public class BackgroundImage {

    @TableId
    private String id;
    private String url;
    private Integer sortOrder;
}
