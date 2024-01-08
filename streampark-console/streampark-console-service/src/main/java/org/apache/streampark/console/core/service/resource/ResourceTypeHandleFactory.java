package org.apache.streampark.console.core.service.resource;

import org.apache.streampark.console.base.domain.RestResponse;
import org.apache.streampark.console.core.entity.Resource;
import org.apache.streampark.console.core.enums.ResourceTypeEnum;
import org.apache.streampark.console.core.service.impl.ResourceServiceImpl;
import org.apache.streampark.console.core.service.resource.impl.AbstractResourceHandle;
import org.apache.streampark.console.core.service.resource.impl.ConnectorResourceHandle;
import org.apache.streampark.console.core.service.resource.impl.FlinkAppResourceHandle;

import com.google.common.collect.ImmutableMap;

public class ResourceTypeHandleFactory {

  public static ResourceHandle getResourceHandle(
      ResourceTypeEnum type, ResourceServiceImpl resourceService) {
    switch (type) {
      case FLINK_APP:
        return new FlinkAppResourceHandle(resourceService);
      case CONNECTOR:
        return new ConnectorResourceHandle(resourceService);
      default:
        return new AbstractResourceHandle(resourceService) {
          @Override
          public RestResponse checkResource(Resource resourceParam) {
            return RestResponse.success().data(ImmutableMap.of(STATE, 0));
          }
        };
    }
  }
}
