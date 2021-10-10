package com.streamxhub.streamx.console.core.entity;


import com.baomidou.mybatisplus.annotation.TableName;
import com.streamxhub.streamx.common.util.CommandUtils;
import com.streamxhub.streamx.common.util.DeflaterUtils;
import com.streamxhub.streamx.common.util.PropertiesUtils;
import lombok.Data;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author benjobs
 */
@Data
@TableName("t_flink_version")
public class FlinkVersion {

    private Long id;

    private String name;

    private String description;

    private String flinkHome;

    private String version;

    private String flinkConf;

    /**
     * 是否为默认版本.
     */
    private Boolean isDefault;

    private Date createTime;

    private transient final Pattern flinkVersionPattern = Pattern.compile("^Version: (.*), Commit ID: (.*)$");

    public String getFlinkVersion(String path) {
        assert path != null;
        AtomicReference<String> flinkVersion = new AtomicReference<>();
        if (path != null) {
            String libPath = path.concat("/lib");
            File[] distJar = new File(libPath).listFiles(x -> x.getName().matches("flink-dist_.*\\.jar"));
            if (distJar == null || distJar.length == 0) {
                throw new IllegalArgumentException("[StreamX] can no found flink-dist jar in " + libPath);
            }
            if (distJar.length > 1) {
                throw new IllegalArgumentException("[StreamX] found multiple flink-dist jar in " + libPath);
            }
            List<String> cmd = Arrays.asList(
                "cd ".concat(path),
                String.format(
                    "java -classpath %s org.apache.flink.client.cli.CliFrontend --version",
                    distJar[0].getAbsolutePath()
                )
            );

            CommandUtils.execute(cmd, versionInfo -> {
                Matcher matcher = flinkVersionPattern.matcher(versionInfo);
                if (matcher.find()) {
                    flinkVersion.set(matcher.group(1));
                }
            });
        }
        return flinkVersion.get();
    }

    public void setFlinkHome(String flinkHome) {
        this.flinkHome = flinkHome;
        this.version = getFlinkVersion(flinkHome);
    }

    public void doSetFlinkConf() throws IOException {
        assert this.flinkHome != null;
        File yaml = new File(this.flinkHome.concat("/conf/flink-conf.yaml"));
        assert yaml.exists();
        String flinkConf = FileUtils.readFileToString(yaml);
        this.flinkConf = DeflaterUtils.zipString(flinkConf);
    }

    public void doSetVersion() throws IOException {
        assert this.flinkHome != null;
        this.setVersion(this.getFlinkVersion(this.getFlinkHome()));
    }

    public Map<String, String> getFlinkYamlMap() {
        String flinkYamlString = DeflaterUtils.unzipString(flinkConf);
        return PropertiesUtils.loadFlinkConfYaml(flinkYamlString);
    }

}
