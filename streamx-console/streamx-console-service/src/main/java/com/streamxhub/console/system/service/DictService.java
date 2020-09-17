package com.streamxhub.console.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.streamxhub.console.base.domain.RestRequest;
import com.streamxhub.console.system.entity.Dict;


public interface DictService extends IService<Dict> {

    IPage<Dict> findDicts(RestRequest request, Dict dict);

    void createDict(Dict dict);

    void updateDict(Dict dicdt);

    void deleteDicts(String[] dictIds);

}
