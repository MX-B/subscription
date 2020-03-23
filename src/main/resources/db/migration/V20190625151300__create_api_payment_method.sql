CREATE TABLE `api_payment_method` (
  `api_gateway_tenant_id` bigint(20) NOT NULL,
  `payment_method_id` bigint(20) NOT NULL,
  PRIMARY KEY (`api_gateway_tenant_id`, `payment_method_id`),
  CONSTRAINT `FK_api_gateway_tenant_id` FOREIGN KEY (`api_gateway_tenant_id`) REFERENCES `api_gateway_tenant`(`id`),
  CONSTRAINT `FK_payment_method_id` FOREIGN KEY (`payment_method_id`) REFERENCES `payment_method`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;