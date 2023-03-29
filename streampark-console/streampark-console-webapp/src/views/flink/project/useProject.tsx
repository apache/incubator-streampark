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
import { useI18n } from '/@/hooks/web/useI18n';
import { GitCredentialEnum, ProjectTypeEnum, CVSTypeEnum } from '/@/enums/projectEnum';
import RepositoryGroup from './components/RepositoryGroup';
import { Form } from 'ant-design-vue';

const { t } = useI18n();
export const useProject = () => {
  const route = useRoute();
  const { createMessage, createErrorSwal } = useMessage();

  const submitLoading = ref(false);
  const projectResource = reactive<Partial<ProjectRecord>>({});

  const getLoading = computed(() => {
    return submitLoading.value;
  });
  const handleCheckRepositoryUrl = (values: Recordable) => {
    if (!values.url) {
      return Promise.reject(t('flink.project.form.repositoryURLRequired'));
    }
    switch (values.gitCredential) {
      case GitCredentialEnum.SSH:
        if (/^git@(.*)/.test(values.url)) {
          return Promise.resolve();
        } else {
          return Promise.reject(t('flink.project.form.gitChecked'));
        }
      default:
        if (/^http(s)?:\/\//.test(values.url)) {
          return Promise.resolve();
        } else {
          return Promise.reject(t('flink.project.form.httpChecked'));
        }
    }
  };
  const projectFormSchema = computed((): FormSchema[] => {
    return [
      {
        field: 'name',
        label: t('flink.project.form.projectName'),
        component: 'Input',
        rules: [{ required: true, validator: checkProjectName, trigger: 'blur' }],
        componentProps: { placeholder: t('flink.project.form.projectNamePlaceholder') },
      },
      {
        field: 'type',
        label: t('flink.project.form.projectType'),
        component: 'Select',
        defaultValue: ProjectTypeEnum.FLINK,
        componentProps: {
          placeholder: t('flink.project.form.projectTypePlaceholder'),
          options: [
            { label: 'apache flink', value: ProjectTypeEnum.FLINK, disabled: false },
            { label: 'apache spark', value: ProjectTypeEnum.SPARK, disabled: true },
          ],
          showSearch: true,
          optionFilterProp: 'children',
          filterOption,
        },
        rules: [
          {
            required: true,
            type: 'number',
            message: t('flink.project.operationTips.projectTypeIsRequiredMessage'),
          },
        ],
      },

      {
        field: 'repository',
        label: t('flink.project.form.cvs'),
        component: 'Select',
        componentProps: {
          showSearch: true,
          optionFilterProp: 'children',
          filterOption,
          options: [{ label: 'GitHub/GitLab', value: CVSTypeEnum.GIT, disabled: false }],
          placeholder: t('flink.project.form.cvsPlaceholder'),
        },
        rules: [
          {
            required: true,
            type: 'number',
            message: t('flink.project.operationTips.cvsIsRequiredMessage'),
          },
        ],
      },
      {
        field: 'gitCredential',
        label: '',
        component: 'Input',
        show: false,
        defaultValue: GitCredentialEnum.HTTPS,
      },
      { field: 'url', label: '', component: 'Input', show: false },
      {
        field: 'repositoryUrl',
        label: t('flink.project.form.repositoryURL'),
        component: 'Input',
        renderColContent: ({ model }) => {
          return (
            <Form.Item
              label={t('flink.project.form.repositoryURL')}
              name="repositoryUrl"
              rules={[{ required: true, validator: () => handleCheckRepositoryUrl(model) }]}
            >
              <RepositoryGroup
                value={{
                  gitCredential: Number(model.gitCredential) || GitCredentialEnum.HTTPS,
                  url: model.url || '',
                }}
                onUpdateProtocol={(value) => (model.gitCredential = value)}
                onUpdateUrl={(value) => (model.url = value)}
              />
            </Form.Item>
          );
        },
      },
      {
        field: 'prvkeyPath',
        label: t('flink.project.form.prvkeyPath'),
        component: 'Input',
        ifShow: ({ values }) => values.gitCredential == GitCredentialEnum.SSH,
        componentProps: {
          placeholder: t('flink.project.form.prvkeyPathPlaceholder'),
        },
      },
      {
        field: 'userName',
        label: t('flink.project.form.userName'),
        component: 'Input',
        ifShow: ({ values }) => values.gitCredential == GitCredentialEnum.HTTPS,
        componentProps: {
          placeholder: t('flink.project.form.userNamePlaceholder'),
          autocomplete: 'new-password',
        },
      },
      {
        field: 'password',
        label: t('flink.project.form.password'),
        component: 'InputPassword',
        componentProps: {
          placeholder: t('flink.project.form.passwordPlaceholder'),
          autocomplete: 'new-password',
        },
      },
      {
        field: 'branches',
        label: t('flink.project.form.branches'),
        component: 'Select',
        required: true,
        componentProps: ({ formModel }) => {
          return {
            showSearch: true,
            filterOption,
            placeholder: t('flink.project.form.branchesPlaceholder'),
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
        label: t('flink.project.form.pom'),
        component: 'Input',
        componentProps: {
          placeholder: t('flink.project.form.pomPlaceholder'),
        },
        rules: [
          {
            message: t('flink.project.operationTips.pomSpecifiesModuleMessage'),
          },
        ],
      },
      {
        field: 'buildArgs',
        label: t('flink.project.form.buildArgs'),
        component: 'InputTextArea',
        componentProps: {
          rows: 2,
          placeholder: t('flink.project.form.buildArgsPlaceholder'),
        },
      },
      {
        field: 'description',
        label: t('flink.project.form.description'),
        component: 'InputTextArea',
        componentProps: {
          rows: 4,
          placeholder: t('flink.project.form.descriptionPlaceholder'),
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
      return Promise.reject(t('flink.project.operationTips.projectNameIsRequiredMessage'));
    }
    if (Object.keys(projectResource).length == 0 || value !== projectResource.name) {
      const res = await isExist({ name: value });
      if (res) {
        return Promise.reject(t('flink.project.operationTips.projectNameIsUniqueMessage'));
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
        gitCredential: values.gitCredential,
        userName: values.userName || null,
        password: values.password || null,
        prvkeyPath: values.prvkeyPath || null,
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
            ? t('flink.project.operationTips.notAuthorizedMessage')
            : t('flink.project.operationTips.authenticationErrorMessage'),
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
        const gitCredential = values.gitCredential;
        const userName = values.userName || null;
        const password = values.password || null;
        const prvkeyPath = values.prvkeyPath || null;
        const userNull = userName === null || userName === undefined || userName === '';
        const passNull = password === null || password === undefined || password === '';
        if ((userNull && passNull) || (!userNull && !passNull)) {
          const res = await fetchBranches({ gitCredential, url, userName, password, prvkeyPath });
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
        gitCredential: res.gitCredential || GitCredentialEnum.HTTPS,
        url: res.url,
        userName: res.userName,
        password: res.password,
        prvkeyPath: res.prvkeyPath || null,
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
