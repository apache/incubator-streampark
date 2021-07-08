SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for t_setting
-- ----------------------------
update `t_setting` set `NUM` = `NUM` + 1 where `NUM` > 6;

update `t_setting` set `KEY` = 'alert.email.sender', DESCRIPTION='用来发送告警邮箱的认证用户名' where `KEY` = 'alert.email.address';

update `t_setting` set `DESCRIPTION`= '用来发送告警邮箱的认证密码' where `KEY` = 'alert.email.password';

INSERT INTO `t_setting` VALUES (7, 'alert.email.from', NULL, 'Alert  Email From', '告警邮箱发送人', 1);

SET FOREIGN_KEY_CHECKS = 1;
