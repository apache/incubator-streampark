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
import {Select, Tag} from 'ant-design-vue';
import { useI18n } from '/@/hooks/web/useI18n';
import { ResourceTypeEnum } from "/@/views/flink/resource/resource.data";
import flinkAppSvg from '/@/assets/icons/flink2.svg';
import connectorSvg from '/@/assets/icons/connector.svg';
import udxfSvg from '/@/assets/icons/fx.svg';
import normalJarSvg from '/@/assets/icons/jar.svg';
import groupSvg from '/@/assets/icons/group.svg';

const { t } = useI18n();

/* render resource type label */
export const renderResourceType = ({ model },) => {

  const renderOptions = () => {
    const options = [
      { label: 'Flink App', value: ResourceTypeEnum.FLINK_APP, src: flinkAppSvg },
      { label: 'Connector', value: ResourceTypeEnum.CONNECTOR, src: connectorSvg },
      { label: 'UDXF', value: ResourceTypeEnum.UDXF, src: udxfSvg },
      { label: 'Normal Jar', value: ResourceTypeEnum.NORMAL_JAR, src: normalJarSvg },
      { label: 'Group', value: ResourceTypeEnum.GROUP, src: groupSvg },
    ];
    return options
      .map(( {label,value, src} ) => {
        return (
          <Select.Option
            key={ value }
            label={ label }
            >
            <div>
              <img
                src={ src }
                style="display: inline-block; width: 20px; height: 20px"
              ></img>
              <span
                style="vertical-align: middle; margin-left: 5px;"
              >
                { label }
              </span>
            </div>
          </Select.Option>
        );
      });
  };

  return (
    <div>
      <Select
        allow-clear
        placeholder={t('flink.resource.form.resourceTypePlaceholder')}
        value={model.resourceType}
        onChange={(value) => (model.resourceType = value)}
      >
        {renderOptions()}
      </Select>
    </div>
  );
};

export const renderStreamParkResourceGroup = ({ model, resources },) => {

  const renderOptions = () => {
    console.log('resources', resources);
    return (resources || [])
      .filter((item) => item.resourceType !== ResourceTypeEnum.FLINK_APP
        && item.resourceType !== ResourceTypeEnum.GROUP)
      .map((resource) => {
        return (
          <Select.Option
            key={resource.id}
            label={ resource.resourceType + '-' + resource.resourceName}>
            <div>
              <Tag color="green" style=";margin-left: 5px;" size="small">
                {resource.resourceType}
              </Tag>
              <span style="color: darkgrey">
              {resource.resourceName}
            </span>
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
        onChange={(value) => (model.resourceGroup = value)}
        value={model.resourceGroup}
        placeholder={t('flink.resource.resourceGroupPlaceholder')}
      >
        {renderOptions()}
      </Select>
    </div>
  );
};
