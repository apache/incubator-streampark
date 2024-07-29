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
import { defHttp } from '/@/utils/http/axios';
import { LoginParams, LoginResultModel } from './model/userModel';

import { ErrorMessageMode, Result } from '/#/axios';

enum Api {
  SIGN_IN = '/passport/signin',
  SIGN_OUT = '/passport/signout',
  SIGN_TYPE = '/passport/signtype',
  SSO_TOKEN = '/sso/token',
}

/**
 * @description: user login api
 * @return {Promise<AxiosResponse<Result<LoginResultModel>>>}
 */
export function signin(
  data: LoginParams,
  mode: ErrorMessageMode = 'modal',
): Promise<AxiosResponse<Result<LoginResultModel>>> {
  return defHttp.post({ url: Api.SIGN_IN, data }, { onlyData: false, errorMessageMode: mode });
}

export function signout() {
  return defHttp.post({ url: Api.SIGN_OUT });
}

export function fetchSignType(): Promise<String[]> {
  return defHttp.post({ url: Api.SIGN_TYPE });
}

export function fetchSsoToken(): Promise<LoginResultModel> {
  return defHttp.get(
    {
      url: Api.SSO_TOKEN,
    },
    {
      errorMessageMode: 'none',
      retryRequest: {
        isOpenRetry: false,
        count: 1,
        waitTime: 100,
      },
    },
  );
}
