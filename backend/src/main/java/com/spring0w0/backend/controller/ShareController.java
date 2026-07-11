package com.spring0w0.backend.controller;

import com.spring0w0.backend.common.Result;
import com.spring0w0.backend.pojo.vo.ShareVo;
import com.spring0w0.backend.service.ShareService;
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
 * 公开友链接口。
 */
@RestController
@RequestMapping("/api/shares")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "友链公开接口", description = "无需登录即可读取的友链列表")
public class ShareController {

    private final ShareService shareService;

    @GetMapping
    @Operation(summary = "获取友链列表")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public Result<List<ShareVo>> getShares() {
        log.info("查询友链列表，请求参数：无");
        List<ShareVo> shares = shareService.getShares();
        log.info("查询友链列表完成，返回参数：友链数量={}", shares.size());
        return Result.success(shares);
    }
}
