package com.koala.dto.content;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class BannerSaveRequest {

    /** 新增为空，编辑必填。 */
    private Long id;

    @NotBlank(message = "图片URL不能为空")
    private String imageUrl;

    private String linkUrl;
    private Integer sortOrder;
    private Boolean valid;
}
