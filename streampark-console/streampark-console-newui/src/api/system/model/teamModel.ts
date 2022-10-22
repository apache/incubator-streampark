export interface TeamListRecord {
  id: string;
  teamName: string;
  description: string;
  createTime: string;
  modifyTime: string;
  sortField?: any;
  sortOrder?: any;
  createTimeFrom?: any;
  createTimeTo?: any;
}

export interface TeamParam {
  id?: string;
  teamName: string;
  description: string;
}
