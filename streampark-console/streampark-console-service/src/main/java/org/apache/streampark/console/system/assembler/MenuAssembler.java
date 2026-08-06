/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.streampark.console.system.assembler;

import org.apache.streampark.console.core.assembler.DtoAssembler;
import org.apache.streampark.console.system.entity.Menu;
import org.apache.streampark.console.system.request.menu.MenuListQueryRequest;
import org.apache.streampark.console.system.response.menu.MenuListResponse;
import org.apache.streampark.console.system.service.impl.MenuServiceImpl;

import java.util.List;
import java.util.Map;

/** Converts between menu entities and API request/response contracts. */
public final class MenuAssembler {

    private MenuAssembler() {
    }

    public static Menu toEntity(MenuListQueryRequest request) {
        return DtoAssembler.toDto(request, Menu.class);
    }

    @SuppressWarnings("unchecked")
    public static MenuListResponse toListResponse(Map<String, Object> menuMap) {
        if (menuMap == null) {
            return null;
        }
        MenuListResponse response = new MenuListResponse();
        response.setIds((List<String>) menuMap.get(MenuServiceImpl.IDS));
        Object total = menuMap.get(MenuServiceImpl.TOTAL);
        if (total instanceof Integer) {
            response.setTotal((Integer) total);
        } else if (total instanceof Long) {
            response.setTotal(((Long) total).intValue());
        }
        response.setRows(
            (org.apache.streampark.console.base.domain.router.RouterTree<?>) menuMap.get(MenuServiceImpl.ROWS));
        return response;
    }
}
