SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for t_flink_env
-- ----------------------------
DROP TABLE IF EXISTS `t_flink_env`;
CREATE TABLE `t_flink_env` (
`ID` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
`FLINK_NAME` varchar(255) NOT NULL COMMENT 'Flink实例名称',
`FLINK_HOME` varchar(255) NOT NULL COMMENT 'Flink Home路径',
`VERSION` varchar(50) NOT NULL COMMENT 'Flink对应的版本号',
`SCALA_VERSION` varchar(50) NOT NULL COMMENT 'Flink对应的scala版本号',
`FLINK_CONF` text NOT NULL COMMENT 'flink-conf配置内容',
`IS_DEFAULT` tinyint NOT NULL DEFAULT '0' COMMENT '是否为默认版本',
`DESCRIPTION` varchar(255) DEFAULT NULL COMMENT '描述信息',
`CREATE_TIME` datetime NOT NULL COMMENT '创建时间',
PRIMARY KEY (`ID`) USING BTREE,
UNIQUE KEY `UN_NAME` (`FLINK_NAME`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;


INSERT INTO `t_setting` VALUES (11, 'docker.register.address', null, 'Docker Register Address', 'Docker容器服务地址', 1);
INSERT INTO `t_setting` VALUES (12, 'docker.register.user', null, 'Docker Register User', 'Docker容器服务认证用户名', 1);
INSERT INTO `t_setting` VALUES (13, 'docker.register.password', null, 'Docker Register Password', 'Docker容器服务认证密码', 1);
INSERT INTO `t_setting` VALUES (14, 'alert.phone.host',null,'Alert Phone Host','用来发送告警服务中心地址',1);
INSERT INTO `t_setting` VALUES (15, 'alert.phone.number',null,'Alert Phone Number','用来发送告警短信号码',1);

ALTER TABLE `t_flink_app` ADD COLUMN `NAME` varchar(255) NULL AFTER `JOB_NAME`;
ALTER TABLE `t_flink_app` ADD COLUMN `VERSION_ID` bigint NULL AFTER `JOB_ID`;
ALTER TABLE `t_flink_app` ADD COLUMN `CLUSTER_ID` varchar(255) NULL AFTER `VERSION_ID`;
ALTER TABLE `t_flink_app` ADD COLUMN `K8S_NAMESPACE` varchar(255) NULL AFTER `CLUSTER_ID`;
ALTER TABLE `t_flink_app` ADD COLUMN `FLINK_IMAGE` varchar(255) NULL AFTER `K8S_NAMESPACE`;
ALTER TABLE `t_flink_app` ADD COLUMN `K8S_REST_EXPOSED_TYPE` tinyint NULL AFTER `RESOLVE_ORDER`;
ALTER TABLE `t_flink_app` ADD COLUMN `K8S_POD_TEMPLATE` text NULL AFTER `ALERT_EMAIL`;
ALTER TABLE `t_flink_app` ADD COLUMN `K8S_JM_POD_TEMPLATE` text NULL AFTER `K8S_POD_TEMPLATE`;
ALTER TABLE `t_flink_app` ADD COLUMN `K8S_TM_POD_TEMPLATE` text NULL AFTER `K8S_JM_POD_TEMPLATE`;
ALTER TABLE `t_flink_app` ADD COLUMN `ALERT_PHONE_NUMBER` varchar(255) NULL AFTER `ALERT_PHONE_NUMBER`;

delete from `t_setting` where `NUM` = 1;
delete from `t_setting` where `NUM` = 4;

update `t_setting` set `NUM`= case when `NUM` > 4 then `NUM` - 2 else `NUM` - 1 end;

update `t_flink_project` set `url`='https://gitee.com/streamxhub/streamx-quickstart.git' where `NAME`='streamx-quickstart';

SET FOREIGN_KEY_CHECKS = 1;

