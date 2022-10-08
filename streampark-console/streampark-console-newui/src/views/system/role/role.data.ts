import { BasicColumn } from '/@/components/Table';
import { FormSchema } from '/@/components/Table';

export const columns: BasicColumn[] = [
  {
    title: 'Role Name',
    dataIndex: 'roleName',
  },
  {
    title: 'Description',
    dataIndex: 'remark',
  },
  {
    title: 'Create Time',
    dataIndex: 'createTime',
  },
  {
    title: 'Modify Time',
    dataIndex: 'modifyTime',
  },
];

export const searchFormSchema: FormSchema[] = [
  {
    field: 'roleName',
    label: 'Role',
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
    field: 'roleId',
    label: 'Role Id',
    component: 'Input',
    show: false,
  },
  {
    field: 'roleName',
    label: 'Role Name',
    required: true,
    component: 'Input',
  },
  {
    label: 'Description',
    field: 'remark',
    component: 'InputTextArea',
  },
  {
    label: '',
    field: 'menuId',
    slot: 'menu',
    defaultValue: [],
    component: 'Input',
  },
];
