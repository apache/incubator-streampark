import { CSSProperties, VNodeChild } from 'vue';
import VueTypes, {
  toValidableType,
  createTypes,
  VueTypeValidableDef,
  VueTypesInterface,
} from 'vue-types';

export default class ProjectTypes extends VueTypes {
  // a native-like validator that supports the `.validable` method
  static get positive() {
    return toValidableType('positive', {
      type: Number,
      validator: (v) => v > 0,
    });
  }
}

export type VueNode = VNodeChild | JSX.Element;

type PropTypes = VueTypesInterface & {
  readonly style: VueTypeValidableDef<CSSProperties>;
  readonly VNodeChild: VueTypeValidableDef<VueNode>;
  // readonly trueBool: VueTypeValidableDef<boolean>;
};

const propTypes = createTypes({
  func: undefined,
  bool: undefined,
  string: undefined,
  number: undefined,
  object: undefined,
  integer: undefined,
}) as PropTypes;
export { propTypes };
