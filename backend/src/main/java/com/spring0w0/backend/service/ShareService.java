package com.spring0w0.backend.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.spring0w0.backend.common.JsonContentReader;
import com.spring0w0.backend.mapper.ShareMapper;
import com.spring0w0.backend.pojo.entity.Share;
import com.spring0w0.backend.pojo.vo.ShareVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 友链列表查询服务。
 */
@Service
@RequiredArgsConstructor
public class ShareService {

    private final ShareMapper shareMapper;
    private final JsonContentReader jsonContentReader;

    public List<ShareVo> getShares() {
        return shareMapper.selectList(Wrappers.<Share>lambdaQuery()
                        .orderByAsc(Share::getSortOrder)
                        .orderByAsc(Share::getId))
                .stream()
                .map(share -> new ShareVo(
                        share.getName(),
                        share.getLogoUrl(),
                        share.getUrl(),
                        share.getDescription(),
                        jsonContentReader.readStringList(share.getTags()),
                        share.getStars() == null ? 0 : share.getStars()
                ))
                .toList();
    }
}
