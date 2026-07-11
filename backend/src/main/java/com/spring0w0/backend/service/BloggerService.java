package com.spring0w0.backend.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.spring0w0.backend.mapper.BloggerMapper;
import com.spring0w0.backend.pojo.entity.Blogger;
import com.spring0w0.backend.pojo.vo.BloggerVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 博主列表查询服务。
 */
@Service
@RequiredArgsConstructor
public class BloggerService {

    private final BloggerMapper bloggerMapper;

    public List<BloggerVo> getBloggers() {
        return bloggerMapper.selectList(Wrappers.<Blogger>lambdaQuery()
                        .orderByAsc(Blogger::getSortOrder)
                        .orderByAsc(Blogger::getId))
                .stream()
                .map(blogger -> new BloggerVo(
                        blogger.getName(),
                        blogger.getAvatarUrl(),
                        blogger.getUrl(),
                        blogger.getDescription(),
                        blogger.getStars() == null ? 0 : blogger.getStars(),
                        blogger.getStatus()
                ))
                .toList();
    }
}
