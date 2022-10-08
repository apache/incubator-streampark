export interface ProjectModel {
  records: ProjectRecord[];
  total: number;
}
export interface ProjectRecord {
  id: string;
  name: string;
  url: string;
  branches: string;
  lastBuild?: any;
  userName?: any;
  password?: any;
  repository: number;
  pom?: any;
  buildArgs?: any;
  description: string;
  buildState: number | string;
  type: number;
  createTime: string;
  modifyTime: string;
  module?: any;
  dateFrom?: any;
  dateTo?: any;
}
