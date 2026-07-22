-- Dashboard 走 paid_at 范围查询（OrderRepositoryImpl.findPaidSince），补索引避免全表扫。
ALTER TABLE `order` ADD INDEX `idx_paid_at` (`paid_at`);
