package com.spring0w0.backend.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/** 首页社交按钮关系记录。 */
@Getter
@Setter
@TableName("social_buttons")
public class SocialButton {

    @TableId
    private String id;
    private String type;
    private String value;
    private String label;
    private Integer sortOrder;
}
