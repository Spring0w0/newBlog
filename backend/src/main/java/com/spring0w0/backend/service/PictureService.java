package com.spring0w0.backend.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.spring0w0.backend.common.ImageUploadScope;
import com.spring0w0.backend.common.JsonContentReader;
import com.spring0w0.backend.common.ResultCode;
import com.spring0w0.backend.exception.BusinessException;
import com.spring0w0.backend.mapper.PictureImageMapper;
import com.spring0w0.backend.mapper.PictureMapper;
import com.spring0w0.backend.pojo.dto.PictureWriteRequest;
import com.spring0w0.backend.pojo.entity.Picture;
import com.spring0w0.backend.pojo.entity.PictureImage;
import com.spring0w0.backend.pojo.vo.AdminPictureVo;
import com.spring0w0.backend.pojo.vo.PictureVo;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

/** 相册公开展示与后台维护服务。 */
@Service
@RequiredArgsConstructor
public class PictureService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final PictureMapper pictureMapper;
    private final PictureImageMapper pictureImageMapper;
    private final JsonContentReader jsonContentReader;
    private final ManagedImageUrlService managedImageUrlService;

    @Cacheable(cacheNames = "pictures", key = "'all'")
    public List<PictureVo> getPictures() {
        return listPictures().stream().map(this::toPublicVo).toList();
    }

    public List<AdminPictureVo> getAdminPictures() {
        return listPictures().stream().map(this::toAdminVo).toList();
    }

    @Transactional
    @CacheEvict(cacheNames = "pictures", allEntries = true)
    public AdminPictureVo createPicture(PictureWriteRequest request) {
        Picture picture = new Picture();
        picture.setId(UUID.randomUUID().toString());
        picture.setSortOrder(Math.toIntExact(pictureMapper.selectCount(Wrappers.emptyWrapper())));
        applyRequest(picture, request, LocalDateTime.now());
        if (pictureMapper.insert(picture) != 1) {
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "相册分组保存失败");
        }
        replacePictureImages(picture.getId(), normalizeImages(request.images()));
        return toAdminVo(picture);
    }

    @Transactional
    @CacheEvict(cacheNames = "pictures", allEntries = true)
    public AdminPictureVo updatePicture(String id, PictureWriteRequest request) {
        Picture picture = getRequiredPicture(id);
        applyRequest(picture, request, picture.getUploadedAt());
        if (pictureMapper.updateById(picture) != 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "相册分组不存在");
        }
        replacePictureImages(picture.getId(), normalizeImages(request.images()));
        return toAdminVo(picture);
    }

    @Transactional
    @CacheEvict(cacheNames = "pictures", allEntries = true)
    public void deletePicture(String id) {
        getRequiredPicture(id);
        if (pictureMapper.deleteById(id) != 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "相册分组不存在");
        }
    }

    private List<Picture> listPictures() {
        return pictureMapper.selectList(Wrappers.<Picture>lambdaQuery()
                .orderByAsc(Picture::getSortOrder)
                .orderByAsc(Picture::getUploadedAt)
                .orderByAsc(Picture::getCreatedAt));
    }

    private Picture getRequiredPicture(String id) {
        Picture picture = pictureMapper.selectById(id);
        if (picture == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "相册分组不存在");
        }
        return picture;
    }

    private void applyRequest(Picture picture, PictureWriteRequest request, LocalDateTime defaultUploadedAt) {
        List<ManagedImageUrlService.ManagedImageReference> images = normalizeImages(request.images());
        picture.setImages(jsonContentReader.writeStringList(images.stream()
                .map(ManagedImageUrlService.ManagedImageReference::storedUrl)
                .toList()));
        picture.setDescription(trimToNull(request.description()));
        picture.setUploadedAt(request.uploadedAt() == null ? defaultUploadedAt : request.uploadedAt());
    }

    private void replacePictureImages(String pictureId, List<ManagedImageUrlService.ManagedImageReference> images) {
        pictureImageMapper.delete(Wrappers.<PictureImage>lambdaQuery()
                .eq(PictureImage::getPictureId, pictureId));
        for (int index = 0; index < images.size(); index++) {
            ManagedImageUrlService.ManagedImageReference imageReference = images.get(index);
            PictureImage image = new PictureImage();
            image.setPictureId(pictureId);
            image.setFileAssetId(imageReference.fileAssetId());
            image.setUrl(imageReference.storedUrl());
            image.setSortOrder(index);
            if (pictureImageMapper.insert(image) != 1) {
                throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "相册图片引用保存失败");
            }
        }
    }

    private List<ManagedImageUrlService.ManagedImageReference> normalizeImages(List<String> images) {
        LinkedHashMap<String, ManagedImageUrlService.ManagedImageReference> values = new LinkedHashMap<>();
        for (String image : images) {
            ManagedImageUrlService.ManagedImageReference reference = managedImageUrlService
                    .normalizeAndValidate(image, ImageUploadScope.PICTURES);
            if (reference.storedUrl() == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "相册图片 URL 不能为空");
            }
            values.putIfAbsent(reference.storedUrl(), reference);
        }
        if (values.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "相册分组至少需要一张图片");
        }
        return new ArrayList<>(values.values());
    }

    private List<String> readPictureImages(Picture picture) {
        List<PictureImage> images = pictureImageMapper.selectList(Wrappers.<PictureImage>lambdaQuery()
                .eq(PictureImage::getPictureId, picture.getId())
                .orderByAsc(PictureImage::getSortOrder)
                .orderByAsc(PictureImage::getId));
        if (images.isEmpty()) {
            return jsonContentReader.readStringList(picture.getImages()).stream()
                    .map(url -> managedImageUrlService.toPublicUrl(url, ImageUploadScope.PICTURES))
                    .toList();
        }
        return images.stream()
                .map(PictureImage::getUrl)
                .map(url -> managedImageUrlService.toPublicUrl(url, ImageUploadScope.PICTURES))
                .toList();
    }

    private PictureVo toPublicVo(Picture picture) {
        return new PictureVo(
                picture.getId(),
                toDateString(picture.getUploadedAt() == null ? picture.getCreatedAt() : picture.getUploadedAt()),
                picture.getDescription(),
                readPictureImages(picture)
        );
    }

    private AdminPictureVo toAdminVo(Picture picture) {
        return new AdminPictureVo(
                picture.getId(),
                toDateString(picture.getUploadedAt() == null ? picture.getCreatedAt() : picture.getUploadedAt()),
                picture.getDescription(),
                readPictureImages(picture)
        );
    }

    private String toDateString(LocalDateTime dateTime) {
        return dateTime == null ? null : DATE_TIME_FORMATTER.format(dateTime);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
