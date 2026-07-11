package com.spring0w0.backend.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 运行期上传文件的元数据。relativePath 仅是相对 uploads 的路径，绝不保存或暴露物理绝对路径。
 */
@Getter
@Setter
@TableName("file_assets")
public class FileAsset {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String scope;
    private String storedFilename;
    private String relativePath;
    private String originalName;
    private String contentType;
    private Long fileSize;
    private String sha256;
    private Integer width;
    private Integer height;
    private LocalDateTime createdAt;
}
