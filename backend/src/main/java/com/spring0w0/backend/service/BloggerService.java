package com.spring0w0.backend.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.spring0w0.backend.common.ImageUploadScope;
import com.spring0w0.backend.common.ResultCode;
import com.spring0w0.backend.exception.BusinessException;
import com.spring0w0.backend.mapper.BloggerMapper;
import com.spring0w0.backend.pojo.dto.BloggerWriteRequest;
import com.spring0w0.backend.pojo.entity.Blogger;
import com.spring0w0.backend.pojo.vo.AdminBloggerVo;
import com.spring0w0.backend.pojo.vo.BloggerVo;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 博主公开展示与后台维护服务。 */
@Service
@RequiredArgsConstructor
public class BloggerService {

    private final BloggerMapper bloggerMapper;
    private final ManagedImageUrlService managedImageUrlService;

    @Cacheable(cacheNames = "bloggers", key = "'all'")
    public List<BloggerVo> getBloggers() {
        return listBloggers().stream().map(this::toPublicVo).toList();
    }

    public List<AdminBloggerVo> getAdminBloggers() {
        return listBloggers().stream().map(this::toAdminVo).toList();
    }

    @Transactional
    @CacheEvict(cacheNames = "bloggers", allEntries = true)
    public AdminBloggerVo createBlogger(BloggerWriteRequest request) {
        Blogger blogger = new Blogger();
        applyRequest(blogger, request);
        blogger.setSortOrder(Math.toIntExact(bloggerMapper.selectCount(Wrappers.emptyWrapper())));
        if (bloggerMapper.insert(blogger) != 1) {
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "博主保存失败");
        }
        return toAdminVo(blogger);
    }

    @Transactional
    @CacheEvict(cacheNames = "bloggers", allEntries = true)
    public AdminBloggerVo updateBlogger(Long id, BloggerWriteRequest request) {
        Blogger blogger = getRequiredBlogger(id);
        applyRequest(blogger, request);
        if (bloggerMapper.updateById(blogger) != 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "博主不存在");
        }
        return toAdminVo(blogger);
    }

    @Transactional
    @CacheEvict(cacheNames = "bloggers", allEntries = true)
    public void deleteBlogger(Long id) {
        getRequiredBlogger(id);
        if (bloggerMapper.deleteById(id) != 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "博主不存在");
        }
    }

    private List<Blogger> listBloggers() {
        return bloggerMapper.selectList(Wrappers.<Blogger>lambdaQuery()
                .orderByAsc(Blogger::getSortOrder)
                .orderByAsc(Blogger::getId));
    }

    private Blogger getRequiredBlogger(Long id) {
        Blogger blogger = bloggerMapper.selectById(id);
        if (blogger == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "博主不存在");
        }
        return blogger;
    }

    private void applyRequest(Blogger blogger, BloggerWriteRequest request) {
        ManagedImageUrlService.ManagedImageReference avatar = managedImageUrlService
                .normalizeAndValidate(request.avatar(), ImageUploadScope.BLOGGERS);
        blogger.setName(request.name().trim());
        blogger.setAvatarUrl(avatar.storedUrl());
        blogger.setAvatarFileAssetId(avatar.fileAssetId());
        blogger.setUrl(request.url().trim());
        blogger.setDescription(request.description().trim());
        blogger.setStars(request.stars() == null ? 3 : request.stars());
        blogger.setStatus(trimToNull(request.status()) == null ? "recent" : request.status().trim());
    }

    private BloggerVo toPublicVo(Blogger blogger) {
        return new BloggerVo(
                blogger.getName(),
                managedImageUrlService.toPublicUrl(blogger.getAvatarUrl(), ImageUploadScope.BLOGGERS),
                blogger.getUrl(),
                blogger.getDescription(),
                blogger.getStars() == null ? 0 : blogger.getStars(),
                blogger.getStatus()
        );
    }

    private AdminBloggerVo toAdminVo(Blogger blogger) {
        return new AdminBloggerVo(
                blogger.getId(),
                blogger.getName(),
                managedImageUrlService.toPublicUrl(blogger.getAvatarUrl(), ImageUploadScope.BLOGGERS),
                blogger.getUrl(),
                blogger.getDescription(),
                blogger.getStars() == null ? 0 : blogger.getStars(),
                blogger.getStatus()
        );
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
