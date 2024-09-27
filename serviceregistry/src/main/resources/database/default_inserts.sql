INSERT IGNORE INTO `service_interface_template` (`name`, `protocol`) VALUES ('GENERIC_HTTP', 'http');

INSERT IGNORE INTO `service_interface_template_property` (`service_interface_template_id`, `property_name`, `mandatory`, `validator`) VALUES (
	(SELECT `id` FROM `service_interface_template` WHERE `name` = 'GENERIC_HTTP'),
	'accessAddresses',
	1,
	'NOT_EMPTY_ADDRESS_LIST'
);

INSERT IGNORE INTO `service_interface_template_property` (`service_interface_template_id`, `property_name`, `mandatory`, `validator`) VALUES (
	(SELECT `id` FROM `service_interface_template` WHERE `name` = 'GENERIC_HTTP'),
	'accessPort',
	1,
	'PORT'
); 

INSERT IGNORE INTO `service_interface_template_property` (`service_interface_template_id`, `property_name`, `mandatory`) VALUES (
	(SELECT `id` FROM `service_interface_template` WHERE `name` = 'GENERIC_HTTP'),
	'basePath',
	1
); 

INSERT IGNORE INTO `service_interface_template_property` (`service_interface_template_id`, `property_name`, `mandatory`, `validator`) VALUES (
	(SELECT `id` FROM `service_interface_template` WHERE `name` = 'GENERIC_HTTP'),
	'operations',
	0,
	'HTTP_OPERATIONS'
); 


INSERT IGNORE INTO `service_interface_template` (`name`, `protocol`) VALUES ('GENERIC_HTTPS', 'https');

INSERT IGNORE INTO `service_interface_template_property` (`service_interface_template_id`, `property_name`, `mandatory`, `validator`) VALUES (
	(SELECT `id` FROM `service_interface_template` WHERE `name` = 'GENERIC_HTTPS'),
	'accessAddresses',
	1,
	'NOT_EMPTY_ADDRESS_LIST'
);

INSERT IGNORE INTO `service_interface_template_property` (`service_interface_template_id`, `property_name`, `mandatory`, `validator`) VALUES (
	(SELECT `id` FROM `service_interface_template` WHERE `name` = 'GENERIC_HTTPS'),
	'accessPort',
	1,
	'PORT'
); 

INSERT IGNORE INTO `service_interface_template_property` (`service_interface_template_id`, `property_name`, `mandatory`) VALUES (
	(SELECT `id` FROM `service_interface_template` WHERE `name` = 'GENERIC_HTTPS'),
	'basePath',
	1
); 

INSERT IGNORE INTO `service_interface_template_property` (`service_interface_template_id`, `property_name`, `mandatory`, `validator`) VALUES (
	(SELECT `id` FROM `service_interface_template` WHERE `name` = 'GENERIC_HTTPS'),
	'operations',
	0,
	'HTTP_OPERATIONS'
); 