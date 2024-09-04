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
import { RenderCallbackParams } from '/@/components/Form/src/types/form';
import { Icon, SvgIcon } from '/@/components/Icon';

import Mergely from '../components/Mergely.vue';
import { Alert, Dropdown, Input, Menu, Select, Switch, Tag } from 'ant-design-vue';
import { SettingTwoTone } from '@ant-design/icons-vue';
import { unref } from 'vue';
import { decodeByBase64 } from '/@/utils/cipher';
import { SelectValue } from 'ant-design-vue/lib/select';
import { useI18n } from '/@/hooks/web/useI18n';
import { fetchYarnQueueList } from '/@/api/setting/yarnQueue';
import { ApiSelect } from '/@/components/Form';
import { ResourceTypeEnum } from '/@/views/resource/upload/upload.data';
import { fetchSparkConfTemplate } from '/@/api/spark/conf';

const { t } = useI18n();
/* render input dropdown component */
export const renderInputDropdown = (
  formModel: Recordable,
  field: string,
  componentProps: { placeholder: string; options: Array<string> },
) => {
  return (
    <Input
      type="text"
      placeholder={componentProps.placeholder}
      allowClear
      value={formModel[field]}
      onInput={(e) => (formModel[field] = e.target.value)}
    >
      {{
        addonAfter: () => (
          <Dropdown placement="bottomRight">
            {{
              overlay: () => (
                <div>
                  <Menu trigger="['click', 'hover']">
                    {componentProps.options.map((item) => {
                      return (
                        <Menu.Item
                          key={item}
                          onClick={() => (formModel[field] = item)}
                          class="pr-60px"
                        >
                          <Icon icon="ant-design:plus-circle-outlined" />
                          {{ item }}
                        </Menu.Item>
                      );
                    })}
                  </Menu>
                  <Icon icon="ant-design:history-outlined" />
                </div>
              ),
            }}
          </Dropdown>
        ),
      }}
    </Input>
  );
};

/* render Yarn Queue */
export const renderYarnQueue = ({ model, field }: RenderCallbackParams) => {
  return (
    <div>
      <ApiSelect
        name="yarnQueue"
        placeholder={t('setting.yarnQueue.placeholder.yarnQueueLabelExpression')}
        api={fetchYarnQueueList}
        params={{ page: 1, pageSize: 9999 }}
        resultField={'records'}
        labelField={'queueLabel'}
        valueField={'queueLabel'}
        showSearch={true}
        value={model[field]}
        onChange={(value: string) => (model[field] = value)}
      />
      <p class="conf-desc mt-10px">
        <span class="note-info">
          <Tag color="#2db7f5" class="tag-note">
            {t('flink.app.noteInfo.note')}
          </Tag>
          {t('setting.yarnQueue.selectionHint')}
        </span>
      </p>
    </div>
  );
};

/* render memory option */
export const renderDynamicProperties = ({ model, field }: RenderCallbackParams) => {
  return (
    <div>
      <Input.TextArea
        rows={8}
        name="dynamicProperties"
        placeholder="$key=$value,If there are multiple parameters,you can new line enter them (-D <arg>)"
        value={model[field]}
        onInput={(e: ChangeEvent) => (model[field] = e?.target?.value)}
      />
      <p class="conf-desc mt-10px">
        <span class="note-info">
          <Tag color="#2db7f5" class="tag-note">
            {t('flink.app.noteInfo.note')}
          </Tag>
          <a
            href="https://ci.apache.org/projects/flink/flink-docs-stable/ops/config.html"
            target="_blank"
            class="pl-5px"
          >
            Flink {t('flink.app.noteInfo.officialDoc')}
          </a>
        </span>
      </p>
    </div>
  );
};

