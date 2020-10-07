import com.streamxhub.common.util.HdfsUtils.getDefaultFS

object HdfsTestApp {

  def main(args: Array[String]): Unit = {
      print(getDefaultFS)
  }
}
