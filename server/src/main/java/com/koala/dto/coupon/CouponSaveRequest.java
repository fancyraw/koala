package com.koala.dto.coupon;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CouponSaveRequest {

    /** 新增为空，编辑必填。 */
    private Long id;

    @NotBlank(message = "券名称不能为空")
    @Size(max = 64, message = "券名称过长")
    private String name;

    /** 1=满减券 2=无门槛券 */
    @NotNull(message = "券类型不能为空")
    private Integer type;

    @NotNull(message = "优惠金额不能为空")
    @DecimalMin(value = "0.01", message = "优惠金额需大于0")
    private BigDecimal discountAmount;

    /** 满减门槛（无门槛券传 0 或留空）。 */
    private BigDecimal minSpend;

    @NotNull(message = "发行总量不能为空")
    private Integer totalCount;

    /** 1=固定区间 2=领取后N天 */
    @NotNull(message = "有效期类型不能为空")
    private Integer validityType;

    private LocalDateTime validStartAt;
    private LocalDateTime validEndAt;
    private Integer validDays;
}
