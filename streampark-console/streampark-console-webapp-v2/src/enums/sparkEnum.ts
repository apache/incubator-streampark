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

export enum OperationEnum {
  RELEASE,
  START,
  STOP,
}
export enum OptionStateEnum {
  /** Application which is currently action: none. */
  NONE = 0,
  /** Application which is currently action: deploying. */
  RELEASING = 1,
  /** Application which is currently action: starting. */
  STARTING = 2,
  /** Application which is currently action: stopping. */
  STOPPING = 3,
}

/* DeployMode  */
export enum DeployMode {
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

export enum AppStateEnum {
  /** Added new job to database. */
  ADDED = 0,

  /** (From Yarn)Application which was just created. */
  NEW = 1,

  /** (From Yarn)Application which is being saved. */
  NEW_SAVING = 2,

  /** Application which is currently running. */
  STARTING = 3,

  /** (From Yarn)Application which has been submitted. */
  SUBMITTED = 4,

  /** (From Yarn)Application has been accepted by the scheduler. */
  ACCEPTED = 5,

  /** The job has failed and is currently waiting for the cleanup to complete. */
  RUNNING = 6,

  /** (From Yarn)Application which finished successfully. */
  FINISHED = 7,

  /** (From Yarn)Application which failed. */
  FAILED = 8,

  /** Loss of mapping. */
  LOST = 9,

  /** Mapping. */
  MAPPING = 10,

  /** Other statuses. */
  OTHER = 11,

  /** Has rollback. */
  REVOKED = 12,

  /** Spark job has being cancelling(killing) by streampark */
  STOPPING = 13,

  /** Job SUCCEEDED on yarn. */
  SUCCEEDED = 14,

  /** Has killed in Yarn. */
  KILLED = -9,
}
