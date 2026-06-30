package com.koala.dto.product;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class CategorySaveRequest {

    /** 新增为空，编辑必填。 */
    private Long id;

    @NotBlank(message = "分类名不能为空")
    @Size(max = 32, message = "分类名过长")
    private String name;

    private String iconUrl;
    private Integer sortOrder;
    private Boolean valid;
}
