CREATE TABLE `method_type`
(
  `id`   bigint(20)  NOT NULL AUTO_INCREMENT,
  `name` varchar(64) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_uuid___plan_type` (`name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;

INSERT INTO `method_type` (`id`, `name`) VALUES (1, 'GET');
INSERT INTO `method_type` (`id`, `name`) VALUES (2, 'POST');
INSERT INTO `method_type` (`id`, `name`) VALUES (3, 'PUT');
INSERT INTO `method_type` (`id`, `name`) VALUES (4, 'DELETE');
INSERT INTO `method_type` (`id`, `name`) VALUES (5, 'HEAD');
INSERT INTO `method_type` (`id`, `name`) VALUES (6, 'OPTIONS');
INSERT INTO `method_type` (`id`, `name`) VALUES (7, 'PATCH');
INSERT INTO `method_type` (`id`, `name`) VALUES (8, 'TRACE');
INSERT INTO `method_type` (`id`, `name`) VALUES (9, 'CONNECT');

CREATE TABLE `api_endpoint`
(
  `id`             bigint(20)   NOT NULL AUTO_INCREMENT,
  `uuid`           varchar(42)  NOT NULL,
  `name`           varchar(256) NOT NULL,
  `path`           varchar(256) NOT NULL,
  `method_type_id` bigint(20)   NOT NULL DEFAULT 1,
  `api_id`         bigint(20)   NOT NULL,

  `created_at`     datetime     NOT NULL,
  `updated_at`     datetime DEFAULT NULL,
  `removed_at`     datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_uuid___api_endpoint` (`uuid`),
  UNIQUE KEY `UK_name_path___api_endpoint` (`name`,`path`, `removed_at`),
  CONSTRAINT `FK_api_endpoint___api` FOREIGN KEY (`api_id`) REFERENCES `api` (`id`),
  CONSTRAINT `FK_api_endpoint___method_type` FOREIGN KEY (`method_type_id`) REFERENCES `method_type` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

ALTER TABLE `plan_endpoint`
  ADD `api_endpoint_id` bigint(20) NULL;

ALTER TABLE `plan_endpoint`
  ADD CONSTRAINT `fk__plan_endpoint__api_endpoint` FOREIGN KEY (`api_endpoint_id`) REFERENCES `api_endpoint` (`id`);

INSERT INTO `api_endpoint` (uuid, name, path, api_id, created_at)
SELECT CONCAT('APE-', UUID()) `uuid`, MAX(pe.name) `name`, pe.endpoint `path`, ag.api_id `api_id`, NOW() `created_at`
FROM `plan_endpoint` pe
INNER JOIN `plan` p ON p.id = pe.plan_id
INNER JOIN `api_gateway_tenant` agt ON agt.id = p.api_gateway_tenant_id
INNER JOIN `api_gateway` ag ON ag.id = agt.api_gateway_id
GROUP BY `api_id`, `path`;

UPDATE `plan_endpoint` pe
INNER JOIN `plan` plan ON plan.id = pe.plan_id
LEFT JOIN `api_gateway_tenant` agt ON agt.id = plan.api_gateway_tenant_id
LEFT JOIN `api_gateway` ag ON ag.id = agt.api_gateway_id
SET pe.api_endpoint_id = (
  SELECT ae.id
  FROM `api_endpoint` ae
  WHERE ae.name = pe.name
    AND ae.path = pe.endpoint
    AND ae.api_id = COALESCE(plan.api_id, ag.api_id)
);

ALTER TABLE `plan_endpoint` MODIFY `endpoint` VARCHAR(256) NULL;
