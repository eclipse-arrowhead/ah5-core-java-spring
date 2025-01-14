USE `ah_authentication`;

REVOKE ALL, GRANT OPTION FROM 'authentication'@'localhost';

GRANT ALL PRIVILEGES ON `ah_authentication`.`logs` TO 'authentication'@'localhost';

REVOKE ALL, GRANT OPTION FROM 'authentication'@'%';

GRANT ALL PRIVILEGES ON `ah_authentication`.`logs` TO 'authentication'@'%';

FLUSH PRIVILEGES;