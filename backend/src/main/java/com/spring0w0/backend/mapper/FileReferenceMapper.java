package com.spring0w0.backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 集中查询各业务表对上传图片的引用，避免由 Controller 拼接 SQL。
 */
@Mapper
public interface FileReferenceMapper {

    @Select("""
            SELECT
                (SELECT COUNT(*) FROM blog_posts
                    WHERE cover_url = CONCAT('/images/', #{relativePath})
                       OR cover_url LIKE CONCAT('%/images/', #{relativePath}))
              + (SELECT COUNT(*) FROM blog_images
                    WHERE url = CONCAT('/images/', #{relativePath})
                       OR url LIKE CONCAT('%/images/', #{relativePath}))
              + (SELECT COUNT(*) FROM site_config
                    WHERE favicon_url = CONCAT('/images/', #{relativePath})
                       OR favicon_url LIKE CONCAT('%/images/', #{relativePath})
                       OR avatar_url = CONCAT('/images/', #{relativePath})
                       OR avatar_url LIKE CONCAT('%/images/', #{relativePath})
                       OR JSON_SEARCH(theme, 'one', CONCAT('%/images/', #{relativePath})) IS NOT NULL)
              + (SELECT COUNT(*) FROM art_images
                    WHERE url = CONCAT('/images/', #{relativePath})
                       OR url LIKE CONCAT('%/images/', #{relativePath}))
              + (SELECT COUNT(*) FROM background_images
                    WHERE url = CONCAT('/images/', #{relativePath})
                       OR url LIKE CONCAT('%/images/', #{relativePath}))
              + (SELECT COUNT(*) FROM bloggers
                    WHERE avatar_url = CONCAT('/images/', #{relativePath})
                       OR avatar_url LIKE CONCAT('%/images/', #{relativePath}))
              + (SELECT COUNT(*) FROM projects
                    WHERE image_url = CONCAT('/images/', #{relativePath})
                       OR image_url LIKE CONCAT('%/images/', #{relativePath}))
              + (SELECT COUNT(*) FROM shares
                    WHERE logo_url = CONCAT('/images/', #{relativePath})
                       OR logo_url LIKE CONCAT('%/images/', #{relativePath}))
              + (SELECT COUNT(*) FROM pictures
                    WHERE JSON_SEARCH(images, 'one', CONCAT('%/images/', #{relativePath})) IS NOT NULL)
              + (SELECT COUNT(*) FROM picture_images
                    WHERE file_asset_id = (SELECT id FROM file_assets WHERE relative_path = #{relativePath})
                       OR url = CONCAT('/images/', #{relativePath})
                       OR url LIKE CONCAT('%/images/', #{relativePath}))
            """)
    long countByRelativePath(@Param("relativePath") String relativePath);
}
