package com.spring0w0.backend.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.spring0w0.backend.mapper.SnippetMapper;
import com.spring0w0.backend.pojo.dto.SnippetSaveRequest;
import com.spring0w0.backend.pojo.entity.Snippet;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 代码片段查询服务。
 */
@Service
@RequiredArgsConstructor
public class SnippetService {

    private final SnippetMapper snippetMapper;

    @Cacheable(cacheNames = "snippets", key = "'all'")
    public List<String> getSnippets() {
        return snippetMapper.selectList(Wrappers.<Snippet>lambdaQuery()
                        .orderByAsc(Snippet::getSortOrder)
                        .orderByAsc(Snippet::getId))
                .stream()
                .map(Snippet::getContent)
                .toList();
    }

    @Transactional
    @CacheEvict(cacheNames = "snippets", allEntries = true)
    public List<String> saveSnippets(SnippetSaveRequest request) {
        List<String> snippets = request.items().stream()
                .map(String::trim)
                .distinct()
                .toList();
        if (snippets.isEmpty()) {
            throw new IllegalArgumentException("至少需要保留一条代码片段");
        }
        snippetMapper.delete(Wrappers.emptyWrapper());
        for (int index = 0; index < snippets.size(); index++) {
            Snippet snippet = new Snippet();
            snippet.setContent(snippets.get(index));
            snippet.setSortOrder(index);
            if (snippetMapper.insert(snippet) != 1) {
                throw new IllegalStateException("代码片段保存失败");
            }
        }
        return snippets;
    }
}
