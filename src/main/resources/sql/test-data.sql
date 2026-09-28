-- 本地演示数据：可重复执行。
-- 客户账号：13800138000 / 123456，拥有 QA-CAR-2026-A001。
-- 隔离账号：13800138001 / 123456，拥有 QA-CAR-2025-B002，用于验证车辆数据隔离。

INSERT INTO `user_info` (`phone`, `password`, `name`, `nickname`, `status`)
VALUES
    ('13800138000', '123456', '测试客户', '小测', 'ACTIVE'),
    ('13800138001', '123456', '隔离测试客户', '小隔', 'ACTIVE')
ON DUPLICATE KEY UPDATE
    `password` = VALUES(`password`),
    `name` = VALUES(`name`),
    `nickname` = VALUES(`nickname`),
    `status` = VALUES(`status`);

INSERT INTO `car_info` (
    `info_id`, `brand`, `model_name`, `model_year`, `version`, `full_name`,
    `vehicle_type`, `fuel_type`, `seat_count`, `displacement`, `motor_power`, `range_km`,
    `guide_price`, `color_options`, `dimensions`, `wheelbase`, `manufacturer`, `status`, `description`, `remark`
)
VALUES
    ('QA-CI-E7-2026', '测试汽车', '远航 E7', 2026, '长续航智驾版', '测试汽车 远航 E7 2026款 长续航智驾版',
     'SUV', '纯电', 5, NULL, 230.00, 620, 23.98, '珍珠白,星云蓝,曜石黑', '4860x1935x1660', 2920, '测试汽车制造有限公司', '在售',
     '用于全流程验证的纯电 SUV 测试车型。', '测试数据，请勿作为真实商品信息。'),
    ('QA-CI-H5-2025', '测试汽车', '远航 H5', 2025, '超混尊享版', '测试汽车 远航 H5 2025款 超混尊享版',
     'SUV', '插电混动', 5, 1.5, 120.00, 125, 19.98, '月光银,深海灰,松石绿', '4750x1900x1680', 2810, '测试汽车制造有限公司', '在售',
     '用于全流程验证的插电混动 SUV 测试车型。', '测试数据，请勿作为真实商品信息。'),
    ('QA-CI-X3-2026', '测试汽车', '都市 X3', 2026, '舒享版', '测试汽车 都市 X3 2026款 舒享版',
     '轿车', '纯电', 5, NULL, 150.00, 510, 16.98, '极地白,晨雾灰,流光紫', '4680x1820x1480', 2760, '测试汽车制造有限公司', '在售',
     '用于验证待支付订单和车型对比的纯电轿车测试车型。', '测试数据，请勿作为真实商品信息。')
ON DUPLICATE KEY UPDATE
    `brand` = VALUES(`brand`),
    `model_name` = VALUES(`model_name`),
    `model_year` = VALUES(`model_year`),
    `version` = VALUES(`version`),
    `full_name` = VALUES(`full_name`),
    `vehicle_type` = VALUES(`vehicle_type`),
    `fuel_type` = VALUES(`fuel_type`),
    `seat_count` = VALUES(`seat_count`),
    `displacement` = VALUES(`displacement`),
    `motor_power` = VALUES(`motor_power`),
    `range_km` = VALUES(`range_km`),
    `guide_price` = VALUES(`guide_price`),
    `color_options` = VALUES(`color_options`),
    `dimensions` = VALUES(`dimensions`),
    `wheelbase` = VALUES(`wheelbase`),
    `manufacturer` = VALUES(`manufacturer`),
    `status` = VALUES(`status`),
    `description` = VALUES(`description`),
    `remark` = VALUES(`remark`);

INSERT INTO `car_order` (
    `order_id`, `user_id`, `car_id`, `order_no`, `order_type`, `order_status`, `brand`, `model`, `color`, `vin`,
    `seller_name`, `seller_contact`, `vehicle_price`, `purchase_tax`, `insurance_fee`, `other_fee`,
    `total_amount`, `discount_amount`, `actual_amount`, `payment_method`, `order_date`, `delivery_date`, `remark`
)
SELECT
    'QA-ORDER-2026-A001', CAST(`id` AS CHAR), 'QA-CAR-2026-A001', 'QA20260318001', '购买', '已完成', '测试汽车',
    '远航 E7 2026款 长续航智驾版', '星云蓝', 'LQAE72026QA000001', '测试汽车浦东体验中心', '021-55550101',
    239800.00, 0.00, 6800.00, 2000.00, 248600.00, 19800.00, 228800.00, '全款', '2026-03-10', '2026-03-18',
    '已完成交付的车主订单测试数据。'
