package com.spring0w0.backend.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 相册分组中的图片引用。
 */
@Getter
@Setter
@TableName("picture_images")
public class PictureImage {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String pictureId;
    private Long fileAssetId;
    private String url;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
