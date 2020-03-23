CREATE TABLE `provider_type` (
 `id` INTEGER(3) NOT NULL AUTO_INCREMENT,
 `name` VARCHAR(32) NOT NULL,

  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_name___provider_type` (`name`)
);

ALTER TABLE `provider` ADD `type_id` INTEGER(3) NULL DEFAULT 2 AFTER `name`;
ALTER TABLE `provider` MODIFY `wallet_id` VARCHAR(64) NULL;

INSERT INTO `provider_type` (`id`, `name`) VALUES (1, 'FREE');
INSERT INTO `provider_type` (`id`, `name`) VALUES (2, 'PAID');

ALTER TABLE `provider`
ADD CONSTRAINT `FK_provider___provider_type`
FOREIGN KEY (`type_id`) REFERENCES `provider_type` (`id`);