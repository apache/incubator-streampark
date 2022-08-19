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

package com.streamxhub.streamx.console.core.service.alert;

import com.streamxhub.streamx.console.base.domain.RestRequest;
import com.streamxhub.streamx.console.base.exception.AlertException;
import com.streamxhub.streamx.console.core.entity.alert.AlertConfig;
import com.streamxhub.streamx.console.core.entity.alert.AlertConfigWithParams;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @author weijinglun
 * @date 2022.01.14
 */
public interface AlertConfigService extends IService<AlertConfig> {
    IPage<AlertConfigWithParams> page(AlertConfigWithParams params, RestRequest request);

    boolean exist(AlertConfig alertConfig);

    boolean deleteById(Long id) throws AlertException;
}
