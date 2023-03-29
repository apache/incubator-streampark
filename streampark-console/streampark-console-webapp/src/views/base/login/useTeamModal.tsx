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
import { ref } from 'vue';
import Icon from '/@/components/Icon';
import { useMessage } from '/@/hooks/web/useMessage';
import { useUserStore } from '/@/store/modules/user';
export const useTeamModal = (handleTeamSelect: Fn) => {
  const teamId = ref('');
  const { createConfirm, createMessage } = useMessage();
  const userStore = useUserStore();
  async function createTeamModal() {
    createConfirm({
      iconType: 'warning',
      title: () => {
        return (
          <div>
            <Icon icon="ant-design:setting-outlined" />
            <span>Select Team</span>
          </div>
        );
      },
      content: () => {
        return (
          <div>
            <span>Team</span>
            <Select
              value={teamId}
              onChange={(value: string) => (teamId.value = value)}
              options={userStore.getTeamList}
            ></Select>
          </div>
        );
      },
      onOk: async () => {
        if (!teamId.value) {
          createMessage.warning('please select a team');
          return Promise.reject();
        }
        await handleTeamSelect(teamId.value);
        return Promise.resolve();
      },
    });
  }
  return { createTeamModal };
};
