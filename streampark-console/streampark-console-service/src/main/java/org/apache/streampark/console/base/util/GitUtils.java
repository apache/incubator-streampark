/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.streampark.console.base.util;

import org.apache.commons.lang3.StringUtils;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LsRemoteCommand;
import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.util.FS;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/** */
public class GitUtils {

    private GitUtils() {
    }

    public static Git clone(GitCloneRequest request) throws GitAPIException {
        try {
            CloneCommand cloneCommand =
                Git.cloneRepository().setURI(request.getUrl()).setDirectory(request.getStoreDir());
            setCredentials(cloneCommand, request);
            if (StringUtils.isNotBlank(request.getBranch())) {
                cloneCommand.setBranch(Constants.R_HEADS + request.getBranch());
                cloneCommand.setBranchesToClone(
                    Collections.singletonList(Constants.R_HEADS + request.getBranch()));
            }
            Git git = cloneCommand.call();
            if (StringUtils.isNotBlank(request.getBranch())) {
                git.checkout().setName(request.getBranch()).call();
            } else if (StringUtils.isNotBlank(request.getTag())) {
                git.checkout().setName(request.getTag()).call();
            } else {
                throw new IllegalArgumentException("git clone failed, No tag or branch specified");
            }
            return git;
        } catch (Exception e) {
            if (e instanceof InvalidRemoteException && request.getConnType() == GitConnType.HTTP) {
                String url = httpUrlToSSH(request.getUrl());
                request.setUrl(url);
                return clone(request);
            }
            throw e;
        }
    }

    public static List<String> getBranches(GitGetRequest request) throws GitAPIException {
        try {
            LsRemoteCommand command = Git.lsRemoteRepository().setRemote(request.getUrl()).setHeads(true);
            setCredentials(command, request);
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
        } catch (Exception e) {
            if (e instanceof InvalidRemoteException && request.getConnType() == GitConnType.HTTP) {
                String url = httpUrlToSSH(request.getUrl());
                request.setUrl(url);
                return getBranches(request);
            }
            throw e;
        }
    }

    public static List<String> getTags(GitGetRequest request) throws GitAPIException {
        try {
            LsRemoteCommand command = Git.lsRemoteRepository().setRemote(request.getUrl()).setTags(true);
            setCredentials(command, request);
            Collection<Ref> refList = command.call();
            List<String> tagList = new ArrayList<>(4);
            for (Ref ref : refList) {
                String refName = ref.getName();
                if (refName.startsWith(Constants.R_TAGS)) {
                    String branchName = refName.replace(Constants.R_TAGS, "");
                    tagList.add(branchName);
                }
            }
            return tagList;
        } catch (Exception e) {
            if (e instanceof InvalidRemoteException && request.getConnType() == GitConnType.HTTP) {
                String url = httpUrlToSSH(request.getUrl());
                request.setUrl(url);
                return getTags(request);
            }
            throw e;
        }
    }

    public static String httpUrlToSSH(String url) {
        return url.replaceAll("(https://|http://)(.*?)/(.*?)/(.*?)(\\.git|)\\s*$", "git@$2:$3/$4.git");
    }

    public static boolean isSshRepositoryUrl(String url) {
        return url.trim().startsWith("git@");
    }

    public static boolean isHttpRepositoryUrl(String url) {
        return !isSshRepositoryUrl(url);
    }

    private static void setCredentials(
                                       TransportCommand<?, ?> transportCommand, GitAuthRequest request) {
        switch (request.connType) {
            case HTTP:
                if (!StringUtils.isAllEmpty(request.getUsername(), request.getPassword())) {
                    UsernamePasswordCredentialsProvider credentialsProvider =
                        new UsernamePasswordCredentialsProvider(request.getUsername(), request.getPassword());
                    transportCommand.setCredentialsProvider(credentialsProvider);
                }
                break;
            case SSH:
                transportCommand.setTransportConfigCallback(
                    transport -> {
                        SshTransport sshTransport = (SshTransport) transport;
                        sshTransport.setSshSessionFactory(
                            new JschConfigSessionFactory() {

                                @Override
                                protected void configure(OpenSshConfig.Host hc, Session session) {
                                    session.setConfig("StrictHostKeyChecking", "no");
                                }

                                @Override
                                protected JSch createDefaultJSch(FS fs) throws JSchException {
                                    JSch jSch = super.createDefaultJSch(fs);
                                    String prvkeyPath = request.getPrivateKey();
                                    if (StringUtils.isBlank(prvkeyPath)) {
                                        String userHome = System.getProperty("user.home");
                                        if (userHome != null) {
                                            String rsaPath = userHome.concat("/.ssh/id_rsa");
                                            File resFile = new File(rsaPath);
                                            if (resFile.exists()) {
                                                prvkeyPath = rsaPath;
                                            }
                                        }
                                    }
                                    if (prvkeyPath == null) {
                                        return jSch;
                                    }
                                    if (StringUtils.isEmpty(request.getPassword())) {
                                        jSch.addIdentity(prvkeyPath);
                                    } else {
                                        jSch.addIdentity(prvkeyPath, request.getPassword());
                                    }
                                    return jSch;
                                }
                            });
                    });
                break;
            default:
                throw new IllegalStateException(
                    "[StreamPark] repository URL is invalid, must be ssh or http(s)");
        }
    }

    @Getter
    public enum GitConnType {
        HTTP,
        SSH
    }

    @Getter
    @Setter
    public static class GitAuthRequest {

        private GitConnType connType;
        private String username;
        private String password;
        private String privateKey;
    }

    @Getter
    @Setter
    public static class GitGetRequest extends GitAuthRequest {

        private String url;

        public void setUrl(String url) {
            if (StringUtils.isBlank(url)) {
                throw new IllegalArgumentException("git url cannot be empty");
            }
            this.url = url;
            if (GitUtils.isSshRepositoryUrl(url)) {
                setConnType(GitConnType.SSH);
            } else {
                setConnType(GitConnType.HTTP);
            }
        }
    }

    @Getter
    @Setter
    public static class GitCloneRequest extends GitGetRequest {

        private File storeDir;
        private String branch;
        private String tag;

        public void setRefs(String refs) {
            if (StringUtils.isNotBlank(refs)) {
                if (!refs.startsWith(Constants.R_REFS)) {
                    this.branch = refs;
                    return;
                }
                if (refs.startsWith(Constants.R_HEADS)) {
                    this.branch = refs.replace(Constants.R_HEADS, "");
                    return;
                }
                if (refs.startsWith(Constants.R_TAGS)) {
                    this.tag = refs.replace(Constants.R_TAGS, "");
                }
            }
        }
    }
}
