package com.koala.dto.address;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class IdRequest {

    @NotNull(message = "id不能为空")
    private Long id;
}
