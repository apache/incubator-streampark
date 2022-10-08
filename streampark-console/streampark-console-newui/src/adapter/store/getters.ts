import { state } from './modules/user';

const getters = {
  get expire() {
    return state.expire;
  },
};

export default getters;
