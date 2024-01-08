package org.apache.streampark.console.core.service.resource;

import org.apache.streampark.console.base.domain.RestResponse;
import org.apache.streampark.console.core.entity.Resource;

public interface ResourceHandle {

  RestResponse checkResource(Resource resourceParam) throws Exception;

  void handleResource(Resource resource) throws Exception;
}
