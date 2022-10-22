export interface VariableListRecord {
  id: string;
  variableCode: string;
  variableValue: string;
  description: string;
  creatorId: string;
  creatorName: string;
  teamId: string;
  createTime: string;
  modifyTime: string;
  sortField?: string;
  sortOrder?: string;
}

export interface VariableParam {
  id?: string;
  variableCode: string;
  variableValue: string;
  description: string;
}

export interface VariableDeleteParam {
  id: string;
  variableCode: string;
  variableValue: string;
  teamId: string;
}
