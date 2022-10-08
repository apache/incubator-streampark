import { BasicColumn } from '/@/components/Table';
import { FormSchema } from '/@/components/Table';
import { h } from 'vue';
import { Tag } from 'ant-design-vue';
import { Icon } from '/@/components/Icon';

export const enum TypeEnum {
  Menu = '0',
  Button = '1',
  Dir = '2',
}

const isDir = (type: string) => type === TypeEnum.Dir;
const isMenu = (type: string) => type === TypeEnum.Menu;
const isButton = (type: string) => type === TypeEnum.Button;

export const columns: BasicColumn[] = [
  {
    title: 'Name',
    dataIndex: 'text',
    width: 200,
    align: 'left',
  },
  {
    title: 'Icon',
    dataIndex: 'icon',
    width: 50,
    customRender: ({ record }) => {
      return record.icon
        ? h(Icon, { icon: record.icon + '-outlined', prefix: 'ant-design' })
        : null;
    },
  },
  {
    title: 'Type',
    dataIndex: 'type',
    customRender: ({ record }) => {
      const text = isMenu(record.type) ? '菜单' : '按钮';
      return h(Tag, { color: isMenu(record.type) ? 'cyan' : 'pink' }, () => text);
    },
  },
  {
    title: 'Path',
    dataIndex: 'path',
  },
  {
    title: 'Vue Component',
    dataIndex: 'component',
  },
  {
    title: 'Permission',
    dataIndex: 'permission',
  },
  {
    title: 'Order By',
    dataIndex: 'order',
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
    field: 'menuName',
    label: 'Menu Name',
    component: 'Input',
    colProps: { span: 8 },
  },
  {
    field: 'createTime',
    label: 'Create Time',
    component: 'RangePicker',
    componentProps: {},
    colProps: { span: 8 },
  },
];

export const formSchema: FormSchema[] = [
  {
    field: 'type',
    label: 'Menu Type',
    component: 'RadioButtonGroup',
    defaultValue: TypeEnum.Menu,
    componentProps: {
      options: [
        { label: 'Menu', value: TypeEnum.Menu },
        { label: 'Button', value: TypeEnum.Button },
      ],
    },
    colProps: { lg: 24, md: 24 },
  },
  {
    field: 'menuId',
    label: 'menuId',
    component: 'Input',
    show: false,
  },
  {
    field: 'menuName',
    label: 'Menu Name',
    component: 'Input',
    required: true,
    rules: [
      { required: true, message: 'Menu Name is required' },
      { max: 20, message: 'exceeds maximum length limit of 20 characters' },
    ],
  },
  {
    field: 'parentId',
    label: 'Parent Menu',
    component: 'TreeSelect',
    componentProps: {
      fieldNames: {
        label: 'title',
        key: 'id',
        value: 'id',
      },
      treeLine: true,
      getPopupContainer: () => document.body,
    },
  },
  {
    field: 'orderNum',
    label: '排序',
    component: 'InputNumber',
    required: true,
  },
  {
    field: 'icon',
    label: '图标',
    component: 'IconPicker',
    required: true,
    ifShow: ({ values }) => !isButton(values.type),
  },
  {
    field: 'path',
    label: '菜单URL',
    component: 'Input',
    required: true,
    ifShow: ({ values }) => !isButton(values.type),
  },
  {
    field: 'component',
    label: '组件地址',
    component: 'Input',
    ifShow: ({ values }) => isMenu(values.type),
  },
  {
    field: 'perms',
    label: '相关权限',
    component: 'Input',
    rules: [{ max: 50, message: '长度不能超过50个字符' }],
    ifShow: ({ values }) => !isDir(values.type),
  },
  {
    field: 'display',
    label: '是否显示',
    component: 'Switch',
    defaultValue: '1',
    componentProps: {
      checkedValue: '1',
      unCheckedValue: '0',
      checkedChildren: 'Yes',
      unCheckedChildren: 'No',
    },
    ifShow: ({ values }) => !isButton(values.type),
  },
];
