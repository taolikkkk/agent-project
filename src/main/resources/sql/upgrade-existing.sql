-- 仅用于缺少对应字段的旧库；新库无需执行。
ALTER TABLE `staff_info` ADD COLUMN `password` VARCHAR(128) DEFAULT NULL comment '登录密码' AFTER `name`;

ALTER TABLE `chat_message`
    ADD COLUMN `cache_hit`        TINYINT       NOT NULL DEFAULT 0 comment '本次回答是否命中缓存：0未命中、1命中' AFTER `metadata`,
    ADD COLUMN `helpful`          TINYINT       NULL comment '回答评价：NULL未评价、1有帮助、0无帮助' AFTER `cache_hit`,
    ADD COLUMN `feedback_comment` VARCHAR(1000) NULL comment '用户评价说明' AFTER `helpful`,
    ADD COLUMN `feedback_at`      DATETIME(6)   NULL comment '首次评价时间' AFTER `feedback_comment`;
