/**
 * Copyright (c) 2019 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.streamxhub.console.core.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.streamxhub.common.util.CommandUtils;
import com.streamxhub.console.base.utils.CommonUtil;
import com.streamxhub.console.base.utils.WebUtil;
import com.streamxhub.console.core.dao.FlameGraphMapper;
import com.streamxhub.console.core.entity.Application;
import com.streamxhub.console.core.entity.FlameGraph;
import com.streamxhub.console.core.service.ApplicationService;
import com.streamxhub.console.core.service.FlameGraphService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import scala.Function1;
import scala.runtime.BoxedUnit;

import java.io.*;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

/**
 * @author benjobs
 */
@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class FlameGraphServiceImpl extends ServiceImpl<FlameGraphMapper, FlameGraph> implements FlameGraphService {

    @Autowired
    private ApplicationService applicationService;

    @Override
    public String generateFlameGraph(FlameGraph flameGraph) throws IOException {
        List<FlameGraph> flameGraphList = this.baseMapper.getFlameGraph(flameGraph.getAppId(), flameGraph.getStart(), flameGraph.getEnd());
        if (CommonUtil.notEmpty(flameGraphList)) {
            StringBuffer jsonBuffer = new StringBuffer();
            flameGraphList.forEach(x -> jsonBuffer.append(x.getUnzipContent()).append("\r\n"));

            Application application = applicationService.getById(flameGraph.getAppId());
            String jsonName = String.format("%d_%d_%d.json", flameGraph.getAppId(), flameGraph.getStart().getTime(), flameGraph.getEnd().getTime());
            String jsonPath = WebUtil.getAppDir("temp").concat(File.separator).concat(jsonName);
            String foldedPath = jsonPath.replace(".json", ".folded");
            String svgPath = jsonPath.replace(".json", ".svg");
            String flameGraphPath = WebUtil.getAppDir("bin/flame-graph");

            //write json
            FileOutputStream fileOutputStream = new FileOutputStream(jsonPath);
            IOUtils.write(jsonBuffer.toString().getBytes(), fileOutputStream);

            String title = application.getJobName().concat(" ___ FlameGraph");
            //generate...
            List<String> commands = Arrays.asList(
                    String.format("cd %s", flameGraphPath),
                    String.format("python ./stackcollapse.py -i %s > %s ", jsonPath, foldedPath),
                    String.format("./flamegraph.pl --title=\"%s\" --width=%d --colors=java %s > %s ", title, flameGraph.getWidth(), foldedPath, svgPath)
            );
            CommandUtils.execute(commands, (line) -> log.info("[StreamX] flameGraph: {} ", line));
            return svgPath;
        }
        return null;
    }

    @Override
    public void clean(Date end) {
        baseMapper.clean(end);
    }
}
