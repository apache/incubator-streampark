package com.streamxhub.console.core.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.streamxhub.console.core.entity.Tutorial;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * @author benjobs
 */
public interface TutorialMapper extends BaseMapper<Tutorial> {

    @Select("select * from t_flink_tutorial where name=#{name}")
    Tutorial getByName(@Param("name") String name);

}
