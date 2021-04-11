package com.streamxhub.streamx.console.core.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.streamxhub.streamx.console.core.dao.SettingMapper;
import com.streamxhub.streamx.console.core.entity.Setting;
import com.streamxhub.streamx.console.core.service.SettingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author benjobs
 */
@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class SettingServiceImpl extends ServiceImpl<SettingMapper, Setting>
        implements SettingService {

    @Override
    public Setting get(String key) {
        return baseMapper.get(key);
    }

    private volatile Map<String, Setting> settings = new ConcurrentHashMap<>();

    @PostConstruct
    public void initSetting() {
        List<Setting> settingList = super.list();
        settingList.forEach(x -> settings.put(x.getKey(), x));
    }

    @Override
    public boolean update(Setting setting) {
        try {
            this.baseMapper.updateByKey(setting);
            settings.get(setting.getKey()).setValue(setting.getValue());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getStreamXWorkspace() {
        return settings.get(SettingService.KEY_STREAMX_WORKSPACE).getValue();
    }

    @Override
    public String getStreamXAddress() {
        return settings.get(SettingService.KEY_STREAMX_ADDRESS).getValue();
    }

    @Override
    public String getMavenRepository() {
        return settings.get(SettingService.KEY_MAVEN_REPOSITORY).getValue();
    }
}
