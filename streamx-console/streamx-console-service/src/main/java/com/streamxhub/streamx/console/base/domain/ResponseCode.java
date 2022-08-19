/*
 * Copyright 2019 The StreamX Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.console.base.domain;

/**
 * @author xianwei.yang
 * 业务响应code
 */
public interface ResponseCode {

    /**
     * default
     */
    Long CODE_SUCCESS = 200L;

    Long CODE_FAIL = 500L;

    Long CODE_API_FAIL = 501L;

}
