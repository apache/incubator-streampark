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

import { Input, Space } from 'ant-design-vue';

import { LinkBadge } from '/@/components/LinkBadge';

import { useI18n } from '/@/hooks/web/useI18n';

const { t } = useI18n();

const DEFAULT_BLUE_HEX = '#2970e3';
const DEFAULT_GREEN_HEX = '#10b53f';

export const renderColorField = (formModel: Recordable, field: string) => {
  const handlePromptColorUpdate = (color: string) => () => {
    formModel[field] = color;
  };
  return (
    <Space>
      <Input
        style="width:200px"
        type="color"
        allowClear
        value={formModel[field]}
        onInput={(e) => (formModel[field] = e.target.value)}
      />
      <LinkBadge
        label={t('setting.externalLink.form.badgeLabel')}
        message={t('setting.externalLink.form.badgeName')}
        color={DEFAULT_GREEN_HEX}
        onBadgeClick={handlePromptColorUpdate(DEFAULT_GREEN_HEX)}
      />
      <LinkBadge
        label="Fink"
        message="Metrics"
        color={DEFAULT_BLUE_HEX}
        onBadgeClick={handlePromptColorUpdate(DEFAULT_BLUE_HEX)}
      />
    </Space>
  );
};

export const renderPreview = (formModel: Recordable) => {
  return (
    <LinkBadge
      label={formModel['badgeLabel']}
      message={formModel['badgeName'] || t('setting.externalLink.form.badgeName')}
      color={formModel['badgeColor']}
    />
  );
};
