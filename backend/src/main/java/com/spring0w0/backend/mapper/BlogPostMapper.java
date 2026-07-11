package com.spring0w0.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.spring0w0.backend.pojo.entity.BlogPost;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BlogPostMapper extends BaseMapper<BlogPost> {
}
