INSERT IGNORE INTO `service_interface_template` (`name`, `protocol`) VALUES ('generic_http', 'http');

INSERT IGNORE INTO `service_interface_template_property` (`service_interface_template_id`, `property_name`, `mandatory`, `validator`) VALUES (
	(SELECT `id` FROM `service_interface_template` WHERE `name` = 'generic_http'),
	'accessAddresses',
	1,
	'NOT_EMPTY_ADDRESS_LIST'
);

INSERT IGNORE INTO `service_interface_template_property` (`service_interface_template_id`, `property_name`, `mandatory`, `validator`) VALUES (
	(SELECT `id` FROM `service_interface_template` WHERE `name` = 'generic_http'),
	'accessPort',
	1,
	'PORT'
); 

INSERT IGNORE INTO `service_interface_template_property` (`service_interface_template_id`, `property_name`, `mandatory`) VALUES (
	(SELECT `id` FROM `service_interface_template` WHERE `name` = 'generic_http'),
	'basePath',
	1
); 

INSERT IGNORE INTO `service_interface_template_property` (`service_interface_template_id`, `property_name`, `mandatory`, `validator`) VALUES (
	(SELECT `id` FROM `service_interface_template` WHERE `name` = 'generic_http'),
	'operations',
	0,
	'HTTP_OPERATIONS'
); 


INSERT IGNORE INTO `service_interface_template` (`name`, `protocol`) VALUES ('generic_https', 'https');

INSERT IGNORE INTO `service_interface_template_property` (`service_interface_template_id`, `property_name`, `mandatory`, `validator`) VALUES (
	(SELECT `id` FROM `service_interface_template` WHERE `name` = 'generic_https'),
	'accessAddresses',
	1,
	'NOT_EMPTY_ADDRESS_LIST'
);

INSERT IGNORE INTO `service_interface_template_property` (`service_interface_template_id`, `property_name`, `mandatory`, `validator`) VALUES (
	(SELECT `id` FROM `service_interface_template` WHERE `name` = 'generic_https'),
	'accessPort',
	1,
	'PORT'
); 

INSERT IGNORE INTO `service_interface_template_property` (`service_interface_template_id`, `property_name`, `mandatory`) VALUES (
	(SELECT `id` FROM `service_interface_template` WHERE `name` = 'generic_https'),
	'basePath',
	1
); 

INSERT IGNORE INTO `service_interface_template_property` (`service_interface_template_id`, `property_name`, `mandatory`, `validator`) VALUES (
	(SELECT `id` FROM `service_interface_template` WHERE `name` = 'generic_https'),
	'operations',
	0,
	'HTTP_OPERATIONS'
); 

INSERT IGNORE INTO `service_interface_template` (`name`, `protocol`) VALUES ('generic_mqtt', 'tcp');

INSERT IGNORE INTO `service_interface_template_property` (`service_interface_template_id`, `property_name`, `mandatory`, `validator`) VALUES (
	(SELECT `id` FROM `service_interface_template` WHERE `name` = 'generic_mqtt'),
	'accessAddresses',
	1,
	'NOT_EMPTY_ADDRESS_LIST'
);

INSERT IGNORE INTO `service_interface_template_property` (`service_interface_template_id`, `property_name`, `mandatory`, `validator`) VALUES (
	(SELECT `id` FROM `service_interface_template` WHERE `name` = 'generic_mqtt'),
	'accessPort',
	1,
	'PORT'
); 

INSERT IGNORE INTO `service_interface_template_property` (`service_interface_template_id`, `property_name`, `mandatory`) VALUES (
	(SELECT `id` FROM `service_interface_template` WHERE `name` = 'generic_mqtt'),
	'baseTopic',
	1
); 

INSERT IGNORE INTO `service_interface_template_property` (`service_interface_template_id`, `property_name`, `mandatory`, `validator`) VALUES (
	(SELECT `id` FROM `service_interface_template` WHERE `name` = 'generic_mqtt'),
	'operations',
	1,
	'NOT_EMPTY_STRING_SET|OPERATION'
);

INSERT IGNORE INTO `service_interface_template` (`name`, `protocol`) VALUES ('generic_mqtts', 'ssl');

INSERT IGNORE INTO `service_interface_template_property` (`service_interface_template_id`, `property_name`, `mandatory`, `validator`) VALUES (
	(SELECT `id` FROM `service_interface_template` WHERE `name` = 'generic_mqtts'),
	'accessAddresses',
	1,
	'NOT_EMPTY_ADDRESS_LIST'
);

INSERT IGNORE INTO `service_interface_template_property` (`service_interface_template_id`, `property_name`, `mandatory`, `validator`) VALUES (
	(SELECT `id` FROM `service_interface_template` WHERE `name` = 'generic_mqtts'),
	'accessPort',
	1,
	'PORT'
); 

INSERT IGNORE INTO `service_interface_template_property` (`service_interface_template_id`, `property_name`, `mandatory`) VALUES (
	(SELECT `id` FROM `service_interface_template` WHERE `name` = 'generic_mqtts'),
	'baseTopic',
	1
); 

INSERT IGNORE INTO `service_interface_template_property` (`service_interface_template_id`, `property_name`, `mandatory`, `validator`) VALUES (
	(SELECT `id` FROM `service_interface_template` WHERE `name` = 'generic_mqtts'),
	'operations',
	1,
	'NOT_EMPTY_STRING_SET|OPERATION'
);