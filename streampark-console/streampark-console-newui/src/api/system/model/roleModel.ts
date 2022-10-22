export interface RoleParam {
  roleId?: number;
  roleName: string;
  remark: string;
  menuId: string[];
}

export interface RoleListRecord {
  roleId: string;
  roleName: string;
  roleCode?: any;
  remark: string;
  createTime: string;
  modifyTime: string;
  sortField?: any;
  sortOrder?: any;
  createTimeFrom?: any;
  createTimeTo?: any;
  menuId?: any;
}