FROM `user_info`
WHERE `phone` = '13800138000'
ON DUPLICATE KEY UPDATE
    `user_id` = VALUES(`user_id`), `car_id` = VALUES(`car_id`), `order_status` = VALUES(`order_status`),
    `brand` = VALUES(`brand`), `model` = VALUES(`model`), `color` = VALUES(`color`), `vin` = VALUES(`vin`),
    `seller_name` = VALUES(`seller_name`), `seller_contact` = VALUES(`seller_contact`),
    `vehicle_price` = VALUES(`vehicle_price`), `purchase_tax` = VALUES(`purchase_tax`),
    `insurance_fee` = VALUES(`insurance_fee`), `other_fee` = VALUES(`other_fee`),
    `total_amount` = VALUES(`total_amount`), `discount_amount` = VALUES(`discount_amount`),
    `actual_amount` = VALUES(`actual_amount`), `payment_method` = VALUES(`payment_method`),
    `order_date` = VALUES(`order_date`), `delivery_date` = VALUES(`delivery_date`), `remark` = VALUES(`remark`);

INSERT INTO `car_order` (
    `order_id`, `user_id`, `car_id`, `order_no`, `order_type`, `order_status`, `brand`, `model`, `color`, `vin`,
    `seller_name`, `seller_contact`, `vehicle_price`, `purchase_tax`, `insurance_fee`, `other_fee`,
    `total_amount`, `discount_amount`, `actual_amount`, `payment_method`, `order_date`, `delivery_date`, `remark`
)
SELECT
    'QA-ORDER-2026-C003', CAST(`id` AS CHAR), NULL, 'QA20260925003', '购买', '等待支付', '测试汽车',
    '都市 X3 2026款 舒享版', '晨雾灰', NULL, '测试汽车浦东体验中心', '021-55550101',
    169800.00, 0.00, 5600.00, 1800.00, 177200.00, 12800.00, 0.00, '全款', '2026-09-25', NULL,
    '尚未支付的订单测试数据。'
FROM `user_info`
WHERE `phone` = '13800138000'
ON DUPLICATE KEY UPDATE
    `user_id` = VALUES(`user_id`), `car_id` = VALUES(`car_id`), `order_status` = VALUES(`order_status`),
    `brand` = VALUES(`brand`), `model` = VALUES(`model`), `color` = VALUES(`color`), `vin` = VALUES(`vin`),
    `seller_name` = VALUES(`seller_name`), `seller_contact` = VALUES(`seller_contact`),
    `vehicle_price` = VALUES(`vehicle_price`), `purchase_tax` = VALUES(`purchase_tax`),
    `insurance_fee` = VALUES(`insurance_fee`), `other_fee` = VALUES(`other_fee`),
    `total_amount` = VALUES(`total_amount`), `discount_amount` = VALUES(`discount_amount`),
    `actual_amount` = VALUES(`actual_amount`), `payment_method` = VALUES(`payment_method`),
    `order_date` = VALUES(`order_date`), `delivery_date` = VALUES(`delivery_date`), `remark` = VALUES(`remark`);

INSERT INTO `car_order` (
    `order_id`, `user_id`, `car_id`, `order_no`, `order_type`, `order_status`, `brand`, `model`, `color`, `vin`,
    `seller_name`, `seller_contact`, `vehicle_price`, `purchase_tax`, `insurance_fee`, `other_fee`,
    `total_amount`, `discount_amount`, `actual_amount`, `payment_method`, `order_date`, `delivery_date`, `remark`
)
SELECT
    'QA-ORDER-2025-B002', CAST(`id` AS CHAR), 'QA-CAR-2025-B002', 'QA20250520002', '购买', '已支付', '测试汽车',
    '远航 H5 2025款 超混尊享版', '深海灰', 'LQAH52025QB000002', '测试汽车宁波体验中心', '0574-55550202',
    199800.00, 8600.00, 7200.00, 2400.00, 218000.00, 12000.00, 206000.00, '分期', '2025-05-20', '2025-05-31',
    '隔离账号的已支付订单测试数据。'
