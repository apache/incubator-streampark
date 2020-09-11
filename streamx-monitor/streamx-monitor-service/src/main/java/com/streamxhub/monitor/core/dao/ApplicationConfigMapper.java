package com.streamxhub.monitor.core.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.streamxhub.monitor.core.entity.ApplicationConfig;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface ApplicationConfigMapper extends BaseMapper<ApplicationConfig> {

    @Select("select max(`version`) as lastVersion from t_flink_config where app_id=#{appId}")
    Integer getLastVersion(@Param("appId") Long appId);

    @Update("update t_flink_config set actived = 0 where app_id=#{appId} and actived = 1")
    void standby(@Param("appId") Long id);

    @Select("select * from t_flink_config where app_id=#{appId} and actived = 1")
    ApplicationConfig getActived(@Param("appId")Long id);

    @Update("update t_flink_config set actived = 1 where app_id=#{appId} and version = #{version}")
    void active(@Param("appId")Long appId,@Param("version") Long version);
}
