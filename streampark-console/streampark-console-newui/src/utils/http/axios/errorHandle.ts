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
import { AxiosResponse } from 'axios';
import { SessionTimeoutProcessingEnum } from '/@/enums/appEnum';
import { useMessage } from '/@/hooks/web/useMessage';
import projectSetting from '/@/settings/projectSetting';
import { useUserStore } from '/@/store/modules/user';

export function errorHandler(response: AxiosResponse<any>) {
  const { Swal, notification } = useMessage();
  const stp = projectSetting.sessionTimeoutProcessing;

  if (response) {
    switch (response?.data?.code) {
      case 501:
        Swal.fire({
          icon: 'error',
          title: 'Oops...',
          text: response.data.message,
          footer: '<a href="https://streampark.apache.org/">View the official documentation?</a>',
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
          title: 'Oops...',
          width: width,
          html: '<pre class="propException">' + response.data.message + '</pre>',
          footer:
            '<a href="https://github.com/apache/incubator-streampark/issues/new/choose">report issue ?</a>',
          focusConfirm: false,
        });
        break;
      default:
        const errorMessage =
          response.data === null
            ? 'System error，Please contact the administrator'
            : response.data.message;
        switch (response.status) {
          case 404:
            notification.error({
              message: 'Sorry, resource not found',
              duration: 4,
            });
            break;
          case 403:
          case 401:
            const userStore = useUserStore();
            userStore.setToken(undefined);
            notification.warn({
              message:
                "Sorry, you can't access. May be because you don't have permissions or the Sign In is invalid",
              duration: 4,
            });
            if (stp === SessionTimeoutProcessingEnum.PAGE_COVERAGE) {
              userStore.setSessionTimeout(true);
            } else {
              userStore.logout(true);
            }
            break;
          default:
            notification.error({
              message: errorMessage,
              duration: 4,
            });
            break;
        }
        break;
    }
  }
}
