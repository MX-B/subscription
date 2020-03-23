ALTER TABLE `gateway` ADD `external_id` varchar(128) NOT NULL;
CREATE UNIQUE INDEX `gateway_external_id_uindex` ON `gateway` (`external_id`);