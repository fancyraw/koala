package com.koala.dto.dashboard;

import lombok.Data;

/** 待处理事项：店主要动手的活。 */
@Data
public class Pending {

    /** 待发货订单数(status=1)。 */
    private long toShip;
    /** 退款售后待处理数(售后单待审核/待收货)。 */
    private long afterSale;
}
