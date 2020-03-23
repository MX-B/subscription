CREATE TABLE `partner` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `uuid` varchar(42) NOT NULL,
  `name` varchar(64) NOT NULL,
  `wallet_id` varchar(64) NOT NULL,
  `email` varchar(64) DEFAULT NULL,
  `phone` varchar(24) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `removed_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_uuid___partner` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `api` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `uuid` varchar(42) NOT NULL,
  `name` varchar(64) NOT NULL,
  `partner_id` bigint(20) NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `removed_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_uuid___api` (`uuid`),
  CONSTRAINT `FK_api___partner` FOREIGN KEY (`partner_id`) REFERENCES `partner` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `gateway` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `uuid` varchar(42) NOT NULL,
  `name` varchar(64) NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `removed_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_uuid___gateway` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `tenant` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `uuid` varchar(42) NOT NULL,
  `name` varchar(64) NOT NULL,
  `email` varchar(64) DEFAULT NULL,
  `phone` varchar(24) DEFAULT NULL,
  `logo` varchar(250) DEFAULT NULL,
  `wallet_id` varchar(64) NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `removed_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_uuid___tenant` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `api_gateway` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `uuid` varchar(42) NOT NULL,
  `external_id` varchar(128) NOT NULL,
  `api_id` bigint(20) NOT NULL,
  `gateway_id` bigint(20) NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `removed_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_uuid___api_gateway` (`uuid`),
  CONSTRAINT `FK_api_gateway___api` FOREIGN KEY (`api_id`) REFERENCES `api` (`id`),
  CONSTRAINT `FK_api_gateway___gateway` FOREIGN KEY (`gateway_id`) REFERENCES `gateway` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `api_gateway_tenant` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `uuid` varchar(42) NOT NULL,
  `tenant_id` bigint(20) NOT NULL,
  `api_gateway_id` bigint(20) NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `removed_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_uuid___api_gateway_tenant` (`uuid`),
  CONSTRAINT `FK_api_gw_tenant___tenant` FOREIGN KEY (`tenant_id`) REFERENCES `tenant` (`id`),
  CONSTRAINT `FK_api_gw_tenant___api_gateway` FOREIGN KEY (`api_gateway_id`) REFERENCES `api_gateway` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

