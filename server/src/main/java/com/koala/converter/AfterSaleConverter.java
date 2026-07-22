package com.koala.converter;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.koala.dto.aftersale.AdminAfterSaleView;
import com.koala.dto.aftersale.AfterSaleView;
import com.koala.entity.AfterSale;

import java.util.Collections;
import java.util.List;

/** 售后 entity → 视图。纯字段拷贝 + evidenceImages JSON 反序列化。 */
public final class AfterSaleConverter {

    private AfterSaleConverter() {}

    public static AfterSaleView toView(AfterSale as) {
        AfterSaleView v = new AfterSaleView();
        v.setAfterSaleNo(as.getAfterSaleNo());
        v.setOrderNo(as.getOrderNo());
        v.setType(as.getType());
        v.setReason(as.getReason());
        v.setRemark(as.getRemark());
        v.setEvidenceImages(parseImages(as.getEvidenceImages()));
        v.setRefundAmount(as.getRefundAmount());
        v.setReturnTrackingNo(as.getReturnTrackingNo());
        v.setStatus(as.getStatus());
        v.setAuditRemark(as.getAuditRemark());
        v.setCreatedAt(as.getCreatedAt());
        v.setUpdatedAt(as.getUpdatedAt());
        return v;
    }

    public static AdminAfterSaleView toAdminView(AfterSale as, String nickname) {
        AdminAfterSaleView v = new AdminAfterSaleView();
        v.setAfterSaleNo(as.getAfterSaleNo());
        v.setOrderNo(as.getOrderNo());
        v.setUserId(as.getUserId());
        v.setNickname(nickname);
        v.setType(as.getType());
        v.setReason(as.getReason());
        v.setRemark(as.getRemark());
        v.setEvidenceImages(parseImages(as.getEvidenceImages()));
        v.setRefundAmount(as.getRefundAmount());
        v.setReturnTrackingNo(as.getReturnTrackingNo());
        v.setStatus(as.getStatus());
        v.setAuditRemark(as.getAuditRemark());
        v.setCreatedAt(as.getCreatedAt());
        v.setUpdatedAt(as.getUpdatedAt());
        return v;
    }

    private static List<String> parseImages(String json) {
        if (StrUtil.isBlank(json)) {
            return Collections.emptyList();
        }
        return JSONUtil.toList(json, String.class);
    }
}
