USE `ah_serviceorchestration_dynamic`;

REVOKE ALL, GRANT OPTION FROM 'serviceorchestration-dynamic'@'localhost';

GRANT ALL PRIVILEGES ON `ah_serviceorchestration_dynamic`.`logs` TO 'serviceorchestration-dynamic'@'localhost';
GRANT ALL PRIVILEGES ON `ah_serviceorchestration_dynamic`.`subscription` TO 'serviceorchestration-dynamic'@'localhost';

REVOKE ALL, GRANT OPTION FROM 'serviceorchestration-dynamic'@'%';

GRANT ALL PRIVILEGES ON `ah_serviceorchestration_dynamic`.`logs` TO 'serviceorchestration-dynamic'@'%';
GRANT ALL PRIVILEGES ON `ah_serviceorchestration_dynamic`.`subscription` TO 'serviceorchestration-dynamic'@'%';

FLUSH PRIVILEGES;