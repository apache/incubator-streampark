import { AxiosResponse } from 'axios';
import { ContentTypeEnum } from '/@/enums/httpEnum';
import { defHttp } from '/@/utils/http/axios';

enum NOTEBOOK_API {
  TUTORIAL = '/tutorial/get',
  SUBMIT = '/flink/notebook/submit',
}
interface TutorialResponse {
  id: string;
  name: string;
  type: number;
  content: string;
  createTime: string;
}
/**
 * @returns Promise<TutorialResponse>
 */
export function fetchReadmd(params: { name: string }) {
  return defHttp.post<TutorialResponse>({
    url: NOTEBOOK_API.TUTORIAL,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}
/**
 * submit notebook
 * @returns Promise<AxiosResponse>
 */
export function fetchNotebookSubmit(params: { env: string; text: string }) {
  return defHttp.post<AxiosResponse<any>>(
    {
      url: NOTEBOOK_API.SUBMIT,
      params,
      headers: {
        'Content-Type': ContentTypeEnum.FORM_URLENCODED,
      },
    },
    {
      isReturnNativeResponse: true,
    },
  );
}
