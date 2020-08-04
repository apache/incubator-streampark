package com.streamxhub.flink.monitor.core.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.wuwenze.poi.annotation.Excel;
import lombok.Data;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.IOException;
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

    private String description;



    /**
     * ssh://
     * http://
     * svn://
     */
    private String protocol;

    private String url;

    //分支.
    private String branches;

    private Date date;

    @TableField("BUILDDATE")
    private Date buildDate;

    private String username;

    private String password;

    /**
     * 是否clone过
     */
    private boolean cloned = false;

    /**
     *  1:git
     *  2:svn
     */
    private Integer resptype;

    private transient String dateFrom;

    private transient String dateTo;

    public File getHome(String workspace) throws IllegalAccessException {
        File workspaceFile = new File(workspace);
        if(!workspaceFile.exists()) {
            workspaceFile.mkdirs();
        }
        if(workspaceFile.isFile()) {
           throw new IllegalAccessException("[StreamX] workspace must be directory");
        }
        String branche = "master";
        if(this.getBranches() != null) {
            branche = this.getBranches();
        }
        String rootName = url.replaceAll(".*/|\\.git|\\.svn","");
        String fullName = rootName.concat("-").concat(branche);
        File file = new File(workspaceFile.getAbsolutePath().concat("/").concat(fullName));
        return file;
    }

    public CredentialsProvider getCredentialsProvider() {
        return new UsernamePasswordCredentialsProvider("wanghuajie","123322242");
    }

    public FileRepository getRepository(String workspace) throws Exception {
        File home = getHome(workspace);
        return new FileRepository(new File(home,".git"));
    }

}
