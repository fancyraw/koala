package com.koala.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** Banner(纯图)：created_at/updated_at 由 DB 默认值填充。 */
@Data
@TableName("banner")
public class Banner implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String imageUrl;
    private String linkUrl;
    private Integer sortOrder;
    /** 1=上线 0=下线 */
    private Integer isValid;
    @TableField(insertStrategy = com.baomidou.mybatisplus.annotation.FieldStrategy.NEVER)
    private LocalDateTime createdAt;
    @TableField(insertStrategy = com.baomidou.mybatisplus.annotation.FieldStrategy.NEVER,
            updateStrategy = com.baomidou.mybatisplus.annotation.FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}
