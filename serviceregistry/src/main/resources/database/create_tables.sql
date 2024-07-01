USE `ah_serviceregistry`;

CREATE TABLE IF NOT EXISTS `test_table` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `valami` varchar(255),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
  