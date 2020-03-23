ALTER TABLE `partner` RENAME `provider`;
ALTER TABLE `api` CHANGE `partner_id` `provider_id` bigint(20) NOT NULL;