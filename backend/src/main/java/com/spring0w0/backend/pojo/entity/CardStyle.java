package com.spring0w0.backend.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("card_styles")
public class CardStyle {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String cardKey;
    private Integer width;
    private Integer height;
    private Integer offsetX;
    private Integer offsetY;
    private Integer sortOrder;
    private Boolean enabled;
    private String config;
}
