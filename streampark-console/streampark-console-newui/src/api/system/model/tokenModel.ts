// token list record
export interface TokenListRecord {
  id: string;
  userId: string;
  token: string;
  status: number;
  expireTime: string;
  description: string;
  createTime: string;
  modifyTime: string;
  username: string;
  userStatus: string;
  finalStatus: number;
}

export interface TokenCreateParam {
  userId: number;
  description: string;
  expireTime: string;
  teamId: string;
}
