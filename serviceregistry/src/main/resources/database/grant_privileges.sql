USE `ah_serviceregistry`;

REVOKE ALL, GRANT OPTION FROM 'serviceregistry'@'localhost';

GRANT ALL PRIVILEGES ON `ah_serviceregistry`.`logs` TO 'serviceregistry'@'localhost';

REVOKE ALL, GRANT OPTION FROM 'serviceregistry'@'%';

GRANT ALL PRIVILEGES ON `ah_serviceregistry`.`logs` TO 'serviceregistry'@'%';

FLUSH PRIVILEGES;