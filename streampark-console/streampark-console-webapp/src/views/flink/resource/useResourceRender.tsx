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
import { Select } from 'ant-design-vue';
import { useI18n } from '/@/hooks/web/useI18n';
import { ResourceTypeEnum } from "/@/views/flink/resource/resource.data";
import flinkAppSvg from '/@/assets/icons/flink2.svg';
import connectorSvg from '/@/assets/icons/connector.svg';
import udxfSvg from '/@/assets/icons/fx.svg';
import normalJarSvg from '/@/assets/icons/jar.svg';

const { t } = useI18n();

/* render resource type label */
export const renderResourceType = ({ model },) => {

  const renderOptions = () => {
    const options = [
      { label: 'FLINK_APP', value: ResourceTypeEnum.FLINK_APP, src: flinkAppSvg },
      { label: 'CONNECTOR', value: ResourceTypeEnum.CONNECTOR, src: connectorSvg },
      { label: 'UDXF', value: ResourceTypeEnum.UDXF, src: udxfSvg },
      { label: 'NORMAL_JAR', value: ResourceTypeEnum.NORMAL_JAR, src: normalJarSvg },
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
        style="width: calc(100% - 60px)"
        onChange={(value) => (model.resourceType = value)}
      >
        {renderOptions()}
      </Select>
    </div>
  );
};
