/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.streampark.console.base.enums;

import javax.validation.constraints.NotNull;

public enum I18nResourceEnum {
  /** alert message */
  ALERT_IS_BOUND_BY_APP_PLEASE_CLEAR_THE_CONFIG_FIRST("exception.message.10010000"),
  FAILED_SEND_DINGTALK_ALERT("exception.message.10020100"),
  FAILED_TO_REQUEST_DINGTALK_ROBOT_ALARM_URL("exception.message.10020102"),
  FAILED_TO_REQUEST_DINGTALK_ROBOT_ALERT_URL("exception.message.10020103"),
  FAILED_TO_REQUEST_DINGTALK_ROBOT_ALERT_URL_ERROR_CODE("exception.message.10020104"),
  FAILED_SEND_LARK_ALERT("exception.message.10030000"),
  FAILED_TO_REQUEST_LARK_ROBOT_ALERT("exception.message.10030101"),
  FAILED_TO_REQUEST_WECOM_ROBOT_ALERT("exception.message.10040100"),
  FAILED_SEND_WECOM_ALERT("exception.message.10040000"),
  FAILED_SEND_EMAIL_ALERT("exception.message.10050000"),
  PLEASE_CONFIGURE_THE_EMAIL_SENDER_FIRST("exception.message.10050100"),
  FAILED_SEND_HTTPCALLBACK_ALERT("exception.message.10060000"),
  PLEASE_CONFIGURE_A_VALID_CONTACTS("exception.message.10070000"),
  CALCULATE_THE_SIGNATURE_FAILED("exception.message.10080000"),

  /** Variable */
  VARIABLE_CODE_ALREADY_EXISTS("exception.message.20010100"),
  VARIABLE_IS_ACTUALLY_USED("exception.message.20010101"),
  THE_VARIABLE_ID_CANNOT_BE_NULL("exception.message.20010102"),
  THE_VARIABLE_CODE_CANNOT_BE_UPDATED("exception.message.20010103"),
  THE_VARIABLE_DOES_NOT_EXIST("exception.message.20010104"),
  PLEASE_DELETE_THE_VARIABLE_UNDER_THE_TEAM("exception.message.20020100"),

  /** User */
  USER_DOES_NOT_EXIST("exception.message.30010100"),
  USER_CAN_NOT_LOGIN_WITH_PWD("exception.message.30010101"),
  USER_CAN_ONLY_SIGN_IN_WITH("exception.message.30010102"),
  THE_USER_HAS_BEEN_ADDED("exception.message.30010103"),
  CURRENT_LOGIN_USER_IS_NULL("exception.message.30010104"),
  USER_IS_NULL_UPDATE_PWD_FAILED("exception.message.30010105"),
  THE_CURRENT_USER_DOES_NOT_BELONG_TO_ANY_TEAM("exception.message.30010106"),
  USER_ID_CANNOT_BE_CHANGED("exception.message.30020100"),
  THE_USER_ID_NOT_FOUND("exception.message.30020101"),
  THE_USERNAME_NOT_FOUND("exception.message.30030100"),
  UPDATE_PWD_NEED_SIGN_IN_PWD("exception.message.30040100"),
  OLD_PWD_ERROR_UPDATE_FAILED("exception.message.30040101"),
  INCORRECT_PWD("exception.message.30040102"),
  THE_LOGIN_TYPE_IS_NOT_SUPPORTED("exception.message.30050100"),
  SSO_IS_NOT_AVAILABLE("exception.message.30050101"),
  PERMISSION_DENIED_PLEASE_LOGIN_FIRST("exception.message.30060100"),
  PERMISSION_DENIED_ONLY_USER_HIMSELF_CAN_ACCESS_THIS_PERMISSION("exception.message.30060101"),
  PERMISSION_DENIED_ONLY_USER_BELONGS_TO_THIS_TEAM_CAN_ACCESS_THIS_PERMISSION(
      "exception.message.30060002"),

