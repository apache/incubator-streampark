
import java.util.{Calendar, Date, TimeZone}

object HdfsTestApp {

  def main(args: Array[String]): Unit = {
    val start = new Date
    val cal = Calendar.getInstance
    cal.setTimeZone(TimeZone.getDefault)
    cal.setTime(start)
    cal.add(Calendar.HOUR_OF_DAY, -24)
    val end = cal.getTime
    println(end)
  }
}
