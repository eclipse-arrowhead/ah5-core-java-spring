USE `ah_serviceregistry`;

REVOKE ALL, GRANT OPTION FROM 'serviceregistry'@'localhost';

GRANT ALL PRIVILEGES ON `ah_serviceregistry`.`test_table` TO 'serviceregistry'@'localhost';

REVOKE ALL, GRANT OPTION FROM 'serviceregistry'@'%';

GRANT ALL PRIVILEGES ON `ah_serviceregistry`.`test_table` TO 'serviceregistry'@'%';

FLUSH PRIVILEGES;