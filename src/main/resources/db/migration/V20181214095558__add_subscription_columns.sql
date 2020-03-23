ALTER TABLE `subscription` ADD `subscription_start` datetime DEFAULT NULL;
ALTER TABLE `subscription` ADD `subscription_end` datetime DEFAULT NULL;

ALTER TABLE `subscription` ADD `next_subscription_id` bigint(20) NULL;

ALTER TABLE `subscription` ADD
CONSTRAINT `fk__subscription_next`
FOREIGN KEY (`next_subscription_id`)
REFERENCES `subscription` (`id`)