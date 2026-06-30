package com.koala.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.koala.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("product")
public class Product extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String mainImage;
    /** 详情图URL数组JSON */
    private String detailImages;
    /** 标签ID(单选可空,0=无) */
    private Long tagId;
    private Integer isRecommended;
    /** 产品亮点JSON数组 */
    private String highlights;
    private Long categoryId;
    /** 单次下单限购上限(0=不限) */
    private Integer perOrderLimit;
    private Integer salesCount;
    /** 1=上架 0=下架 */
    private Integer isValid;
    private Long createdBy;
    private Long updatedBy;
}
