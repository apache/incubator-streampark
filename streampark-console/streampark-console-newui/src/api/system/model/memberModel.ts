export interface MemberListRecord {
  id: string;
  teamId: string;
  userId: string;
  roleId: string;
  createTime: string;
  modifyTime: string;
  userName: string;
  roleName: string;
  sortField?: string;
  sortOrder?: string;
  createTimeFrom?: string;
  createTimeTo?: string;
}
export interface AddMemberParams {
  teamId: string;
  userName: string;
  roleId: number;
}

export interface TeamMemberResp {
  id: string;
  teamName: string;
}

export interface UpdateMemberParams extends AddMemberParams {
  id: string;
  userId: string;
}
