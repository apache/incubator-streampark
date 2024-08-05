import { Persistent, BasicKeys } from '/@/utils/cache/persistent';
import { CacheTypeEnum } from '/@/enums/cacheEnum';
import projectSetting from '/@/settings/projectSetting';
import { TOKEN_KEY } from '/@/enums/cacheEnum';

const { permissionCacheType } = projectSetting;
const isLocal = permissionCacheType === CacheTypeEnum.LOCAL;

export function getToken() {
  return getAuthCache(TOKEN_KEY);
}

export function getAuthCache<T>(key: BasicKeys) {
  if (isLocal) {
    return Persistent.getLocal(key) as T;
  } else {
    const sessionCacheValue = Persistent.getSession(key) as T;
    const localCacheValue = Persistent.getLocal(key) as T;
    if (!sessionCacheValue && localCacheValue) {
      Persistent.setSession(key, localCacheValue, true);
      return localCacheValue;
    }
    return sessionCacheValue;
  }
}

export function setAuthCache(key: BasicKeys, value: any, expire?: number) {
  if (isLocal) {
    return Persistent.setLocal(key, value, true, expire);
  } else {
    Persistent.setLocal(key, value, true, expire);
    return Persistent.setSession(key, value, true, expire);
  }
}

export function clearAuthCache(immediate = true) {
  if (isLocal) {
    return Persistent.clearLocal(immediate);
  } else {
    Persistent.clearLocal(immediate);
    return Persistent.clearSession(immediate);
  }
}
