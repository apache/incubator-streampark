export interface NoticyList {
  records: NoticyItem[];
  total: string;
}
export interface NoticyItem {
  id: string;
  appId: string;
  userId: string;
  title: string;
  type: number;
  context: string;
  readed: number;
  createTime: string;
  extra?: string;
}
