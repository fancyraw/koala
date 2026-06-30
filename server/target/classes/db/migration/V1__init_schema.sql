-- =====================================================================
-- Koala 初始化 schema（对齐《整体技术设计.md》第五章）
-- 引擎 InnoDB / 字符集 utf8mb4；金额 DECIMAL(10,2)；并发表加 version 乐观锁。
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 5.1 用户与登录
-- ---------------------------------------------------------------------
CREATE TABLE user (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    nickname        VARCHAR(64)  DEFAULT '' COMMENT '昵称',
    avatar_url      VARCHAR(512) DEFAULT '' COMMENT '头像URL',
    is_valid        TINYINT      DEFAULT 1 COMMENT '是否有效:1=是 0=否',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE user_auth (
    id         BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    user_id    BIGINT NOT NULL COMMENT '用户ID',
    auth_type  VARCHAR(16)  NOT NULL COMMENT '凭证类型:wechat_mp/email',
    auth_id    VARCHAR(128) NOT NULL COMMENT '凭证标识:openid/邮箱',
    created_at DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_type_id (auth_type, auth_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户登录凭证表';

CREATE TABLE user_address (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    name VARCHAR(32) NOT NULL COMMENT '收货人姓名',
    phone VARCHAR(20) NOT NULL COMMENT '收货人手机号',
    province_code CHAR(6) NOT NULL COMMENT '省级行政区划码(adcode,GB/T2260)',
    city_code CHAR(6) NOT NULL COMMENT '市级行政区划码',
    district_code CHAR(6) NOT NULL COMMENT '区/县级行政区划码',
    province VARCHAR(32) NOT NULL COMMENT '省(中文名,展示冗余)',
    city VARCHAR(32) NOT NULL COMMENT '市(中文名,展示冗余)',
    district VARCHAR(32) NOT NULL COMMENT '区/县(中文名,展示冗余)',
    detail VARCHAR(256) NOT NULL COMMENT '详细地址',
    full_address VARCHAR(512) NOT NULL COMMENT '完整地址(冗余,省市区+详细)',
    is_default TINYINT DEFAULT 0 COMMENT '是否默认地址:1=是 0=否',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户地址表';

CREATE TABLE region (
    code CHAR(6) PRIMARY KEY COMMENT '行政区划码(adcode)',
    parent_code CHAR(6) COMMENT '父级码(省级为NULL)',
    name VARCHAR(64) NOT NULL COMMENT '区划名称',
    level TINYINT NOT NULL COMMENT '层级:1=省 2=市 3=区/县',
    INDEX idx_parent (parent_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='行政区划字典表';

-- ---------------------------------------------------------------------
-- 5.2 商品与分类
-- ---------------------------------------------------------------------
CREATE TABLE product_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    name VARCHAR(32) NOT NULL COMMENT '分类名',
    icon_url VARCHAR(512) DEFAULT '' COMMENT '分类图标URL',
    sort_order INT DEFAULT 0 COMMENT '排序值(小在前)',
    is_valid TINYINT DEFAULT 1 COMMENT '是否有效:1=是 0=否',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表(仅商品数为0可删除,业务层校验)';

CREATE TABLE product_tag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    name VARCHAR(16) NOT NULL COMMENT '标签名:热销/新品/礼盒/特惠等',
    sort_order INT DEFAULT 0 COMMENT '排序值(小在前)',
    is_valid TINYINT DEFAULT 1 COMMENT '是否有效:1=是 0=否',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品标签表(仅引用数为0可删除,业务层校验)';

CREATE TABLE product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    name VARCHAR(128) NOT NULL COMMENT '商品名',
    main_image VARCHAR(512) NOT NULL COMMENT '主图URL',
    detail_images TEXT DEFAULT NULL COMMENT '详情图URL数组JSON',
    tag_id BIGINT DEFAULT 0 COMMENT '商品标签(单选可空,0=无;引用 product_tag.id);C端卡片角标',
    is_recommended TINYINT DEFAULT 0 COMMENT '是否推荐(C端精选推荐)',
    highlights VARCHAR(512) DEFAULT '' COMMENT '产品亮点JSON数组;C端精选推荐卡复用首条作种草语',
    category_id BIGINT NOT NULL COMMENT '分类ID',
    per_order_limit INT DEFAULT 0 COMMENT '单次下单限购上限(商品级,0=不限);C端达上限拦截加量',
    sales_count INT DEFAULT 0 COMMENT '销量',
    is_valid TINYINT DEFAULT 1 COMMENT '是否有效:1=上架 0=下架',
    created_by BIGINT DEFAULT 0 COMMENT '创建人(admin.id)',
    updated_by BIGINT DEFAULT 0 COMMENT '更新人(admin.id)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_category (category_id),
    INDEX idx_tag (tag_id),
    INDEX idx_valid (is_valid),
    INDEX idx_sales (sales_count DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

CREATE TABLE product_sku (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    name VARCHAR(64) NOT NULL COMMENT '规格名',
    price DECIMAL(10,2) NOT NULL COMMENT '售价',
    stock INT DEFAULT 0 COMMENT '库存',
    sort_order INT DEFAULT 0 COMMENT '排序值(小在前)',
    version INT DEFAULT 0 COMMENT '乐观锁(扣减库存)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品规格表(最多4个/商品);库存预警阈值在业务层';

-- ---------------------------------------------------------------------
-- 5.3 优惠券
-- ---------------------------------------------------------------------
CREATE TABLE coupon (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    name VARCHAR(64) NOT NULL COMMENT '券名称',
    type TINYINT NOT NULL COMMENT '1=满减券 2=无门槛券',
    discount_amount DECIMAL(10,2) NOT NULL COMMENT '优惠金额',
    min_spend DECIMAL(10,2) DEFAULT 0.00 COMMENT '满减门槛(无门槛=0)',
    total_count INT NOT NULL COMMENT '发行总量',
    issued_count INT DEFAULT 0 COMMENT '已发(下发的用户券数)',
    used_count INT DEFAULT 0 COMMENT '已用',
    validity_type TINYINT NOT NULL DEFAULT 2 COMMENT '有效期类型:1=固定区间(start/end) 2=领取后N天',
    valid_start_at DATETIME DEFAULT NULL COMMENT '固定区间-开始时间(validity_type=1必填)',
    valid_end_at   DATETIME DEFAULT NULL COMMENT '固定区间-结束时间(validity_type=1必填;到期=该时刻)',
    valid_days INT DEFAULT NULL COMMENT '领取后有效天数(validity_type=2必填;到期=领取时间+N天)',
    is_valid TINYINT DEFAULT 1 COMMENT '是否有效:1=正常 0=已停发',
    version INT DEFAULT 0 COMMENT '乐观锁(防超发)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='券模板表(已开始即有下发记录的不可删除,只能停发)';

CREATE TABLE user_coupon (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    coupon_id BIGINT NOT NULL COMMENT '券模板ID',
    status TINYINT DEFAULT 0 COMMENT '0=未使用 1=已使用 2=已过期 3=已锁定(下单未支付)',
    lock_order_no VARCHAR(32) DEFAULT NULL COMMENT '锁定/使用的订单号',
    expire_at DATETIME NOT NULL COMMENT '到期时间(冗余,临期倒计时与过期任务)',
    granted_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '下发时间',
    used_at DATETIME DEFAULT NULL COMMENT '使用时间',
    UNIQUE KEY uk_user_coupon (user_id, coupon_id) COMMENT '每模版每人至多1张,兜底防并发重复下发',
    INDEX idx_user_status (user_id, status),
    INDEX idx_expire (status, expire_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户优惠券表';

-- ---------------------------------------------------------------------
-- 5.4 订单 / 支付 / 售后
-- ---------------------------------------------------------------------
CREATE TABLE `order` (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    order_no VARCHAR(32) NOT NULL UNIQUE COMMENT '订单号(业务主键)',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    receiver_name VARCHAR(32) NOT NULL COMMENT '收货人姓名',
    receiver_phone VARCHAR(20) NOT NULL COMMENT '收货人手机号',
    receiver_address VARCHAR(256) NOT NULL COMMENT '收货地址(完整快照)',
    product_amount  DECIMAL(10,2) NOT NULL COMMENT '商品合计(规格售价×数量)',
    coupon_discount DECIMAL(10,2) DEFAULT 0.00 COMMENT '券抵扣合计(无门槛+满减)',
    shipping_fee    DECIMAL(10,2) DEFAULT 0.00 COMMENT '运费',
    pay_amount      DECIMAL(10,2) NOT NULL COMMENT '实付',
    logistics_company VARCHAR(32) DEFAULT '' COMMENT '物流公司',
    logistics_no VARCHAR(64) DEFAULT '' COMMENT '物流单号',
    status TINYINT DEFAULT 0 COMMENT '0=待付款 1=待发货 2=待收货 3=已完成 4=已取消 5=售后中 6=已退款',
    remark VARCHAR(256) DEFAULT '' COMMENT '订单备注',
    user_deleted TINYINT DEFAULT 0 COMMENT 'C端删除(软删,后台仍可见)',
    paid_at DATETIME DEFAULT NULL COMMENT '支付时间',
    shipped_at DATETIME DEFAULT NULL COMMENT '发货时间',
    completed_at DATETIME DEFAULT NULL COMMENT '完成(确认收货)时间',
    canceled_at DATETIME DEFAULT NULL COMMENT '取消时间',
    expire_at DATETIME DEFAULT NULL COMMENT '支付截止(创建+超时分钟)',
    version INT DEFAULT 0 COMMENT '乐观锁',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建(下单)时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user (user_id, status),
    INDEX idx_status (status),
    INDEX idx_expire (status, expire_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

CREATE TABLE order_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    order_no VARCHAR(32) NOT NULL COMMENT '订单号',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    sku_id BIGINT NOT NULL COMMENT '规格ID',
    product_name VARCHAR(128) NOT NULL COMMENT '商品名快照',
    sku_name VARCHAR(64) NOT NULL COMMENT '规格名快照',
    product_image VARCHAR(512) NOT NULL COMMENT '商品图快照',
    unit_price DECIMAL(10,2) NOT NULL COMMENT '成交单价(快照)',
    quantity INT NOT NULL COMMENT '购买数量',
    subtotal DECIMAL(10,2) NOT NULL COMMENT '小计(单价×数量)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单商品项表';

CREATE TABLE order_coupon (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    order_no VARCHAR(32) NOT NULL COMMENT '订单号',
    user_coupon_id BIGINT NOT NULL COMMENT '用户券ID',
    coupon_id BIGINT NOT NULL COMMENT '券模板ID',
    coupon_type TINYINT NOT NULL COMMENT '1=满减 2=无门槛',
    discount_amount DECIMAL(10,2) NOT NULL COMMENT '本券抵扣金额',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单用券表(每单每类型各一张)';

CREATE TABLE payment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    order_no VARCHAR(32) NOT NULL COMMENT '订单号',
    channel VARCHAR(16) DEFAULT 'wechat' COMMENT '支付渠道(扩展点)',
    transaction_id VARCHAR(64) DEFAULT NULL COMMENT '渠道交易号(回调幂等键)',
    pay_amount DECIMAL(10,2) NOT NULL COMMENT '支付金额',
    status TINYINT DEFAULT 0 COMMENT '0=待支付 1=成功 2=失败 3=已退款',
    paid_at DATETIME DEFAULT NULL COMMENT '支付成功时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_transaction (transaction_id),
    INDEX idx_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付记录表';

CREATE TABLE after_sale (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    after_sale_no VARCHAR(32) NOT NULL UNIQUE COMMENT '售后单号(业务主键)',
    order_no VARCHAR(32) NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    type TINYINT NOT NULL COMMENT '1=仅退款(未发货) 2=退货退款(已发货)',
    reason VARCHAR(64) DEFAULT '' COMMENT '售后原因',
    remark VARCHAR(256) DEFAULT '' COMMENT '买家补充说明',
    evidence_images TEXT DEFAULT NULL COMMENT '凭证图JSON(最多3张)',
    refund_amount DECIMAL(10,2) NOT NULL COMMENT '退款金额',
    return_tracking_no VARCHAR(64) DEFAULT '' COMMENT '买家寄回单号',
    status TINYINT DEFAULT 0 COMMENT '0待审核 1通过待寄回 2买家已寄回 3商家已收货 4已退款 5已拒绝',
    audit_remark VARCHAR(256) DEFAULT '' COMMENT '商家审核备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建(申请)时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_order_no (order_no),
    INDEX idx_user (user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='售后单表';

-- ---------------------------------------------------------------------
-- 5.5 购物车 / 内容 / 配置 / 管理员 / 日志
-- ---------------------------------------------------------------------
CREATE TABLE cart_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    sku_id BIGINT NOT NULL COMMENT '规格ID',
    quantity INT DEFAULT 1 COMMENT '数量',
    checked TINYINT DEFAULT 1 COMMENT '是否勾选结算:1=是 0=否',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_sku (user_id, sku_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车表';

CREATE TABLE banner (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    image_url VARCHAR(512) NOT NULL COMMENT '图片URL',
    link_url VARCHAR(512) DEFAULT '' COMMENT '跳转链接(可空)',
    sort_order INT DEFAULT 0 COMMENT '排序值(小在前)',
    is_valid TINYINT DEFAULT 1 COMMENT '1=上线 0=下线',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Banner表(纯图)';

CREATE TABLE sys_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    config_group VARCHAR(32) NOT NULL COMMENT '配置分组:shipping/payment/order',
    config_key VARCHAR(64) NOT NULL COMMENT '配置键',
    config_value TEXT NOT NULL COMMENT '配置值',
    remark VARCHAR(128) DEFAULT '' COMMENT '备注',
    updated_by BIGINT DEFAULT 0 COMMENT '更新人(admin.id)',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_group_key (config_group, config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表(运行期业务策略)';

CREATE TABLE admin (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    wx_openid VARCHAR(64) NOT NULL UNIQUE COMMENT '微信openid(唯一登录凭证)',
    nickname VARCHAR(32) DEFAULT '' COMMENT '昵称(扫码登录时取微信资料回填)',
    avatar_url VARCHAR(512) DEFAULT '' COMMENT '头像URL(扫码登录时取微信资料回填)',
    is_super TINYINT DEFAULT 0 COMMENT '是否超管:1=是 0=否(仅用于管理员管理能力位)',
    is_valid TINYINT DEFAULT 1 COMMENT '1=有效 0=禁用(含待审核:邀请扫码后置0待超管启用)',
    last_login_at DATETIME DEFAULT NULL COMMENT '最后登录时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员表';

CREATE TABLE operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    operator_type TINYINT NOT NULL COMMENT '1=C端业务 2=后台管理员 3=系统',
    operator_id BIGINT NOT NULL COMMENT '操作者ID',
    operator_name VARCHAR(64) NOT NULL COMMENT '操作者名称',
    module VARCHAR(32) NOT NULL COMMENT '模块',
    action VARCHAR(32) NOT NULL COMMENT '动作',
    target_id VARCHAR(64) DEFAULT '' COMMENT '操作对象ID',
    content TEXT NOT NULL COMMENT '操作内容描述',
    result VARCHAR(16) DEFAULT 'success' COMMENT '结果:success/fail',
    ip VARCHAR(64) DEFAULT '' COMMENT '操作IP',
    trace_id VARCHAR(40) DEFAULT '' COMMENT '链路追踪ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_type_time (operator_type, created_at),
    INDEX idx_module (module)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作/业务日志表';

CREATE TABLE metrics_snapshot (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    captured_at DATETIME NOT NULL COMMENT '采样时刻(定时任务每分钟一行)',
    qps INT DEFAULT 0 COMMENT '采样窗口内 QPS',
    p95_ms INT DEFAULT 0 COMMENT '全接口 p95 耗时(ms)',
    error_rate DECIMAL(5,2) DEFAULT 0 COMMENT '4xx/5xx 占比(%)',
    db_pool_pending INT DEFAULT 0 COMMENT 'HikariCP 等待连接数',
    cpu_percent INT DEFAULT 0 COMMENT 'CPU 使用率(%)',
    heap_used_mb INT DEFAULT 0 COMMENT '堆已用(MB)',
    INDEX idx_captured (captured_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统监控指标快照表(定时采样,仅保留近7天)';
