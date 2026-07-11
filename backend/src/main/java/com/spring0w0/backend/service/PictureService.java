package com.spring0w0.backend.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.spring0w0.backend.common.JsonContentReader;
import com.spring0w0.backend.mapper.PictureMapper;
import com.spring0w0.backend.pojo.entity.Picture;
import com.spring0w0.backend.pojo.vo.PictureVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 相册查询服务。
 */
@Service
@RequiredArgsConstructor
public class PictureService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final PictureMapper pictureMapper;
    private final JsonContentReader jsonContentReader;

    public List<PictureVo> getPictures() {
        return pictureMapper.selectList(Wrappers.<Picture>lambdaQuery()
                        .orderByAsc(Picture::getSortOrder)
                        .orderByAsc(Picture::getUploadedAt)
                        .orderByAsc(Picture::getCreatedAt))
                .stream()
                .map(picture -> new PictureVo(
                        picture.getId(),
                        toDateString(picture.getUploadedAt() == null ? picture.getCreatedAt() : picture.getUploadedAt()),
                        picture.getDescription(),
                        jsonContentReader.readStringList(picture.getImages())
                ))
                .toList();
    }

    private String toDateString(LocalDateTime dateTime) {
        return dateTime == null ? null : DATE_TIME_FORMATTER.format(dateTime);
    }
}
