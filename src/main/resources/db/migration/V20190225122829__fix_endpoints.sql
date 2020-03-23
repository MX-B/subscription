INSERT INTO `api_endpoint` (uuid, name, path, api_id, created_at)
SELECT CONCAT('APE-', UUID()) `uuid`, COALESCE(MAX(pe.name), '') `name`, pe.endpoint `path`, COALESCE(p.api_id, ag.api_id) `api_id`, NOW() `created_at`
FROM `plan_endpoint` pe
INNER JOIN `plan` p ON p.id = pe.plan_id
LEFT JOIN `api_gateway_tenant` agt ON agt.id = p.api_gateway_tenant_id
LEFT JOIN `api_gateway` ag ON ag.id = agt.api_gateway_id
WHERE NOT EXISTS (SELECT ae.id FROM `api_endpoint` ae WHERE ae.path = pe.endpoint AND ae.api_id = COALESCE(p.api_id, ag.api_id))
  AND pe.endpoint IS NOT NULL
GROUP BY `api_id`, `path`;

UPDATE `plan_endpoint` pe
INNER JOIN `plan` plan ON plan.id = pe.plan_id
LEFT JOIN `api_gateway_tenant` agt ON agt.id = plan.api_gateway_tenant_id
LEFT JOIN `api_gateway` ag ON ag.id = agt.api_gateway_id
SET pe.api_endpoint_id = (
  SELECT MAX(ae.id)
  FROM `api_endpoint` ae
  WHERE ae.path = pe.endpoint
    AND ae.api_id = COALESCE(plan.api_id, ag.api_id)
);

