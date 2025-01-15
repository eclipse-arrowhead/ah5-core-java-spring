INSERT IGNORE INTO `system_` (`name`, `authentication_method`, `sysop`, `created_by`, `updated_by`) VALUES ('sysop', 'PASSWORD', 1, 'Initialization script', 'Initialization script');

-- Default password: xFNHbh2mQyIp1gmimeTC (In a production environment this password should be changed after first login)
INSERT IGNORE INTO `password_authentication` (`system_id`, `password`) VALUES ((SELECT `id` FROM `system_` WHERE `name` = 'sysop'), '$2a$10$PeLSXNYONS1gjdqWwNqgxuj2fAKRj3sl4hbUspWqLMr0XpBWZnwmK');