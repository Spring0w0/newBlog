package com.spring0w0.backend.service;

import com.spring0w0.backend.mapper.AboutMapper;
import com.spring0w0.backend.pojo.dto.AboutSaveRequest;
import com.spring0w0.backend.pojo.entity.About;
import com.spring0w0.backend.pojo.vo.AboutVo;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 关于页面查询服务。
 */
@Service
@RequiredArgsConstructor
public class AboutService {

    private final AboutMapper aboutMapper;

    @Cacheable(cacheNames = "aboutContent", key = "'singleton'")
    public AboutVo getAbout() {
        About about = aboutMapper.selectById(1);
        if (about == null) {
            return new AboutVo("", "", "");
        }
        return new AboutVo(about.getTitle(), about.getDescription(), about.getContent());
    }

    @Transactional
    @CacheEvict(cacheNames = "aboutContent", allEntries = true)
    public AboutVo saveAbout(AboutSaveRequest request) {
        About about = aboutMapper.selectById(1);
        if (about == null) {
            about = new About();
            about.setId(1);
            about.setTitle(request.title().trim());
            about.setDescription(request.description().trim());
            about.setContent(request.content());
            if (aboutMapper.insert(about) != 1) {
                throw new IllegalStateException("关于页面保存失败");
            }
        } else {
            about.setTitle(request.title().trim());
            about.setDescription(request.description().trim());
            about.setContent(request.content());
            if (aboutMapper.updateById(about) != 1) {
                throw new IllegalStateException("关于页面保存失败");
            }
        }
        return new AboutVo(about.getTitle(), about.getDescription(), about.getContent());
    }
}
