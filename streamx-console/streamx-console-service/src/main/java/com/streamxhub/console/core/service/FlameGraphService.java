package com.streamxhub.console.core.service;

import java.io.IOException;
import java.util.Date;

import com.baomidou.mybatisplus.extension.service.IService;
import com.streamxhub.console.core.entity.FlameGraph;

/**
 * @author benjobs
 */
public interface FlameGraphService extends IService<FlameGraph> {
    /**
     * @param flameGraph
     * @return
     * @throws IOException
     */
    String generateFlameGraph(FlameGraph flameGraph) throws IOException;

    /**
     * @param end
     */
    void clean(Date end);
}
