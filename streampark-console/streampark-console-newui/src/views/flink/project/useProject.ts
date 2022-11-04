/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { FormSchema } from '/@/components/Table';
import { RuleObject, StoreValue } from 'ant-design-vue/lib/form/interface';
import { computed, nextTick, reactive, ref, unref } from 'vue';
import { fetchBranches, getDetail, gitCheck, isExist } from '/@/api/flink/project';
import { useForm } from '/@/components/Form';
import { useMessage } from '/@/hooks/web/useMessage';
import { useRoute } from 'vue-router';
import { ProjectRecord } from '/@/api/flink/project/model/projectModel';
import { filterOption } from '../app/utils';

export const useProject = () => {
  const route = useRoute();
  const { createMessage, createErrorSwal } = useMessage();

  const submitLoading = ref(false);
  const projectResource = reactive<Partial<ProjectRecord>>({});

  const getLoading = computed(() => {
    return submitLoading.value;
  });

  const projectFormSchema = computed((): FormSchema[] => {
    return [
      {
        field: 'name',
        label: 'Project Name',
        component: 'Input',
        rules: [{ required: true, validator: checkProjectName, trigger: 'blur' }],
        componentProps: { placeholder: 'the project name' },
      },
      {
        field: 'type',
        label: 'Project Type',
        component: 'Select',
        defaultValue: 1,
        componentProps: {
          placeholder: 'the project type',
          options: [
            { label: 'apache flink', value: 1, disabled: false },
            { label: 'apache spark', value: 2, disabled: true },
          ],
          showSearch: true,
          optionFilterProp: 'children',
          filterOption,
        },
        rules: [{ required: true, type: 'number', message: 'Project Type is required' }],
      },
      {
        field: 'repository',
        label: 'CVS',
        component: 'Select',
        componentProps: {
          showSearch: true,
          optionFilterProp: 'children',
          filterOption,
          options: [
            { label: 'GitHub/GitLab', value: 1, disabled: false },
            { label: 'Subversion', value: 2, disabled: true },
          ],
          placeholder: 'CVS',
        },
        rules: [{ required: true, type: 'number', message: 'CVS is required' }],
      },
      {
        field: 'url',
        label: 'Repository URL',
        component: 'Input',
        componentProps: {
          placeholder: 'The Repository URL for this project',
        },
        rules: [{ required: true, message: 'Repository URL is required' }],
      },
      {
        field: 'userName',
        label: 'UserName',
        component: 'Input',
        componentProps: {
          placeholder: 'UserName for this project',
        },
      },
      {
        field: 'password',
        label: 'Password',
        component: 'InputPassword',
        componentProps: {
          placeholder: 'Password for this project',
        },
      },
      {
        field: 'branches',
        label: 'Branches',
        component: 'Select',
        required: true,
        componentProps: ({ formModel }) => {
          return {
            showSearch: true,
            filterOption,
            placeholder: 'Select a branch',
            options: unref(branchList),
            onDropdownVisibleChange: (open: boolean) => {
              console.log('open', open);
              if (open) {
                handleBranches(formModel);
              }
            },
          };
        },
      },
      {
        field: 'pom',
        label: 'POM',
        component: 'Input',
        componentProps: {
          placeholder:
            'By default,lookup pom.xml in root path,You can manually specify the module to compile pom.xml"',
        },
        rules: [
          {
            message:
              'Specifies the module to compile pom.xml If it is not specified, it is found under the root path pom.xml',
          },
        ],
      },
      {
        field: 'buildArgs',
        label: 'Build Argument',
        component: 'InputTextArea',
        componentProps: {
          rows: 2,
          placeholder: 'Build Argument, e.g: -Pprod',
        },
      },
      {
        field: 'description',
        label: 'description',
        component: 'InputTextArea',
        componentProps: {
          rows: 4,
          placeholder: 'Description for this project',
        },
      },
    ];
  });

  const [registerForm, { submit, setFieldsValue }] = useForm({
    labelWidth: 120,
    colon: true,
    labelCol: { lg: { span: 5, offset: 0 }, sm: { span: 7, offset: 0 } },
    wrapperCol: { lg: { span: 16, offset: 0 }, sm: { span: 17, offset: 0 } },
    schemas: projectFormSchema.value,
    showActionButtonGroup: false,
    baseColProps: { span: 24 },
  });
  const branchList = ref<Array<any>>([]);

  async function checkProjectName(_rule: RuleObject, value: StoreValue) {
    if (!value) {
      return Promise.reject('The Project Name is required');
    }
    if (Object.keys(projectResource).length == 0 || value !== projectResource.name) {
      const res = await isExist({ name: value });
      if (res) {
        return Promise.reject(`The Project Name is already exists. Please check`);
      }
    } else {
      return Promise.resolve();
    }
  }

  /* form submit */
  async function handleSubmit(values: Recordable, FetchAction: (v: Recordable) => Promise<void>) {
    submitLoading.value = true;
    try {
      const res = await gitCheck({
        url: values.url,
        branches: values.branches,
        userName: values.userName || null,
        password: values.password || null,
      });
      if (res === 0) {
        if (branchList.value.length === 0) {
          await handleBranches(values);
        }
        if (!branchList.value.find((v) => v.value == values.branches)) {
          createErrorSwal(
            'branch [' +
              values.branches +
              '] does not exist<br>or authentication error,please check',
          );
        } else {
          await FetchAction(values);
        }
      } else {
        createErrorSwal(
          res === 1
            ? 'not authorized ..>﹏<.. <br><br> userName and password is required'
            : 'authentication error ..>﹏<.. <br><br> please check userName and password',
        );
      }
    } catch (error) {
      console.error(error);
    } finally {
      submitLoading.value = false;
    }
  }

  async function handleBranches(values: Recordable) {
    const hide = createMessage.loading('Getting branch');
    try {
      const url = values.url;
      if (url) {
        const userName = values.userName || null;
        const password = values.password || null;
        const userNull = userName === null || userName === undefined || userName === '';
        const passNull = password === null || password === undefined || password === '';
        if ((userNull && passNull) || (!userNull && !passNull)) {
          const res = await fetchBranches({ url, userName, password });
          if (res) branchList.value = res.map((i) => ({ label: i, value: i }));
        }
      }
    } catch (error) {
      console.error(error);
    } finally {
      hide();
    }
  }

  async function handleGet() {
    const res = await getDetail({ id: route.query.id });
    Object.assign(projectResource, res);
    nextTick(() => {
      setFieldsValue({
        name: res.name,
        type: res.type,
        repository: res.repository,
        url: res.url,
        userName: res.userName,
        password: res.password,
        branches: res.branches,
        pom: res.pom,
        buildArgs: res.buildArgs,
        description: res.description,
      });
    });
  }
  return {
    submit,
    handleSubmit,
    getLoading,
    registerForm,
    projectResource,
    handleGet,
  };
};
