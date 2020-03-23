CREATE TABLE `audit` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `register_id` bigint(20) NOT NULL,
  `register_uuid` varchar(42) NOT NULL,
  `created_at` datetime NOT NULL,
  `operation` varchar(40) NOT NULL,
  `register_content` TEXT NOT NULL,
  `user_keycloak_id` varchar(1000) NOT NULL,
  `user_info` varchar(1000) NOT NULL,
  `register_name` varchar(42) NOT NULL,
  `token_issued_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;