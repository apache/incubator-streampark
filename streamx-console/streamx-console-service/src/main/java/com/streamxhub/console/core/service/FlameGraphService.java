package com.streamxhub.console.core.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.streamxhub.console.base.domain.RestResponse;
import com.streamxhub.console.core.entity.FlameGraph;
import org.junit.runners.model.InitializationError;

import java.io.File;
import java.io.IOException;

/**
 * @author benjobs
 */
public interface FlameGraphService extends IService<FlameGraph> {
    String generateFlameGraph(FlameGraph flameGraph) throws IOException;
}
