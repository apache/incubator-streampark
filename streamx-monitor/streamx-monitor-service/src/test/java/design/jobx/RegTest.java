package design.jobx;

import java.util.Optional;

public class RegTest {
    public static void main(String[] args) {
        String uploadFormat = ".*tar\\.gz$|.*jar$";

        String file = "flink.tar.gz";
        System.out.println(file.matches(uploadFormat));

        String application = null;

        String[] array =  Optional.of(application.split("\\s+")).orElse(new String[0]);
        System.out.println(array);

    }
}
