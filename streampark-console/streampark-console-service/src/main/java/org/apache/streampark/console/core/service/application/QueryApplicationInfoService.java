package org.apache.streampark.console.core.service.application;

import org.apache.streampark.common.enums.ExecutionMode;
import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.enums.AppExistsState;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/** Application info query service */
public interface QueryApplicationInfoService extends IService<Application> {

  AppExistsState checkExists(Application app);

  String checkSavepointPath(Application app) throws Exception;

  String readConf(Application app) throws IOException;

  Application getApp(Application app);

  String getMain(Application application);

  IPage<Application> page(Application app, RestRequest request);

  Map<String, Serializable> dashboard(Long teamId);

  List<Application> getByProjectId(Long id);

  List<Application> getByTeamId(Long teamId);

  List<Application> getByTeamIdAndExecutionModes(
      Long teamId, Collection<ExecutionMode> executionModes);

  String getSavePointed(Application appParam);
}
