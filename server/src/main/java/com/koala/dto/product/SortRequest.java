package com.koala.dto.product;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class SortRequest {

    @NotEmpty(message = "排序项不能为空")
    @Valid
    private List<SortItem> items;
}
