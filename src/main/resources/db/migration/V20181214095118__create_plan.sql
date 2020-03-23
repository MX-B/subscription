CREATE TABLE `plan_modality` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `code` varchar(64) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_uuid___plan_modality` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `plan` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `uuid` varchar(42) NOT NULL,
  `name` varchar(64) DEFAULT NULL,
  `description` varchar(256) DEFAULT NULL,
  `api_gateway_tenant_id` bigint(20) NOT NULL,
  `priority` int(11) NOT NULL DEFAULT 0,
  `modality_id` bigint(20) NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `removed_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_uuid___plan` (`uuid`),
  CONSTRAINT `FK_plan___api_gateway_tenant` FOREIGN KEY (`api_gateway_tenant_id`) REFERENCES `api_gateway_tenant` (`id`),
  CONSTRAINT `FK_plan___modality` FOREIGN KEY (`modality_id`) REFERENCES `plan_modality` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `plan_metadata` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `plan_id` bigint(20) NOT NULL,
  `property` varchar(32) NOT NULL,
  `value` varchar(32) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_plan_metadata___plan_property` (`plan_id`,`property`),
  CONSTRAINT `FK_plan_metadata___plan` FOREIGN KEY (`plan_id`) REFERENCES `plan` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `subscription` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `uuid` varchar(42) NOT NULL,
  `unsubscribed_at` datetime DEFAULT NULL,
  `user_id` varchar(64) DEFAULT NULL,
  `plan_id` bigint(20) NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `removed_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_uuid___subscription` (`uuid`),
  CONSTRAINT `FK_subscription___plan` FOREIGN KEY (`plan_id`) REFERENCES `plan` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;