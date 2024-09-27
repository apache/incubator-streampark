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

package org.apache.streampark.console.core.service.impl;

import org.apache.streampark.common.util.AssertUtils;
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.core.entity.ExternalLink;
import org.apache.streampark.console.core.entity.FlinkApplication;
import org.apache.streampark.console.core.enums.PlaceholderTypeEnum;
import org.apache.streampark.console.core.mapper.ExternalLinkMapper;
import org.apache.streampark.console.core.service.ExternalLinkService;
import org.apache.streampark.console.core.service.application.FlinkApplicationManageService;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.PropertyPlaceholderHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class ExternalLinkServiceImpl extends ServiceImpl<ExternalLinkMapper, ExternalLink>
    implements
        ExternalLinkService {

    private final FlinkApplicationManageService applicationManageService;

    @Override
    public void create(ExternalLink externalLink) {
        if (!this.check(externalLink)) {
            return;
        }
        externalLink.setId(null);
        this.save(externalLink);
    }

    @Override
    public void update(ExternalLink externalLink) {
        if (!this.check(externalLink)) {
            return;
        }
        baseMapper.updateById(externalLink);
    }

    @Override
    public void removeById(Long linkId) {
        baseMapper.deleteById(linkId);
    }

    @Override
    public List<ExternalLink> render(Long appId) {
        FlinkApplication app = applicationManageService.getById(appId);
        AssertUtils.notNull(app, "Application doesn't exist");
        List<ExternalLink> externalLink = this.list();
        if (externalLink != null && externalLink.size() > 0) {
            // Render the placeholder
            externalLink.forEach(link -> this.renderLinkUrl(link, app));
        }
        return externalLink;
    }

    private void renderLinkUrl(ExternalLink link, FlinkApplication app) {
        Map<String, String> placeholderValueMap = new HashMap<>();
        placeholderValueMap.put(PlaceholderTypeEnum.ID.get(), String.valueOf(app.getId()));
        placeholderValueMap.put(PlaceholderTypeEnum.JOB_ID.get(), app.getJobId());
        placeholderValueMap.put(PlaceholderTypeEnum.JOB_NAME.get(), app.getJobName());
        placeholderValueMap.put(PlaceholderTypeEnum.YARN_ID.get(), app.getClusterId());
        PropertyPlaceholderHelper propertyPlaceholderHelper = new PropertyPlaceholderHelper("{", "}");
        link.setRenderedLinkUrl(
            propertyPlaceholderHelper.replacePlaceholders(
                link.getLinkUrl().trim(), placeholderValueMap::get));
    }

    private boolean check(ExternalLink params) {
        LambdaQueryWrapper<ExternalLink> queryWrapper = new LambdaQueryWrapper<ExternalLink>();
        // badgeName and LinkUrl cannot be duplicated
        queryWrapper.nested(
            qw -> qw.eq(ExternalLink::getBadgeName, params.getBadgeName())
                .or()
                .eq(ExternalLink::getLinkUrl, params.getLinkUrl()));
        if (params.getId() != null) {
            queryWrapper.and(qw -> qw.ne(ExternalLink::getId, params.getId()));
        }
        ExternalLink result = this.getOne(queryWrapper);
        if (result == null) {
            return true;
        }
        ApiAlertException.throwIfTrue(result.getBadgeName().equals(params.getBadgeName()),
            String.format("The name: %s is already existing.", result.getBadgeName()));
        ApiAlertException.throwIfTrue(result.getLinkUrl().equals(params.getLinkUrl()),
            String.format("The linkUrl: %s is already existing.", result.getLinkUrl()));

        return false;
    }
}
