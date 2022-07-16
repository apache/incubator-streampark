package com.streamxhub.streamx.flink.core.test;

import org.apache.flink.table.functions.ScalarFunction;

/**
 * @author ziqiang.wang
 * @date 2021/12/27 18:34
 */
public class FetchMillisecond extends ScalarFunction {

    /**
     * 获取当前系统毫秒值
     *
     * @return 当前系统毫秒值
     */
    public Long eval() {
        return System.currentTimeMillis();
    }

    /**
     * 是否为确定值<br>
     * 返回true：表示该函数只在flink planner阶段执行一次，然后将执行结果返回发送给runtime，为固定值。<br>
     * 返回false：表示该函数是在运行期间执行，每行数据都会调用一次该函数，为不确定值。<br>
     * 注意：该函数默认返回ture，一般不需要重写该函数！！！
     *
     * @return 是否为确定值
     */
    @Override
    public boolean isDeterministic() {
        return false;
    }

}
