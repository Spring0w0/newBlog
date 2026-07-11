package com.spring0w0.backend.controller;

import com.spring0w0.backend.common.Result;
import com.spring0w0.backend.config.OpenApiConfig;
import com.spring0w0.backend.pojo.dto.PictureWriteRequest;
import com.spring0w0.backend.pojo.vo.AdminPictureVo;
import com.spring0w0.backend.pojo.vo.PictureVo;
import com.spring0w0.backend.service.PictureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 相册公开读取与管理员维护接口。 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "相册接口", description = "公开相册展示，以及管理员的相册分组维护")
public class PictureController {

    private final PictureService pictureService;

    @GetMapping("/pictures")
    @Operation(summary = "获取相册列表")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public Result<List<PictureVo>> getPictures() {
        log.info("查询相册列表，请求参数：无");
        List<PictureVo> pictures = pictureService.getPictures();
        log.info("查询相册列表完成，返回参数：相册分组数量={}", pictures.size());
        return Result.success(pictures);
    }

    @GetMapping("/admin/pictures")
    @Operation(summary = "获取管理员相册列表")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "401", description = "未登录或 Token 无效"),
            @ApiResponse(responseCode = "403", description = "非管理员")
    })
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
    public Result<List<AdminPictureVo>> getAdminPictures() {
        log.info("查询管理员相册列表，请求参数：无");
        List<AdminPictureVo> pictures = pictureService.getAdminPictures();
        log.info("查询管理员相册列表完成，返回参数：相册分组数量={}", pictures.size());
        return Result.success(pictures);
    }

    @PostMapping("/admin/pictures")
    @Operation(summary = "创建相册分组")
    @ApiResponse(responseCode = "200", description = "创建成功")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
    public Result<AdminPictureVo> createPicture(@Valid @RequestBody PictureWriteRequest request) {
        log.info("创建相册分组，请求参数：上传时间={}，图片数量={}，说明长度={}",
                request.uploadedAt(), request.images().size(), request.description() == null ? 0 : request.description().length());
        AdminPictureVo picture = pictureService.createPicture(request);
        log.info("创建相册分组完成，返回参数：分组ID={}，图片数量={}", picture.id(), picture.images().size());
        return Result.success(picture);
    }

    @PutMapping("/admin/pictures/{id}")
    @Operation(summary = "更新相册分组")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
    public Result<AdminPictureVo> updatePicture(
            @PathVariable @NotBlank(message = "相册分组 ID 不能为空") String id,
            @Valid @RequestBody PictureWriteRequest request
    ) {
        log.info("更新相册分组，请求参数：分组ID={}，上传时间={}，图片数量={}，说明长度={}",
                id, request.uploadedAt(), request.images().size(), request.description() == null ? 0 : request.description().length());
        AdminPictureVo picture = pictureService.updatePicture(id, request);
        log.info("更新相册分组完成，返回参数：分组ID={}，图片数量={}", picture.id(), picture.images().size());
        return Result.success(picture);
    }

    @DeleteMapping("/admin/pictures/{id}")
    @Operation(summary = "删除相册分组", description = "删除分组及其图片引用，不会直接删除图片文件实体")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
    public Result<Void> deletePicture(@PathVariable @NotBlank(message = "相册分组 ID 不能为空") String id) {
        log.info("删除相册分组，请求参数：分组ID={}", id);
        pictureService.deletePicture(id);
        log.info("删除相册分组完成，返回参数：分组ID={}", id);
        return Result.success();
    }
}
