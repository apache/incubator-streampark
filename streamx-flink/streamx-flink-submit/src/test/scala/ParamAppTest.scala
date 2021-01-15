import org.apache.flink.api.java.utils.ParameterTool

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
  val param = ParameterTool.fromArgs(arg).mergeWith(ParameterTool.fromArgs(argsx))
  println(param)
}
