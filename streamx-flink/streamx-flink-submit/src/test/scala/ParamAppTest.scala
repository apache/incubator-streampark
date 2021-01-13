import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.runtime.jobmanager.JobManagerProcessUtils

object ParamAppTest extends App {
  val arg = Array(
    "--flink.deployment.option.parallelism",
    "10"
  )
  val argsx = Array(
    "--flink.home",
    "hdfs://nameservice1/streamx/flink/flink-1.11.1",
    "--app.name",
    "testApp123",
    "--flink.deployment.option.parallelism",
    "5"
  )
  JobManagerProcessUtils
  val param = ParameterTool.fromArgs(arg).mergeWith(ParameterTool.fromArgs(argsx))
  println(param)
}
