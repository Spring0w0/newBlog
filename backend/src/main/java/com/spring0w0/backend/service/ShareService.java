package com.spring0w0.backend.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.spring0w0.backend.common.ImageUploadScope;
import com.spring0w0.backend.common.JsonContentReader;
import com.spring0w0.backend.common.ResultCode;
import com.spring0w0.backend.exception.BusinessException;
import com.spring0w0.backend.mapper.ShareMapper;
import com.spring0w0.backend.pojo.dto.ShareWriteRequest;
import com.spring0w0.backend.pojo.entity.Share;
import com.spring0w0.backend.pojo.vo.AdminShareVo;
import com.spring0w0.backend.pojo.vo.ShareVo;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;

/** 友链公开展示与后台维护服务。 */
@Service
@RequiredArgsConstructor
public class ShareService {

    private final ShareMapper shareMapper;
    private final JsonContentReader jsonContentReader;
    private final ManagedImageUrlService managedImageUrlService;

    @Cacheable(cacheNames = "shares", key = "'all'")
    public List<ShareVo> getShares() {
        return listShares().stream().map(this::toPublicVo).toList();
    }

    public List<AdminShareVo> getAdminShares() {
        return listShares().stream().map(this::toAdminVo).toList();
    }

    @Transactional
    @CacheEvict(cacheNames = "shares", allEntries = true)
    public AdminShareVo createShare(ShareWriteRequest request) {
        Share share = new Share();
        applyRequest(share, request);
        share.setSortOrder(Math.toIntExact(shareMapper.selectCount(Wrappers.emptyWrapper())));
        if (shareMapper.insert(share) != 1) {
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "友链保存失败");
        }
        return toAdminVo(share);
    }

    @Transactional
    @CacheEvict(cacheNames = "shares", allEntries = true)
    public AdminShareVo updateShare(Long id, ShareWriteRequest request) {
        Share share = getRequiredShare(id);
        applyRequest(share, request);
        if (shareMapper.updateById(share) != 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "友链不存在");
        }
        return toAdminVo(share);
    }

    @Transactional
    @CacheEvict(cacheNames = "shares", allEntries = true)
    public void deleteShare(Long id) {
        getRequiredShare(id);
        if (shareMapper.deleteById(id) != 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "友链不存在");
        }
    }

    private List<Share> listShares() {
        return shareMapper.selectList(Wrappers.<Share>lambdaQuery()
                .orderByAsc(Share::getSortOrder)
                .orderByAsc(Share::getId));
    }

    private Share getRequiredShare(Long id) {
        Share share = shareMapper.selectById(id);
        if (share == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "友链不存在");
        }
        return share;
    }

    private void applyRequest(Share share, ShareWriteRequest request) {
        ManagedImageUrlService.ManagedImageReference logo = managedImageUrlService
                .normalizeAndValidate(request.logo(), ImageUploadScope.SHARES);
        share.setName(request.name().trim());
        share.setLogoUrl(logo.storedUrl());
        share.setLogoFileAssetId(logo.fileAssetId());
        share.setUrl(request.url().trim());
        share.setDescription(request.description().trim());
        share.setTags(jsonContentReader.writeStringList(normalizeTags(request.tags())));
        share.setStars(request.stars() == null ? 3 : request.stars());
    }

    private ShareVo toPublicVo(Share share) {
        return new ShareVo(
                share.getName(),
                managedImageUrlService.toPublicUrl(share.getLogoUrl(), ImageUploadScope.SHARES),
                share.getUrl(),
                share.getDescription(),
                jsonContentReader.readStringList(share.getTags()),
                share.getStars() == null ? 0 : share.getStars()
        );
    }

    private AdminShareVo toAdminVo(Share share) {
        return new AdminShareVo(
                share.getId(),
                share.getName(),
                managedImageUrlService.toPublicUrl(share.getLogoUrl(), ImageUploadScope.SHARES),
                share.getUrl(),
                share.getDescription(),
                jsonContentReader.readStringList(share.getTags()),
                share.getStars() == null ? 0 : share.getStars()
        );
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null) {
            return List.of();
        }
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (String tag : tags) {
            String normalized = trimToNull(tag);
            if (normalized != null) {
                values.add(normalized);
            }
        }
        return List.copyOf(values);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
