CREATE TABLE `plan_type` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(64) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_uuid___plan_type` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `plan_type` (`id`, `name`) VALUES (1, 'BUY');
INSERT INTO `plan_type` (`id`, `name`) VALUES (2, 'SELL');

ALTER TABLE `plan` CHANGE `api_gateway_tenant_id` `api_gateway_tenant_id` bigint(20) NULL;
ALTER TABLE `plan` ADD `api_id` bigint(20) NULL AFTER `api_gateway_tenant_id`;
ALTER TABLE `plan` ADD `type_id` bigint(20) NULL DEFAULT 2 AFTER `modality_id`;

ALTER TABLE `plan` ADD CONSTRAINT `FK_plan___api` FOREIGN KEY (`api_id`) REFERENCES `api` (`id`);
ALTER TABLE `plan` ADD CONSTRAINT `FK_plan___type` FOREIGN KEY (`type_id`) REFERENCES `plan_type` (`id`);

CREATE TABLE `split_mode` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(64) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_uuid___split_mode` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `split_mode` (`id`, `name`) VALUES (1, 'AUTO');
INSERT INTO `split_mode` (`id`, `name`) VALUES (2, 'MANUAL');

ALTER TABLE `api` ADD `split_mode_id` bigint(20) NULL DEFAULT 2 AFTER `name`;
ALTER TABLE `api` ADD `plan_id` bigint(20) NULL AFTER `provider_id`;

ALTER TABLE `api` ADD CONSTRAINT `FK_api___plan` FOREIGN KEY (`plan_id`) REFERENCES `plan` (`id`);
ALTER TABLE `api` ADD CONSTRAINT `FK_api___split_mode` FOREIGN KEY (`split_mode_id`) REFERENCES `split_mode` (`id`);