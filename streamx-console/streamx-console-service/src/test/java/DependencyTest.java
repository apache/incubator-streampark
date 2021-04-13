import com.streamxhub.streamx.common.util.DependencyUtils;
import com.streamxhub.streamx.console.core.entity.Application;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import scala.Option;

import java.util.*;

import scala.collection.JavaConversions;

@Slf4j
public class DependencyTest {

    @Test
    public void resolveMavenDependencies() throws Throwable {
        /**
         * <dependency>
         *      <groupId>org.apache.flink</groupId>
         *      <artifactId>flink-table-common</artifactId>
         *      <version>${flink.version}</version>
         * </dependency>
         *
         * <dependency>
         *     <groupId>org.apache.flink</groupId>
         *     <artifactId>flink-java</artifactId>
         *     <version>${flink.version}</version>
         * </dependency>
         *
         */

        List<Application.Pom> dependency = new ArrayList<>();

        Application.Pom dept = new Application.Pom();
        dept.setGroupId("org.apache.flink");
        dept.setArtifactId("flink-java");
        dept.setVersion("1.11.x");
        dependency.add(dept);

        StringBuilder builder = new StringBuilder();
        dependency.forEach(x -> {
            String info = String.format("%s:%s:%s,", x.getGroupId(), x.getArtifactId(), x.getVersion());
            builder.append(info);
        });
        String packages = builder.deleteCharAt(builder.length() - 1).toString();

        builder.setLength(0);
        builder.append("org.apache.flink:force-shading,")
                .append("com.google.code.findbugs:jsr305,")
                .append("org.slf4j:*,")
                .append("org.apache.logging.log4j:*");

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                log.info(">>>>> running....");
            }
        }, 0, 3000);

        try {
            Collection<String> jars = JavaConversions.asJavaCollection(
                    DependencyUtils.resolveMavenDependencies(
                            builder.toString(),
                            packages,
                            null,
                            null,
                            null,
                            out -> {
                                System.err.println("---------->" + out);
                            }
                    )
            );
            System.out.println();
            System.out.println("----------------------------------------------------------------");
            jars.forEach(System.out::println);
        }catch (Exception e ) {
            System.out.println(e.getMessage());
        }
    }


}
