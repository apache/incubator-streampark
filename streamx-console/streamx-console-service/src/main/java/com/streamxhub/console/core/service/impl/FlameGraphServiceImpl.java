package com.streamxhub.console.core.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.streamxhub.console.base.domain.RestResponse;
import com.streamxhub.console.base.utils.CommonUtil;
import com.streamxhub.console.base.utils.WebUtil;
import com.streamxhub.console.core.dao.FlameGraphMapper;
import com.streamxhub.console.core.entity.FlameGraph;
import com.streamxhub.console.core.service.FlameGraphService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * @author benjobs
 */
@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class FlameGraphServiceImpl extends ServiceImpl<FlameGraphMapper, FlameGraph> implements FlameGraphService {

    @Override
    public RestResponse generateFlameGraph(FlameGraph flameGraph) throws IOException {
        List<FlameGraph> flameGraphList = this.baseMapper.getFlameGraph(flameGraph.getAppId(), flameGraph.getStart(), flameGraph.getEnd());
        if (CommonUtil.notEmpty(flameGraphList)) {
            StringBuffer jsonBuffer = new StringBuffer();
            flameGraphList.forEach(x -> jsonBuffer.append(x.getContent()));

            String jsonPath = WebUtil.getAppDir("temp").concat(File.separator).concat(flameGraph.getFlameGraphJsonName());
            FileOutputStream fileOutputStream = new FileOutputStream(jsonPath);
            IOUtils.write(jsonBuffer.toString().getBytes(), fileOutputStream);
        }
        return null;
    }
}
