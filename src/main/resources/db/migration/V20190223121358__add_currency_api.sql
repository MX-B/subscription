ALTER TABLE `api` ADD `currency_id` BIGINT(20) NULL;
ALTER TABLE `api` ADD CONSTRAINT `FK_api___currency` FOREIGN KEY (`currency_id`) REFERENCES `currency` (`id`);