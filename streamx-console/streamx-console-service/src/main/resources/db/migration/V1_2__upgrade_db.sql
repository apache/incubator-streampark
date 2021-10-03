SET FOREIGN_KEY_CHECKS = 0;

INSERT INTO `t_setting` VALUES (11, 'docker.register.address', null, 'Docker Register Address', 'Docker容器服务地址', 1);
INSERT INTO `t_setting` VALUES (12, 'docker.register.user', null, 'Docker Register User', 'Docker容器服务认证用户名', 1);
INSERT INTO `t_setting` VALUES (13, 'docker.register.password', null, 'Docker Register Password', 'Docker容器服务认证密码', 1);

ALTER TABLE `streamx`.`t_flink_app` ADD COLUMN `CLUSTER_ID` varchar(255) NULL AFTER `JOB_ID`;
ALTER TABLE `streamx`.`t_flink_app` ADD COLUMN `K8S_NAMESPACE` varchar(255) NULL AFTER `CLUSTER_ID`;
ALTER TABLE `streamx`.`t_flink_app` ADD COLUMN `FLINK_IMAGE` varchar(255) NULL AFTER `K8S_NAMESPACE`;
ALTER TABLE `streamx`.`t_flink_app` ADD COLUMN `K8S_REST_EXPOSED_TYPE` tinyint NULL AFTER `RESOLVE_ORDER`;
ALTER TABLE `streamx`.`t_flink_app` ADD COLUMN `K8S_POD_TEMPLATE` text NULL AFTER `ALERT_EMAIL`;
ALTER TABLE `streamx`.`t_flink_app` ADD COLUMN `K8S_JM_POD_TEMPLATE` text NULL AFTER `K8S_POD_TEMPLATE`;
ALTER TABLE `streamx`.`t_flink_app` ADD COLUMN `K8S_TM_POD_TEMPLATE` text NULL AFTER `K8S_JM_POD_TEMPLATE`;


delete from `t_setting` where `NUM` = 4;
update `t_setting` set `NUM`=`NUM`-1 where `NUM` > 3;

update `t_flink_project` set `url`='https://gitee.com/streamxhub/streamx-quickstart.git' where `NAME`='streamx-quickstart';

SET FOREIGN_KEY_CHECKS = 1;