FROM `user_info`
WHERE `phone` = '13800138001'
ON DUPLICATE KEY UPDATE
    `user_id` = VALUES(`user_id`), `car_id` = VALUES(`car_id`), `order_status` = VALUES(`order_status`),
    `brand` = VALUES(`brand`), `model` = VALUES(`model`), `color` = VALUES(`color`), `vin` = VALUES(`vin`),
    `seller_name` = VALUES(`seller_name`), `seller_contact` = VALUES(`seller_contact`),
    `vehicle_price` = VALUES(`vehicle_price`), `purchase_tax` = VALUES(`purchase_tax`),
    `insurance_fee` = VALUES(`insurance_fee`), `other_fee` = VALUES(`other_fee`),
    `total_amount` = VALUES(`total_amount`), `discount_amount` = VALUES(`discount_amount`),
    `actual_amount` = VALUES(`actual_amount`), `payment_method` = VALUES(`payment_method`),
    `order_date` = VALUES(`order_date`), `delivery_date` = VALUES(`delivery_date`), `remark` = VALUES(`remark`);

INSERT INTO `my_car` (
    `car_id`, `user_id`, `car_info_id`, `nickname`, `full_name`, `order_id`, `plate_number`, `color`, `vin`, `engine_number`,
    `purchase_date`, `purchase_price`, `mileage`, `register_date`, `insurance_expire_date`, `inspection_expire_date`, `remark`
)
SELECT
    'QA-CAR-2026-A001', CAST(`id` AS CHAR), 'QA-CI-E7-2026', '小远', '测试汽车 远航 E7 2026款 长续航智驾版',
    'QA-ORDER-2026-A001', '沪AQA927', '星云蓝', 'LQAE72026QA000001', 'E7QA20260001',
    '2026-03-18', 228800.00, 28640, '2026-03-18', '2027-03-17', '2028-03-17', '主测试账号名下车辆。'
FROM `user_info`
WHERE `phone` = '13800138000'
ON DUPLICATE KEY UPDATE
    `user_id` = VALUES(`user_id`), `car_info_id` = VALUES(`car_info_id`), `nickname` = VALUES(`nickname`),
    `full_name` = VALUES(`full_name`), `order_id` = VALUES(`order_id`), `plate_number` = VALUES(`plate_number`),
    `color` = VALUES(`color`), `vin` = VALUES(`vin`), `engine_number` = VALUES(`engine_number`),
    `purchase_date` = VALUES(`purchase_date`), `purchase_price` = VALUES(`purchase_price`), `mileage` = VALUES(`mileage`),
    `register_date` = VALUES(`register_date`), `insurance_expire_date` = VALUES(`insurance_expire_date`),
    `inspection_expire_date` = VALUES(`inspection_expire_date`), `remark` = VALUES(`remark`);

INSERT INTO `my_car` (
    `car_id`, `user_id`, `car_info_id`, `nickname`, `full_name`, `order_id`, `plate_number`, `color`, `vin`, `engine_number`,
    `purchase_date`, `purchase_price`, `mileage`, `register_date`, `insurance_expire_date`, `inspection_expire_date`, `remark`
)
SELECT
    'QA-CAR-2025-B002', CAST(`id` AS CHAR), 'QA-CI-H5-2025', '小混', '测试汽车 远航 H5 2025款 超混尊享版',
    'QA-ORDER-2025-B002', '浙BQB518', '深海灰', 'LQAH52025QB000002', 'H5QB20250002',
    '2025-05-31', 206000.00, 32580, '2025-05-31', '2026-05-30', '2027-05-30', '隔离测试账号名下车辆。'
FROM `user_info`
WHERE `phone` = '13800138001'
ON DUPLICATE KEY UPDATE
    `user_id` = VALUES(`user_id`), `car_info_id` = VALUES(`car_info_id`), `nickname` = VALUES(`nickname`),
    `full_name` = VALUES(`full_name`), `order_id` = VALUES(`order_id`), `plate_number` = VALUES(`plate_number`),
    `color` = VALUES(`color`), `vin` = VALUES(`vin`), `engine_number` = VALUES(`engine_number`),
    `purchase_date` = VALUES(`purchase_date`), `purchase_price` = VALUES(`purchase_price`), `mileage` = VALUES(`mileage`),
    `register_date` = VALUES(`register_date`), `insurance_expire_date` = VALUES(`insurance_expire_date`),
    `inspection_expire_date` = VALUES(`inspection_expire_date`), `remark` = VALUES(`remark`);
