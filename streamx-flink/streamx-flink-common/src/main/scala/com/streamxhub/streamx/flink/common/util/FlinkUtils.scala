package com.streamxhub.streamx.flink.common.util

import org.apache.flink.api.common.state.{ListState, ListStateDescriptor}
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.runtime.state.FunctionInitializationContext

object FlinkUtils {

  def getUnionListState[R: TypeInformation](context: FunctionInitializationContext, descriptorName: String): ListState[R] = {
    context.getOperatorStateStore.getUnionListState(new ListStateDescriptor(descriptorName, implicitly[TypeInformation[R]].getTypeClass))
  }


}
