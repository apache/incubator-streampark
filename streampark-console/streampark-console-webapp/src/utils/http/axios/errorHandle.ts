import { AxiosResponse } from 'axios';
import { useI18n } from '/@/hooks/web/useI18n';
import { SessionTimeoutProcessingEnum } from '/@/enums/appEnum';
import { useMessage } from '/@/hooks/web/useMessage';
import projectSetting from '/@/settings/projectSetting';
import { useUserStore } from '/@/store/modules/user';

export function errorHandler(response: AxiosResponse<any>) {
  const { Swal, notification } = useMessage();
  const { t } = useI18n();
  const stp = projectSetting.sessionTimeoutProcessing;
  if (response) {
    const code = parseInt(response?.data?.code);
    switch (code) {
      case 501:
        Swal.fire({
          icon: 'error',
          title: t('sys.api.errorTip'),
          text: response.data.message,
          footer: t('sys.api.error501'),
        });
        break;
      case 502:
        let width = document.documentElement.clientWidth || document.body.clientWidth;
        if (width > 1200) {
          width = 1080;
        }
        width *= 0.96;
        Swal.fire({
          icon: 'error',
          title: t('sys.api.errorTip'),
          width: width,
          html: '<pre class="api-exception">' + response.data.message + '</pre>',
          footer: t('sys.api.error502'),
          focusConfirm: false,
        });
        break;
      default:
        const errorMessage = response.data === null ? t('sys.api.errorMsg') : response.data.message;
        switch (response.status) {
          case 404:
            notification.error({
              message: t('sys.api.error404'),
              duration: 4,
            });
            break;
          case 403:
          case 401:
            const userStore = useUserStore();
            userStore.setToken(undefined);
            if (stp === SessionTimeoutProcessingEnum.PAGE_COVERAGE) {
              userStore.setSessionTimeout(true);
            } else {
              userStore.logout(true);
            }
            setTimeout(() => {
              notification.warn({
                message: t('sys.api.error403'),
                duration: 4,
              });
            }, 500);
            break;
          default:
            notification.error({
              message: errorMessage || t('sys.api.networkExceptionMsg'),
              duration: 3,
            });
            break;
        }
        break;
    }
  }
}
