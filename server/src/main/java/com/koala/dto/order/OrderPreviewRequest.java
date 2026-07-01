package com.koala.dto.order;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;

/** 算价预览：商品项 + 可选地址(算运费与地址无关，此处仅回填)。 */
@Data
public class OrderPreviewRequest {

    @Valid
    @NotEmpty(message = "商品项不能为空")
    private List<OrderItemRequest> items;

    /** 可选：指定地址id用于回填收货信息。 */
    private Long addressId;
}
