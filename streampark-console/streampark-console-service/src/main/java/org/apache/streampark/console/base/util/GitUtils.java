/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.apache.streampark.console.base.util;

import org.apache.streampark.console.core.entity.Project;
import org.apache.streampark.console.core.enums.GitProtocol;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LsRemoteCommand;
import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.util.FS;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 *
 */
public class GitUtils {

    public static Git clone(Project project) throws GitAPIException {
        CloneCommand cloneCommand = Git.cloneRepository()
            .setURI(project.getUrl())
            .setDirectory(project.getAppSource());

        if (project.getBranches() != null) {
            cloneCommand.setBranch(Constants.R_HEADS + project.getBranches());
            cloneCommand.setBranchesToClone(Collections.singletonList(Constants.R_HEADS + project.getBranches()));
        }
        setCredentials(cloneCommand, project);
        return cloneCommand.call();
    }

    public static List<String> getBranchList(Project project) throws GitAPIException {
        LsRemoteCommand command = Git.lsRemoteRepository()
            .setRemote(project.getUrl())
            .setHeads(true);
        setCredentials(command, project);
        Collection<Ref> refList = command.call();
        List<String> branchList = new ArrayList<>(4);
        for (Ref ref : refList) {
            String refName = ref.getName();
            if (refName.startsWith(Constants.R_HEADS)) {
                String branchName = refName.replace(Constants.R_HEADS, "");
                branchList.add(branchName);
            }
        }
        return branchList;
    }

    private static void setCredentials(TransportCommand<?, ?> transportCommand, Project project) {
        GitProtocol gitProtocol = GitProtocol.of(project.getGitProtocol());
        switch (gitProtocol) {
            case HTTPS:
                if (!StringUtils.isAllEmpty(project.getUserName(), project.getPassword())) {
                    UsernamePasswordCredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider(project.getUserName(), project.getPassword());
                    transportCommand.setCredentialsProvider(credentialsProvider);
                }
                break;
            case SSH:
                transportCommand.setTransportConfigCallback(transport -> {
                    SshTransport sshTransport = (SshTransport) transport;
                    sshTransport.setSshSessionFactory(new JschConfigSessionFactory() {
                        @Override
                        protected void configure(OpenSshConfig.Host hc, Session session) {
                            session.setConfig("StrictHostKeyChecking", "no");
                        }

                        @Override
                        protected JSch createDefaultJSch(FS fs) throws JSchException {
                            JSch jSch = super.createDefaultJSch(fs);
                            if (project.getRsaPath() == null) {
                                return jSch;
                            }
                            if (StringUtils.isEmpty(project.getPassword())) {
                                jSch.addIdentity(project.getRsaPath());
                            } else {
                                jSch.addIdentity(project.getRsaPath(), project.getPassword());
                            }
                            return jSch;
                        }
                    });
                });
                break;
            default:
                throw new IllegalStateException("[StreamPark] git setCredentials: unsupported protocol type");
        }
    }

}