  /** Team */
  THE_TEAMID_CAN_NOT_BE_NULL("exception.message.40010100"),
  TEAMID_CAN_NOT_BE_CHANGED("exception.message.40010101"),
  TEAMID_OF_YARN_QUEUE_QUERY_PARAM_MUST_NOT_BE_NULL("exception.message.40010102"),
  THE_TEAM_ID_NOT_FOUND("exception.message.40010103"),
  THE_TEAM_ID_IS_REQUIRED("exception.message.40010104"),
  TEAM_ID_CANNOT_BE_CHANGED("exception.message.40010105"),
  THE_TEAM_DOES_NOT_EXIST("exception.message.40020106"),
  TEAM_NAME_EXISTS_ALREADY("exception.message.40020100"),
  TEAM_NAME_CAN_NOT_BE_CHANGED("exception.message.40020101"),

  /** Project */
  PROJECT_NAME_ALREADY_EXISTS_ADD_FAILED("exception.message.50010100"),
  THE_PROJECT_IS_BEING_BUILT_UPDATE_FAILED("exception.message.50020100"),
  PROJECT_MODULE_CAN_NOT_BE_NULL("exception.message.50030100"),
  PLEASE_DELETE_THE_PROJECT_UNDER_THE_TEAM_FIRST("exception.message.50040100"),

  /** Application */
  APPLICATION_ID_NOT_FOUND_REVOKE_FAILED("exception.message.60010100"),
  APPLICATION_ID_CAN_NOT_BE_FOUND("exception.message.60010101"),
  PYFLINK_FILE_CAN_NOT_BE_NULL_START_APP_FAILED("exception.message.60020100"),
  PYFLINK_FORMAT_ERROR("exception.message.60020101"),
  CREATE_APPLICATION_FAILED("exception.message.60030100"),
  INVALID_OPERATION_APP_IS_NULL("exception.message.60030101"),
  CREATE_APPLICATION_FROM_COPY_FAILED("exception.message.60030102"),
  PLEASE_DELETE_APP_UNDER_TEAM_FIRST("exception.message.60030103"),
  EXECUTIONMODE_CAN_NOT_BE_NULL_START_APP_FAILED("exception.message.60040100"),
  SOME_APP_IS_RUNNING_ON_THIS_CLUSTER_THE_CLUSTER_CAN_NOT_BE_SHUTDOWN("exception.message.60050100"),
  APP_CONFIG_ERROR("exception.message.60050101"),
  APP_NAME_CAN_NOT_BE_REPEATED_COPY_APP_FAILED("exception.message.60070100"),
  THE_JOB_IS_INVALID_OR_THE_JOB_CAN_NOT_BUILT("exception.message.60080100"),
  EXECUTIONMODE_MUST_BE_K8S("exception.message.60080101"),

  /** Flink Cluster */
  THE_FLINK_HOME_IS_SET_AS_DEFAULT("exception.message.70010100"),
  THE_FLINK_HOME_DOES_NOT_EXIST("exception.message.70010101"),
  THE_FLINK_HOME_IS_STILL_IN_USE_BY_FLINK_CLUSTER("exception.message.70010102"),
  THE_FLINK_HOME_IS_STILL_IN_USE_BY_SOME_APP("exception.message.70010103"),
  THE_FLINK_CLUSTER_DO_NOT_EXIST("exception.message.70020100"),
  THE_FLINK_CLUSTER_NOT_RUNNING("exception.message.70020101"),
  FLINK_CLUSTER_NOT_EXIST("exception.message.70020102"),
  FLINK_CLUSTER_IS_RUNNING_CAN_NOT_BE_DELETE("exception.message.70020103"),
  SHUTDOWN_CLUSTER_FAILED("exception.message.70020104"),
  DEPLOY_CLUSTER_FAILED_UNKNOWN_REASON("exception.message.70020105"),
  THE_TARGET_CLUSTER_IS_UNAVAILABLE("exception.message.70020106"),
  CLUSTER_SHOULD_NOT_BE_SHUTDOWN_DUE_TO_THE_PRESENCE_OF_ACTIVE_JOBS("exception.message.70020107"),
  CURRENT_CLUSTER_IS_NOT_ACTIVE("exception.message.70020108"),
  SOME_APP_ON_CLUSTER_CAN_NOT_BE_DELETED("exception.message.70020109"),
  BIND_FLINK_CLUSTER_NOT_RUNNING_UPDATE_FAILED("exception.message.70020110"),
  THE_CLUSTER_ID_CAN_NOT_BE_FIND("exception.message.70030100"),
  THE_CLUSTER_ID_CAN_NOT_BE_EMPTY("exception.message.70030101"),
  FLINK_SQL_IS_NULL_UPDATE_FLINK_SQL_JOB_FAILED("exception.message.70040100"),
  CAN_NOT_FOUNT_STREAMPARK_FLINK_SQLCLIENT_JAR("exception.message.70040101"),
  FOUND_MULTIPLE_STREAMPARK_FLINK_SQLCLIENT_JAR("exception.message.70040102"),
  UNSUPPORTED_FLINK_VERSION("exception.message.70050100"),
  CHECK_FLINK_ENV_FAILED("exception.message.70050101"),
  THE_FLINK_CONNECTOR_IS_NULL("exception.message.70060100"),
  PLEASE_UPLOAD_JAR_FOR_FLINK_APP_RESOURCE("exception.message.70070100"),
  FLINK_APP_JAR_MUST_EXIST("exception.message.70070101"),
  THE_YARN_SESSION_CLUSTER_ID_CAN_NOT_FOUND("exception.message.70080100"),
  FLINK_EXECUTIONMODE_ERROR("exception.message.70090100"),

