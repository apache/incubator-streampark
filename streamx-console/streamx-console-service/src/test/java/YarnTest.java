import com.streamxhub.streamx.common.util.HadoopUtils;
import com.streamxhub.streamx.common.util.PropertiesUtils;
import org.apache.hadoop.yarn.api.records.NodeState;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.fusesource.jansi.Ansi.Color.*;
import static org.fusesource.jansi.Ansi.ansi;

public class YarnTest {

    @Test
    public void vCore() throws IOException, YarnException {
        int numYarnMaxVcores = HadoopUtils.yarnClient().getNodeReports(NodeState.RUNNING)
            .stream()
            .mapToInt(report -> report.getCapability().getVirtualCores())
            .max()
            .orElse(0);
        System.out.println(numYarnMaxVcores);
    }

    @Test
    public void getURL() {
        /**
         * 将hadoop的配置文件放到一个目录下,
         * 在运行该类的时候加上jvm级别的参数(idea里的 vmOption ) -DHADOOP_CONF_DIR=${目录}
         */
        String url = HadoopUtils.getRMWebAppURL(true);
        System.out.println(url);
    }


    @Test
    public void loadFlinkYaml() {
        String path = System.getenv("FLINK_HOME").concat("/conf/flink-conf.yaml");
        File yaml = new File(path);
        Map<String, String> map = PropertiesUtils.loadFlinkConfYaml(yaml);
        System.out.println(map.size());
    }

    @Test
    public void logo() {
        System.out.println(ansi().eraseScreen().fg(YELLOW).a("\n\n              .+.                          ").fg(RED).a("       ").reset());
        System.out.println(ansi().eraseScreen().fg(YELLOW).a("        _____/ /_________  ____ _____ ___ ").fg(RED).a(" _  __").reset());
        System.out.println(ansi().eraseScreen().fg(YELLOW).a("       / ___/ __/ ___/ _ \\/ __ `/ __ `__ \\").fg(RED).a("| |/_/").reset());
        System.out.println(ansi().eraseScreen().fg(YELLOW).a("      (__  ) /_/ /  /  __/ /_/ / / / / / /").fg(RED).a(">  <  ").reset());
        System.out.println(ansi().eraseScreen().fg(YELLOW).a("     /____/\\__/_/   \\___/\\__,_/_/ /_/ /_/").fg(RED).a("_/|_|  ").reset());
        System.out.println(ansi().eraseScreen().fg(YELLOW).a("                                         ").fg(RED).a("  |/   ").reset());
        System.out.println(ansi().eraseScreen().fg(YELLOW).a("                                         ").fg(RED).a("  .    ").reset());
        System.out.println("\n   WebSite:  http://www.streamxhub.com            ");
        System.out.println("   GitHub :  https://github.com/streamxhub/streamx");
        System.out.println("   Gitee  :  https://gitee.com/streamxhub/streamx    ");
        System.out.println("   Ver    :  1.2.0                                ");
        System.out.println("                                                  ");
        System.out.println(ansi().eraseScreen().fg(GREEN).a("   [StreamX] Make Flink|Spark easier ô‿ô!         ").reset());
    }

}
