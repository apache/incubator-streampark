package com.streamxhub.console.core.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.streamxhub.console.core.entity.Tutorial;

/**
 * @author benjobs
 */
public interface TutorialService extends IService<Tutorial> {
    Tutorial getByName(String name);
}
