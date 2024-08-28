export enum SparkEnvCheckEnum {
  INVALID_PATH = -1,
  OK = 0,
  NAME_REPEATED = 1,
  SPARK_DIST_NOT_FOUND = 2,
  SPARK_DIST_REPEATED = 3,
}

export enum JobTypeEnum {
  JAR = 1,
  SQL = 2,
  PYSPARK = 3,
}

/* ExecutionMode  */
export enum ExecModeEnum {
  /** remote (standalone) */
  REMOTE = 1,
  /** yarn per-job (deprecated, please use yarn-application mode) */
  YARN_CLUSTER = 2,
  /** yarn session */
  YARN_CLIENT = 3,
}

export enum AppExistsStateEnum {
  /** no exists */
  NO,

  /** exists in database */
  IN_DB,

  /** exists in yarn */
  IN_YARN,

  /** exists in remote kubernetes cluster. */
  IN_KUBERNETES,

  /** job name invalid because of special utf-8 character */
  INVALID,
}
