package com.koala.dto.product;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class TagSaveRequest {

    /** 新增为空，编辑必填。 */
    private Long id;

    @NotBlank(message = "标签名不能为空")
    @Size(max = 16, message = "标签名过长")
    private String name;

    private Integer sortOrder;
    private Boolean valid;
}