  /** resource */
  THE_RESOURCE_NAME_IS_REQUIRED("exception.message.80010100"),
  PLEASE_MAKE_SURE_THE_RESOURCE_NAME_IS_NOT_CHANGED("exception.message.80010101"),
  THE_RESOURCE_ALREADY_EXISTS("exception.message.80020100"),
  THE_RESOURCE_ERROR("exception.message.80020101"),
  THE_RESOURCE_IS_USED_CANNOT_REMOVE("exception.message.80020102"),
  PARSE_RESOURCE_GROUP_FAILED("exception.message.80020200"),

  /** gateway */
  GATEWAY_NAME_ALREADY_EXISTS("exception.message.90010100"),
  GET_GATEWAY_VERSION_FAILED("exception.message.90020100"),

  /** savepoint */
  ERROR_IN_GETTING_SAVEPOINT("exception.message.100010100"),

  /** yarn queue */
  YARN_QUEUE_MUST_NOT_BE_EMPTY("exception.message.110010100"),
  THE_QUEUE_DOES_NOT_EXIST("exception.message.110010101"),
  THE_QUEUE_LABEL_IS("exception.message.110020100"),
  YARN_QUEUE_QUERY_PARAMS_MUST_NOT_BE_NULL("exception.message.110030100"),

  /** ldap */
  LDAP_IS_NOT_ENABLED("exception.message.120010100"),
  INVALID_LDAP_CREDENTIALS_OR_LDAP_SEARCH_ERROR("exception.message.120020100"),

  /** role */
  THE_ROLE_ID_NOT_FOUND("exception.message.130010100"),
  ROLE_ID_NOT_FOUND_DELETE_FAILED("exception.message.130010101"),
  UNBIND_USER_FIRST_DELETE_FAILED("exception.message.130020100"),

  /** Member */
  THE_MEMBER_NOT_FOUND("exception.message.140010100"),

  /** Common */
  FILE_TO_UPLOAD_CAN_NOT_BE_NULL_UPLOAD_FAILED("exception.message.150010100"),
  ILLEGAL_FILE_TYPE("exception.message.150010101"),
  MISSING_FILE("exception.message.150010102"),
  PLEASE_DO_NOT_ADD_MULTI_DEPENDENCY_AT_ONE_TIME("exception.message.150020100"),
  PLEASE_ADD_POM_OR_JAR_RESOURCE("exception.message.150020101"),
  PLEASE_CONFIGURE_THE_CORRECT_PRINCIPAL_NAME_ATTRIBUTE("exception.message.150030100"),
  SOMETHING_NOT_EXISTS("exception.message.150030101"),
  GET_SHUTDOWN_RESPONSE_FAILED("exception.message.150040100"),
  WRONG_USE_OF_ANNOTATION_ON_METHOD("exception.message.150040101"),
  API_ACCESS_TOKEN_AUTHENTICATION_FAILED("exception.message.150040102"),
  SCALA_VERSION_NOT_MATCH("exception.message.150050100");

  private final String name;

  I18nResourceEnum(@NotNull String name) {
    this.name = name;
  }

  @NotNull
  public String getName() {
    return name;
  }
}
