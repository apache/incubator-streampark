/*
 * Copyright (c) 2019 The StreamX Project
 *
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

package com.streamxhub.streamx.console.core.service.impl;

import com.streamxhub.streamx.common.util.CommandUtils;
import com.streamxhub.streamx.console.base.util.CommonUtils;
import com.streamxhub.streamx.console.base.util.WebUtils;
import com.streamxhub.streamx.console.core.dao.FlameGraphMapper;
import com.streamxhub.streamx.console.core.entity.Application;
import com.streamxhub.streamx.console.core.entity.FlameGraph;
import com.streamxhub.streamx.console.core.service.ApplicationService;
import com.streamxhub.streamx.console.core.service.FlameGraphService;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @author benjobs
 */
@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class FlameGraphServiceImpl extends ServiceImpl<FlameGraphMapper, FlameGraph>
        implements FlameGraphService {

    @Autowired
    private ApplicationService applicationService;

    @Override
    public String generateFlameGraph(FlameGraph flameGraph) throws IOException {
        List<FlameGraph> flameGraphList = this.baseMapper.getFlameGraph(
                flameGraph.getAppId(),
                flameGraph.getStart(),
                flameGraph.getEnd()
        );
        if (CommonUtils.notEmpty(flameGraphList)) {
            StringBuffer jsonBuffer = new StringBuffer();
            flameGraphList.forEach(x -> jsonBuffer.append(x.getUnzipContent()).append("\r\n"));

            Application application = applicationService.getById(flameGraph.getAppId());
            String jsonName = String.format(
                    "%d_%d_%d.json",
                    flameGraph.getAppId(),
                    flameGraph.getStart().getTime(),
                    flameGraph.getEnd().getTime()
            );
            String jsonPath = new File(WebUtils.getAppTempDir(), jsonName).getAbsolutePath();
            String foldedPath = jsonPath.replace(".json", ".folded");
            String svgPath = jsonPath.replace(".json", ".svg");
            File flameGraphPath = WebUtils.getAppDir("bin/flame-graph");

            // write json
            FileOutputStream fileOutputStream = new FileOutputStream(jsonPath);
            IOUtils.write(jsonBuffer.toString().getBytes(), fileOutputStream);

            String title = application.getJobName().concat(" ___ FlameGraph");
            // generate...
            List<String> commands = Arrays.asList(
                    String.format("python ./stackcollapse.py -i %s > %s ", jsonPath, foldedPath),
                    String.format(
                            "./flamegraph.pl --title=\"%s\" --width=%d --colors=java %s > %s ",
                            title,
                            flameGraph.getWidth(),
                            foldedPath,
                            svgPath
                    )
            );
            CommandUtils.execute(flameGraphPath.getAbsolutePath(), commands, (line) -> log.info("flameGraph: {} ", line));
            return svgPath;
        }
        return null;
    }

    @Override
    public void clean(Date end) {
        baseMapper.clean(end);
    }
}
