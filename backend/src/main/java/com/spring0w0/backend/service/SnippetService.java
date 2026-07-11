package com.spring0w0.backend.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.spring0w0.backend.mapper.SnippetMapper;
import com.spring0w0.backend.pojo.entity.Snippet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 代码片段查询服务。
 */
@Service
@RequiredArgsConstructor
public class SnippetService {

    private final SnippetMapper snippetMapper;

    public List<String> getSnippets() {
        return snippetMapper.selectList(Wrappers.<Snippet>lambdaQuery()
                        .orderByAsc(Snippet::getSortOrder)
                        .orderByAsc(Snippet::getId))
                .stream()
                .map(Snippet::getContent)
                .toList();
    }
}
