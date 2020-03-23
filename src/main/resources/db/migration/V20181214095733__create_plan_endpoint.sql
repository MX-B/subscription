CREATE TABLE `plan_endpoint` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `uuid` varchar(42) NOT NULL,
  `external_id` varchar(128)  NULL,
  `endpoint` varchar(256) NOT NULL,
  `plan_id` bigint(20) NOT NULL,

  `created_at` datetime NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `removed_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_uuid___plan_endpoint` (`uuid`),
  CONSTRAINT `FK_plan_endpoint___plan` FOREIGN KEY (`plan_id`) REFERENCES `plan` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE `plan_endpoint_metadata` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `plan_endpoint_id` bigint(20) NOT NULL,
  `property` varchar(32) NOT NULL,
  `value` varchar(32) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_plan_endpoint_metadata___plan_endpoint_property` (`plan_endpoint_id`,`property`),
  CONSTRAINT `FK_plan_endpoint_metadata___plan_endpoint` FOREIGN KEY (`plan_endpoint_id`) REFERENCES `plan_endpoint` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `plan` ADD `value` bigint(20) NULL;