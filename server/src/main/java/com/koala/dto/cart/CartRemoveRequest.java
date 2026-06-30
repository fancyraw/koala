package com.koala.dto.cart;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class CartRemoveRequest {

    @NotEmpty(message = "ids不能为空")
    private List<Long> ids;
}
