import { ButtonProps } from 'ant-design-vue/es/button/buttonTypes';
import { TooltipProps } from 'ant-design-vue/es/tooltip/Tooltip';
import { RoleEnum } from '/@/enums/roleEnum';
export interface ActionItem extends ButtonProps {
  onClick?: Fn;
  label?: string;
  id?: string;
  class?: string;
  color?: 'success' | 'error' | 'warning';
  icon?: string;
  popConfirm?: PopConfirm;
  disabled?: boolean;
  divider?: boolean;
  // The permission encoding controls whether it is displayed
  auth?: RoleEnum | RoleEnum[] | string | string[];
  // Whether business controls are displayed
  ifShow?: boolean | ((action: ActionItem) => boolean);
  tooltip?: string | TooltipProps;
}

export interface PopConfirm {
  id?: string;
  class?: string;
  title: string;
  okText?: string;
  cancelText?: string;
  confirm: Fn;
  cancel?: Fn;
  icon?: string;
  placement?:
    | 'top'
    | 'left'
    | 'right'
    | 'bottom'
    | 'topLeft'
    | 'topRight'
    | 'leftTop'
    | 'leftBottom'
    | 'rightTop'
    | 'rightBottom'
    | 'bottomLeft'
    | 'bottomRight';
  okButtonProps?: ButtonProps & { class: string };
  cancelButtonProps?: ButtonProps & { class: string };
}
