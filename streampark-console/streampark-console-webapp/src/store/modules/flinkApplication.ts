import { defineStore } from 'pinia';

interface FlinkApplicationState {
  appId: Nullable<string>;
}
export const useFlinkAppStore = defineStore({
  id: 'flink-application',
  state: (): FlinkApplicationState => ({
    appId: null,
  }),
  getters: {
    getApplicationId(): Nullable<string> {
      return this.appId;
    },
  },
  actions: {
    setApplicationId(appId: string): void {
      this.appId = appId;
    },
    clearApplicationId() {
      this.appId = null;
    },
  },
});
