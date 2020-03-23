CREATE TABLE `api_type` (
    `id` INTEGER(3) NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(32) NOT NULL,

     PRIMARY KEY (`id`),
     UNIQUE KEY `UK_name___api_type` (`name`)
);

ALTER TABLE `api_gateway_tenant` ADD `type_id` INTEGER(3) NULL;

ALTER TABLE `api_gateway_tenant`
ADD CONSTRAINT `FK_api_gtw_tenant___api_type`
FOREIGN KEY (`type_id`) REFERENCES `api_type` (`id`);

INSERT INTO `api_type` (`id`, `name`) VALUES (1, 'FREE');
INSERT INTO `api_type` (`id`, `name`) VALUES (2, 'FREEMIUM');
INSERT INTO `api_type` (`id`, `name`) VALUES (3, 'PAID');

ALTER TABLE `plan_endpoint` ADD `name` varchar(64) NULL;
ALTER TABLE `plan` ADD `pending_sync` bit(1) DEFAULT 1 NOT NULL;