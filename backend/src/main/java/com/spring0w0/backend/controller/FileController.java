package com.spring0w0.backend.controller;

import com.spring0w0.backend.common.Result;
import com.spring0w0.backend.config.OpenApiConfig;
import com.spring0w0.backend.config.UploadProperties;
import com.spring0w0.backend.pojo.entity.FileAsset;
import com.spring0w0.backend.pojo.vo.ImageUploadVo;
import com.spring0w0.backend.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 管理端运行期图片上传和删除接口。
 */
@RestController
@RequestMapping("/api/admin/files")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "文件管理接口", description = "管理员上传、删除运行期图片；公开读取走 /images/**")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
public class FileController {

    private final FileService fileService;
    private final UploadProperties uploadProperties;

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传图片", description = "仅支持 JPEG、PNG、GIF、WebP，单个文件不超过 10MB；服务端校验 MIME、扩展名和实际文件签名。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "上传成功"),
            @ApiResponse(responseCode = "400", description = "文件字段、scope 或请求参数不合法"),
            @ApiResponse(responseCode = "401", description = "未登录或 Token 无效"),
            @ApiResponse(responseCode = "403", description = "非管理员"),
            @ApiResponse(responseCode = "413", description = "文件超过 10MB"),
            @ApiResponse(responseCode = "415", description = "不支持的图片类型或图片内容校验失败")
    })
    public Result<ImageUploadVo> uploadImage(
            @Parameter(description = "单个图片文件", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "图片业务范围", required = true,
                    schema = @Schema(allowableValues = {"blog-images", "site", "bloggers", "projects", "shares", "pictures"}))
            @RequestParam("scope") String scope
    ) {
        log.info("管理员上传图片，传入参数：scope={}，contentType={}，size={}，原始文件名已省略", scope, file.getContentType(), file.getSize());
        FileAsset asset = fileService.uploadImage(file, scope);
        ImageUploadVo response = new ImageUploadVo(
                asset.getId(),
                buildPublicUrl(asset),
                asset.getStoredFilename(),
                asset.getOriginalName(),
                asset.getFileSize(),
                asset.getContentType()
        );
        log.info("管理员上传图片成功，返回参数：fileId={}，scope={}，size={}，contentType={}",
                response.fileId(), asset.getScope(), response.size(), response.contentType());
        return Result.success(response);
    }

    @DeleteMapping("/{fileId}")
    @Operation(summary = "删除未被引用的图片", description = "仅当图片未被文章、站点配置、博主、项目、友链、相册等业务数据引用时才能删除。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "401", description = "未登录或 Token 无效"),
            @ApiResponse(responseCode = "403", description = "非管理员"),
            @ApiResponse(responseCode = "404", description = "文件不存在"),
            @ApiResponse(responseCode = "422", description = "图片仍被业务数据引用")
    })
    public Result<Void> deleteImage(
            @Parameter(description = "上传图片的文件 ID", example = "42", required = true)
            @PathVariable @Positive(message = "文件 ID 必须为正整数") Long fileId
    ) {
        log.info("管理员删除图片，传入参数：fileId={}", fileId);
        fileService.deleteImage(fileId);
        log.info("管理员删除图片成功，返回参数：fileId={}", fileId);
        return Result.success();
    }

    private String buildPublicUrl(FileAsset asset) {
        return uploadProperties.buildPublicUrl(uploadProperties.normalizedPublicPath() + "/" + asset.getRelativePath());
    }
}
