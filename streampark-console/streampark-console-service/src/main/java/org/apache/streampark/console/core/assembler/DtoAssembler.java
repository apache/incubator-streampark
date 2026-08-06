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

package org.apache.streampark.console.core.assembler;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.BeanUtils;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Generic bean-copy helpers for request/response DTO mapping. */
public final class DtoAssembler {

    private DtoAssembler() {
    }

    public static <S, T> T toDto(S source, Class<T> targetClass) {
        if (source == null) {
            return null;
        }
        try {
            T target = targetClass.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(source, target);
            return target;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to copy properties to " + targetClass.getName(), e);
        }
    }

    public static <S, T> T map(S source, Function<S, T> mapper) {
        return source == null ? null : mapper.apply(source);
    }

    public static <S, T> List<T> toList(List<S> sources, Function<S, T> mapper) {
        if (sources == null) {
            return Collections.emptyList();
        }
        return sources.stream().map(mapper).collect(Collectors.toList());
    }

    public static <S, T> IPage<T> toPage(IPage<S> page, Function<S, T> mapper) {
        if (page == null) {
            return null;
        }
        Page<T> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(toList(page.getRecords(), mapper));
        return result;
    }

    public static <S, T> void copy(S source, T target) {
        if (source != null && target != null) {
            BeanUtils.copyProperties(source, target);
        }
    }
}
