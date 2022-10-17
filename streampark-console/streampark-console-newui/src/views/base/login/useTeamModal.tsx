import { Select } from 'ant-design-vue';
import { ref } from 'vue';
import { getTeamList } from '/@/api/system/team';
import Icon from '/@/components/Icon';
import { useMessage } from '/@/hooks/web/useMessage';
import { useUserStoreWithOut } from '/@/store/modules/user';

export const useTeamModal = () => {
  const userStore = useUserStoreWithOut();
  const teamId = ref('');
  const { createConfirm, createMessage } = useMessage();

  async function createTeamModal() {
    const teamList: Array<Recordable> = await getTeamList({});
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
            <Icon icon="ant-design:setting-outlined" />
            <Select value={teamId} onChange={(value: string) => (teamId.value = value)}>
              {teamList.map((team: Recordable) => {
                return <Select.Option value={team.id}>{team.teamName}</Select.Option>;
              })}
            </Select>
          </div>
        );
      },
      onOk: () => {
        if (!teamId.value) {
          createMessage.warning('please select a team');
          return Promise.reject();
        }
        userStore.setTeamId(teamId.value);
        return Promise.resolve();
      },
    });
  }
  return { createTeamModal };
};
