package com.spring0w0.backend.common;

import com.spring0w0.backend.exception.BusinessException;

import java.util.Arrays;

/**
 * 图片在 uploads 目录中的受限业务范围。
 */
public enum ImageUploadScope {
    BLOG_IMAGES("blog-images"),
    SITE("site"),
    BLOGGERS("bloggers"),
    PROJECTS("projects"),
    SHARES("shares"),
    PICTURES("pictures");

    private final String value;

    ImageUploadScope(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static ImageUploadScope fromValue(String value) {
        return Arrays.stream(values())
                .filter(scope -> scope.value.equals(value))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ResultCode.BAD_REQUEST, "不支持的图片存储范围"));
    }
}
