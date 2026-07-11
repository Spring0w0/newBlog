package com.spring0w0.backend.controller;

import com.spring0w0.backend.common.Result;
import com.spring0w0.backend.pojo.vo.PictureVo;
import com.spring0w0.backend.service.PictureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 公开相册接口。
 */
@RestController
@RequestMapping("/api/pictures")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "相册公开接口", description = "无需登录即可读取的相册分组")
public class PictureController {

    private final PictureService pictureService;

    @GetMapping
    @Operation(summary = "获取相册列表")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public Result<List<PictureVo>> getPictures() {
        log.info("查询相册列表，请求参数：无");
        List<PictureVo> pictures = pictureService.getPictures();
        log.info("查询相册列表完成，返回参数：相册分组数量={}", pictures.size());
        return Result.success(pictures);
    }
}
