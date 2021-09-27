SET FOREIGN_KEY_CHECKS = 0;

INSERT INTO `t_setting` VALUES (11, 'docker.register.address', null, 'Docker Register Address', 'Docker容器服务地址', 1);
INSERT INTO `t_setting` VALUES (12, 'docker.register.user', null, 'Docker Register User', 'Docker容器服务认证用户名', 1);
INSERT INTO `t_setting` VALUES (13, 'docker.register.password', null, 'Docker Register Password', 'Docker容器服务认证密码', 1);

ALTER TABLE `streamx`.`t_flink_app`
    ADD COLUMN `CLUSTER_ID` varchar(255) NULL AFTER `JOB_ID`,
    ADD COLUMN `K8S_NAME_SPACE` varchar(255) NULL AFTER `CLUSTER_ID`,
    ADD COLUMN `FLINK_IMAGE` varchar(255) NULL AFTER `K8S_NAMESPACE`;

SET FOREIGN_KEY_CHECKS = 1;
