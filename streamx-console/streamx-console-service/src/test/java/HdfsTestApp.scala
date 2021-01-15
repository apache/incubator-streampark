
import java.util.{Calendar, Date, TimeZone}

object HdfsTestApp {

  def main(args: Array[String]): Unit = {
    //"\\d+_\\d+\\.json|\\d+_\\d+\\.folded|\\d+_\\d+\\.svg"

    val name = "12434234_5454.svg"

    if (name.matches("\\d+_\\d+\\.json|\\d+_\\d+\\.folded|\\d+_\\d+\\.svg")) {
      println("rrrrr")
    }

  }
}
