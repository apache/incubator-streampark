/* flink home 数据 */
export interface FlinkEnv {
  id: string;
  flinkName: string;
  flinkHome: string;
  flinkConf: string;
  description: string;
  scalaVersion: string;
  version: string;
  isDefault: boolean;
  createTime: string;
  streamxScalaVersion: string;
}

export interface FlinkCreate {
  id?: string | null;
  flinkName: string;
  flinkHome: string;
  description: string;
}