export const getAlertSvgIcon = (name: string, text: string) => {
  return (
    <Alert type="info">
      {{
        message: () => (
          <div>
            <SvgIcon class="mr-8px" name={name} style={{ color: '#108ee9' }} />
            <span>{text}</span>
          </div>
        ),
      }}
    </Alert>
  );
};
/* render application conf */
export const renderIsSetConfig = (
  model: Recordable,
  field: string,
  registerConfDrawer: Fn,
  openConfDrawer: Fn,
) => {
  /* Open the sqlConf drawer */
  async function handleSQLConf(checked: boolean) {
    if (checked) {
      if (unref(model.configOverride)) {
        openConfDrawer(true, {
          configOverride: unref(model.configOverride),
        });
      } else {
        const res = await fetchSparkConfTemplate();
        openConfDrawer(true, {
          configOverride: decodeByBase64(res),
        });
      }
    } else {
      openConfDrawer(false);
      Object.assign(model, {
        configOverride: null,
        isSetConfig: false,
      });
    }
  }
  function handleEditConfClose() {
    if (!model.configOverride) {
      model.isSetConfig = false;
    }
  }
  function handleMergeSubmit(data: { configOverride: string; isSetConfig: boolean }) {
    if (data.configOverride == null || !data.configOverride.replace(/^\s+|\s+$/gm, '')) {
      Object.assign(model, {
        configOverride: null,
        isSetConfig: false,
      });
    } else {
      Object.assign(model, {
        configOverride: data.configOverride,
        isSetConfig: true,
      });
    }
  }
  function handleConfChange(checked: boolean) {
    model[field] = checked;
    if (checked) {
      handleSQLConf(true);
    }
  }
  return (
    <div>
      <Switch
        checked-children="ON"
        un-checked-children="OFF"
        checked={model[field]}
        onChange={handleConfChange}
      />
      {model[field] && (
        <SettingTwoTone
          class="ml-10px"
          theme="twoTone"
          two-tone-color="#4a9ff5"
          onClick={() => handleSQLConf(true)}
        />
      )}

      <Mergely
        onOk={handleMergeSubmit}
        onClose={() => handleEditConfClose()}
        onRegister={registerConfDrawer}
      />
    </div>
  );
};

export const renderResourceFrom = (model: Recordable) => {
  return (
    <Select
      onChange={(value: string) => (model.resourceFrom = value)}
      value={model.resourceFrom}
      placeholder="Please select resource from"
    >
      <Select.Option value="1">
        <SvgIcon name="github" />
        <span class="pl-10px">Project</span>
        <span class="gray"> (build from Project)</span>
      </Select.Option>
      <Select.Option value="2">
        <SvgIcon name="upload" />
        <span class="pl-10px">Upload</span>
        <span class="gray"> (upload local job)</span>
      </Select.Option>
    </Select>
  );
};

export const renderStreamParkResource = ({ model, resources }) => {
  const renderOptions = () => {
    return (resources || [])
      .filter((item) => item.resourceType !== ResourceTypeEnum.FLINK_APP)
      .map((resource) => {
        return (
          <Select.Option
            key={resource.id}
            label={resource.resourceType + '-' + resource.resourceName}
          >
            <div>
              <Tag color="green" class="ml5px" size="small">
                {resource.resourceType}
              </Tag>
              <span class="color-[darkgrey]">{resource.resourceName}</span>
            </div>
          </Select.Option>
        );
      });
  };

  return (
    <div>
      <Select
        show-search
        allow-clear
        optionFilterProp="label"
        mode="multiple"
        max-tag-count={3}
        onChange={(value) => (model.teamResource = value)}
        value={model.teamResource}
        placeholder={t('flink.app.resourcePlaceHolder')}
      >
        {renderOptions()}
      </Select>
    </div>
  );
};

export const renderStreamParkJarApp = ({ model, resources }) => {
  function handleAppChange(value: SelectValue) {
    const res = resources.filter((item) => item.id == value)[0];
    model.mainClass = res.mainClass;
    model.uploadJobJar = res.resourceName;
  }

  const renderOptions = () => {
    console.log('resources', resources);
    return (resources || [])
      .filter((item) => item.resourceType == ResourceTypeEnum.FLINK_APP)
      .map((resource) => {
        return (
          <Select.Option key={resource.id} label={resource.resourceName}>
            <div>
              <Tag color="green" style=";margin-left: 5px;" size="small">
                {resource.resourceType}
              </Tag>
              <span style="color: darkgrey">{resource.resourceName}</span>
            </div>
          </Select.Option>
        );
      });
  };

  return (
    <div>
      <Select
        show-search
        allow-clear
        optionFilterProp="label"
        onChange={handleAppChange}
        value={model.uploadJobJar}
        placeholder={t('flink.app.selectAppPlaceHolder')}
      >
        {renderOptions()}
      </Select>
    </div>
  );
};
