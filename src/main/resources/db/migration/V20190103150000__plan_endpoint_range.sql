CREATE TABLE `plan_endpoint_range` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `plan_endpoint_id` bigint(20) NOT NULL,
  `init_range` bigint(20) NOT NULL,
  `final_range` bigint(20) NULL,
  `value` bigint(20) NOT NULL,

  PRIMARY KEY (`id`),
  CONSTRAINT `FK_plan_endpoint_range__plan_endpoint` FOREIGN KEY (`plan_endpoint_id`) REFERENCES `plan_endpoint` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `plan_modality` (`id`, `code`) VALUES (4, 'UNLIMITED_RANGE');