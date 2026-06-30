package com.koala.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("region")
public class Region implements Serializable {

    @TableId(type = IdType.INPUT)
    private String code;
    private String parentCode;
    private String name;
    private Integer level;
}
