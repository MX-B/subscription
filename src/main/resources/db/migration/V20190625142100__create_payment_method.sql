CREATE TABLE `payment_method` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(64) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_uuid___payment_method` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `payment_method` (`id`, `name`) VALUES (1, 'CREDIT_CARD');
INSERT INTO `payment_method` (`id`, `name`) VALUES (2, 'INVOICE');