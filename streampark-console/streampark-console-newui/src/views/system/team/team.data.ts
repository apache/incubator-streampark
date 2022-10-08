import { BasicColumn, FormSchema } from '/@/components/Table';

export const columns: BasicColumn[] = [
  {
    title: 'Team Code',
    dataIndex: 'teamCode',
    width: 200,
    align: 'left',
  },
  {
    title: 'Team Name',
    dataIndex: 'teamName',
  },
  {
    title: 'Create Time',
    dataIndex: 'createTime',
    width: 180,
  },
];

export const searchFormSchema: FormSchema[] = [
  {
    field: 'teamName',
    label: 'Team Name',
    component: 'Input',
    colProps: { span: 8 },
  },
  {
    field: 'createTime',
    label: 'Create Time',
    component: 'RangePicker',
    colProps: { span: 8 },
  },
];

export const formSchema: FormSchema[] = [
  {
    field: 'teamName',
    label: 'Team Name',
    component: 'Input',
    required: true,
  },
  {
    field: 'teamCode',
    label: 'Team Code',
    component: 'Input',
    required: true,
  },
];
