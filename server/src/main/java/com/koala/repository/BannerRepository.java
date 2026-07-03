package com.koala.repository;

import com.koala.entity.Banner;

import java.util.List;

public interface BannerRepository {

    Banner findById(Long id);

    /** 全部 banner,按 sortOrder、id 升序。 */
    List<Banner> findAll();

    /** 启用 banner,按 sortOrder、id 升序。 */
    List<Banner> findEnabled();

    void insert(Banner banner);

    void updateById(Banner banner);

    void deleteById(Long id);
}
