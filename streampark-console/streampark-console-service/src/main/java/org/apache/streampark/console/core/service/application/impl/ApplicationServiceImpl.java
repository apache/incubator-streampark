package org.apache.streampark.console.core.service.application.impl;

import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.service.application.ApplicationService;
import org.apache.streampark.console.core.service.application.deploy.K8sApplicationService;
import org.apache.streampark.console.core.service.application.deploy.YarnApplicationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.apache.streampark.console.core.task.FlinkK8sWatcherWrapper.isKubernetesApp;

@Slf4j
@Service("streamApplicationService")
@RequiredArgsConstructor
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class ApplicationServiceImpl implements ApplicationService {

  private final YarnApplicationService yarnApplicationService;

  private final K8sApplicationService k8sApplicationService;

  @Override
  public void start(Application application, boolean auto) throws Exception {
    getApplicationService(application).start(application, auto);
  }

  @Override
  public void restart(Application application) throws Exception {
    getApplicationService(application).restart(application);
  }

  @Override
  public void cancel(Application application) throws Exception {
    getApplicationService(application).cancel(application);
  }

  /**
   * get application service by application type
   *
   * @param application application
   * @return ApplicationService
   */
  private ApplicationService getApplicationService(Application application) {
    return isKubernetesApp(application) ? k8sApplicationService : yarnApplicationService;
  }
}
