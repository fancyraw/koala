-- sys_config 预置（后台「系统设置」页可视化编辑，写入即刷新缓存）
INSERT INTO sys_config (config_group, config_key, config_value, remark) VALUES
('shipping', 'base_fee',          '8',                  '运费(元)'),
('shipping', 'free_threshold',    '99',                 '包邮门槛(商品合计达此值免运费)'),
('payment',  'active_channel',    'wechat',             '激活支付渠道'),
('payment',  'wechat_mch_id',     '',                   '微信商户号(密钥走环境变量)'),
('order',    'pay_timeout_minutes','30',                '支付超时分钟数'),
('order',    'auto_confirm_days',  '7',                 '自动确认收货天数'),
('system',   'maintenance_mode',   '0',                 '维护模式开关:0关 1开'),
('system',   'maintenance_notice', '系统维护中,请稍后再试', '维护提示文案');
