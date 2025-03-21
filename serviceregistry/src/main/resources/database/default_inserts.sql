INSERT IGNORE INTO `service_interface_template` (`name`, `protocol`) VALUES ('generic-http', 'http');

INSERT IGNORE INTO `service_interface_template_property` (`service_interface_template_id`, `property_name`, `mandatory`, `validator`) VALUES (
	(SELECT `id` FROM `service_interface_template` WHERE `name` = 'generic-http'),
	'accessAddresses',
	1,
	'NOT_EMPTY_ADDRESS_LIST'
);

INSERT IGNORE INTO `service_interface_template_property` (`service_interface_template_id`, `property_name`, `mandatory`, `validator`) VALUES (
	(SELECT `id` FROM `service_interface_template` WHERE `name` = 'generic-http'),
	'accessPort',
	1,
	'PORT'
); 

INSERT IGNORE INTO `service_interface_template_property` (`service_interface_template_id`, `property_name`, `mandatory`) VALUES (
	(SELECT `id` FROM `service_interface_template` WHERE `name` = 'generic-http'),
	'basePath',
	1
); 

INSERT IGNORE INTO `service_interface_template_property` (`service_interface_template_id`, `property_name`, `mandatory`, `validator`) VALUES (
	(SELECT `id` FROM `service_interface_template` WHERE `name` = 'generic-http'),
	'operations',
	0,
	'HTTP_OPERATIONS'
); 


INSERT IGNORE INTO `service_interface_template` (`name`, `protocol`) VALUES ('generic-https', 'https');

INSERT IGNORE INTO `service_interface_template_property` (`service_interface_template_id`, `property_name`, `mandatory`, `validator`) VALUES (
	(SELECT `id` FROM `service_interface_template` WHERE `name` = 'generic-https'),
	'accessAddresses',
	1,
	'NOT_EMPTY_ADDRESS_LIST'
);

INSERT IGNORE INTO `service_interface_template_property` (`service_interface_template_id`, `property_name`, `mandatory`, `validator`) VALUES (
	(SELECT `id` FROM `service_interface_template` WHERE `name` = 'generic-https'),
	'accessPort',
	1,
	'PORT'
); 

INSERT IGNORE INTO `service_interface_template_property` (`service_interface_template_id`, `property_name`, `mandatory`) VALUES (
	(SELECT `id` FROM `service_interface_template` WHERE `name` = 'generic-https'),
	'basePath',
	1
); 

INSERT IGNORE INTO `service_interface_template_property` (`service_interface_template_id`, `property_name`, `mandatory`, `validator`) VALUES (
	(SELECT `id` FROM `service_interface_template` WHERE `name` = 'generic-https'),
	'operations',
	0,
	'HTTP_OPERATIONS'
); 

INSERT IGNORE INTO `service_interface_template` (`name`, `protocol`) VALUES ('generic-mqtt', 'tcp');

INSERT IGNORE INTO `service_interface_template_property` (`service_interface_template_id`, `property_name`, `mandatory`, `validator`) VALUES (
	(SELECT `id` FROM `service_interface_template` WHERE `name` = 'generic-mqtt'),
	'accessAddresses',
	1,
	'NOT_EMPTY_ADDRESS_LIST'
);

INSERT IGNORE INTO `service_interface_template_property` (`service_interface_template_id`, `property_name`, `mandatory`, `validator`) VALUES (
	(SELECT `id` FROM `service_interface_template` WHERE `name` = 'generic-mqtt'),
	'accessPort',
	1,
	'PORT'
); 

INSERT IGNORE INTO `service_interface_template_property` (`service_interface_template_id`, `property_name`, `mandatory`) VALUES (
	(SELECT `id` FROM `service_interface_template` WHERE `name` = 'generic-mqtt'),
	'baseTopic',
	1
); 

INSERT IGNORE INTO `service_interface_template_property` (`service_interface_template_id`, `property_name`, `mandatory`, `validator`) VALUES (
	(SELECT `id` FROM `service_interface_template` WHERE `name` = 'generic-mqtt'),
	'operations',
	1,
	'NOT_EMPTY_STRING_SET|NAME'
);

INSERT IGNORE INTO `service_interface_template` (`name`, `protocol`) VALUES ('generic-mqtts', 'ssl');

INSERT IGNORE INTO `service_interface_template_property` (`service_interface_template_id`, `property_name`, `mandatory`, `validator`) VALUES (
	(SELECT `id` FROM `service_interface_template` WHERE `name` = 'generic-mqtts'),
	'accessAddresses',
	1,
	'NOT_EMPTY_ADDRESS_LIST'
);

INSERT IGNORE INTO `service_interface_template_property` (`service_interface_template_id`, `property_name`, `mandatory`, `validator`) VALUES (
	(SELECT `id` FROM `service_interface_template` WHERE `name` = 'generic-mqtts'),
	'accessPort',
	1,
	'PORT'
); 

INSERT IGNORE INTO `service_interface_template_property` (`service_interface_template_id`, `property_name`, `mandatory`) VALUES (
	(SELECT `id` FROM `service_interface_template` WHERE `name` = 'generic-mqtts'),
	'baseTopic',
	1
); 

INSERT IGNORE INTO `service_interface_template_property` (`service_interface_template_id`, `property_name`, `mandatory`, `validator`) VALUES (
	(SELECT `id` FROM `service_interface_template` WHERE `name` = 'generic-mqtts'),
	'operations',
	1,
	'NOT_EMPTY_STRING_SET|NAME'
);