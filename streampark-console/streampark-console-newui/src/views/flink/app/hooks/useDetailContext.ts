import { InjectionKey } from 'vue';
import { AppListRecord } from '/@/api/flink/app/app.type';
import { createContext, useContext } from '/@/hooks/core/useContext';

export interface DetailProviderContextProps {
  app: Partial<AppListRecord>;
}

const key: InjectionKey<DetailProviderContextProps> = Symbol();

export function createDetailProviderContext(context: DetailProviderContextProps) {
  return createContext<DetailProviderContextProps>(context, key);
}

export function useDetailProviderContext() {
  return useContext<DetailProviderContextProps>(key);
}
