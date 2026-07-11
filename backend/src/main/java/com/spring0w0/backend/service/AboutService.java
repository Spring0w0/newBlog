package com.spring0w0.backend.service;

import com.spring0w0.backend.mapper.AboutMapper;
import com.spring0w0.backend.pojo.entity.About;
import com.spring0w0.backend.pojo.vo.AboutVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 关于页面查询服务。
 */
@Service
@RequiredArgsConstructor
public class AboutService {

    private final AboutMapper aboutMapper;

    public AboutVo getAbout() {
        About about = aboutMapper.selectById(1);
        if (about == null) {
            return new AboutVo("", "", "");
        }
        return new AboutVo(about.getTitle(), about.getDescription(), about.getContent());
    }
}
