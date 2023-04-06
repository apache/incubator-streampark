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
import { Input, Tag } from 'ant-design-vue';
import { useI18n } from '/@/hooks/web/useI18n';

const { t } = useI18n();

/* Render Yarn Queue Label */
export const renderYarnQueueLabel = ({ model, field }: RenderCallbackParams) => {
  return (
    <div>
      <Input
        name="queueLabel"
        placeholder={t('setting.yarnQueue.placeholder.yarnQueueLabelExpression')}
        value={model[field]}
        onInput={(e: ChangeEvent) => (model[field] = e?.target?.value)}
      />
      <p class="conf-desc mt-10px">
        <span class="note-info">
          <Tag color="#2db7f5" class="tag-note">
            {t('flink.app.noteInfo.note')}
          </Tag>
          {t('setting.yarnQueue.noteInfo')}
        </span>
      </p>
    </div>
  );
};
