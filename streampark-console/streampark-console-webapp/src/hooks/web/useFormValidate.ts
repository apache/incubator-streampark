import { computed, ref, unref } from 'vue';
import { FormItem } from '/@/components/Form/src/types/formItem';
export type FormValidateStatus = 'success' | 'warning' | 'error' | 'validating' | '';

export const useFormValidate = () => {
  const validateStatus = ref<FormValidateStatus>('');
  const help = ref('');

  const getValidateStatus = computed(() => {
    return validateStatus.value;
  });
  const setValidateStatus = (status: FormValidateStatus) => {
    validateStatus.value = status;
  };

  const getHelp = computed(() => {
    return help.value;
  });

  const setHelp = (message: string) => {
    help.value = message;
  };
  const getItemProp = computed((): Partial<FormItem> => {
    return { validateStatus: unref(validateStatus), help: unref(help), hasFeedback: true };
  });
  return { getValidateStatus, setValidateStatus, getHelp, setHelp, getItemProp };
};
