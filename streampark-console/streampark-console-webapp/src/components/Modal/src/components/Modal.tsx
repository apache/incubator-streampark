import { Modal } from 'ant-design-vue';
import { defineComponent, toRefs, unref } from 'vue';
import { basicProps } from '../props';
import { useModalDragMove } from '../hooks/useModalDrag';
import { extendSlots } from '/@/utils/helper/tsxHelper';

export default defineComponent({
  name: 'Modal',
  inheritAttrs: false,
  props: basicProps,
  emits: ['cancel'],
  setup(props, { attrs, slots, emit }) {
    const { visible, draggable, destroyOnClose } = toRefs(props);
    useModalDragMove({
      visible,
      destroyOnClose,
      draggable,
    });

    const onCancel = (e: Event) => {
      emit('cancel', e);
    };

    return () => {
      const propsData = { ...unref(attrs), ...props, onCancel } as Recordable;
      return <Modal {...propsData}>{extendSlots(slots)}</Modal>;
    };
  },
});
