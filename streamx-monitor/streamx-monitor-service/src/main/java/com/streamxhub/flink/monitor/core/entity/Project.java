package com.streamxhub.flink.monitor.core.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.streamxhub.flink.monitor.base.properties.StreamXProperties;
import com.streamxhub.flink.monitor.base.utils.CommonUtil;
import com.streamxhub.flink.monitor.base.utils.SpringContextUtil;
import com.wuwenze.poi.annotation.Excel;
import lombok.Data;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.Serializable;
import java.util.Date;

/**
 * @author benjobs
 */
@Data
@TableName("t_flink_project")
@Excel("flink项目实体")
public class Project implements Serializable {
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    private String name;

    private String url;

    //分支.
    private String branches;

    @TableField("LASTBUILD")
    private Date lastBuild;

    private String username;

    private String password;

    /**
     * 1:git
     * 2:svn
     */
    private Integer repository;

    private String pom;

    private Date date;

    private String description;

    /**
     * 构建状态:
     * -1:未构建
     * 0:正在构建
     * 1:构建成功
     * 2:构建失败
     */
    @TableField("BUILDSTATE")
    private Integer buildState;

    private transient String dateFrom;

    private transient String dateTo;

    private transient String workspace;

    private transient String appSource;//项目源码路径

    private String getWorkSpace() {
        if (workspace == null) {
            workspace = SpringContextUtil.getBean(StreamXProperties.class).getWorkspace();
        }
        return workspace;
    }

    /**
     * 获取项目源码路径
     * @return
     */
    public File getAppSource() {
        if (appSource == null) {
            appSource = SpringContextUtil.getBean(StreamXProperties.class).getAppHome().concat("/project");
        }
        File sourcePath = new File(appSource);
        if (!sourcePath.exists()) {
            sourcePath.mkdirs();
        }
        if (sourcePath.isFile()) {
            throw new IllegalArgumentException("[StreamX] sourcePath must be directory");
        }
        String branches = this.getBranches() == null ? "master" : this.getBranches();
        String rootName = url.replaceAll(".*/|\\.git|\\.svn", "");
        String fullName = rootName.concat("-").concat(branches);
        return new File(sourcePath.getAbsolutePath().concat("/").concat(fullName));
    }

    public File getAppBase() {
        String appBase = SpringContextUtil.getBean(StreamXProperties.class).getAppHome().concat("/app/");
        return new File(appBase.concat(id.toString()));
    }

    public CredentialsProvider getCredentialsProvider() {
        return new UsernamePasswordCredentialsProvider(this.username, this.password);
    }

    public File getGitRepository() {
        File home = getAppSource();
        return new File(home, ".git");
    }

    public boolean isCloned() {
        File repository = getGitRepository();
        return repository.exists();
    }

    public String getMavenBuildCmd() {
        return String.format(
                "mvn clean install -DskipTests -f %s",
                CommonUtil.notEmpty(this.getPom())
                        ? this.getAppSource().getAbsolutePath().concat("/").concat(this.getPom())
                        : this.getAppSource().getAbsolutePath()
        );
    }

    public String getLog4BuildStart() {
        return String.format("%s[StreamX] project [%s] branches [%s],maven install beginning! cmd: %s\n\n",
                getLogHeader("maven"),
                getName(),
                getBranches(),
                getMavenBuildCmd()
        );
    }

    public String getLog4PullStart() {
        return String.format("%s[StreamX] project [%s] branches [%s] remote [origin],git pull beginning!\n\n",
                getLogHeader("git pull"),
                getName(),
                getBranches()
        );
    }


    public String getLog4CloneStart() {
        return String.format("%s[StreamX] project [%s] branches [%s], clone into [%s],git clone beginning!\n\n",
                getLogHeader("git clone"),
                getName(),
                getBranches(),
                getAppSource()
        );
    }

    public String getLogHeader(String header) {
        return "---------------------------------[ " + header + " ]---------------------------------\n";
    }

}
