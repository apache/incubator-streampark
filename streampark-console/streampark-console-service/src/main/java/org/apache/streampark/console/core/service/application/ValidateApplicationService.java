package org.apache.streampark.console.core.service.application;

import org.apache.streampark.console.base.exception.ApplicationException;
import org.apache.streampark.console.core.entity.Application;

import com.baomidou.mybatisplus.extension.service.IService;

/** Application validation service */
public interface ValidateApplicationService extends IService<Application> {

  // region check application
  boolean checkEnv(Application app) throws ApplicationException;

  boolean checkAlter(Application application);

  boolean existsRunningJobByClusterId(Long clusterId);

  boolean existsByTeamId(Long teamId);

  boolean existsJobByClusterId(Long id);

  boolean existsByJobName(String jobName);

  boolean existsJobByFlinkEnvId(Long flinkEnvId);
  // endregion

}
