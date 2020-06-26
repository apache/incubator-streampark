package design.jobx;

public class RegTest {
    public static void main(String[] args) {
        String uploadFormat = ".*tar\\.gz$|.*jar$";

        String file = "flink.tar.gz";
        System.out.println(file.matches(uploadFormat));
    }
}
