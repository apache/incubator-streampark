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

package org.apache.streampark.flink.packer.maven;

import org.apache.streampark.common.conf.CommonConfig;
import org.apache.streampark.common.conf.InternalConfigHolder;
import org.apache.streampark.common.conf.Workspace;
import org.apache.streampark.common.constants.Constants;
import org.apache.streampark.common.util.AssertUtils;
import org.apache.streampark.common.util.Utils;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.plugins.shade.DefaultShader;
import org.apache.maven.plugins.shade.ShadeRequest;
import org.apache.maven.plugins.shade.filter.Filter;
import org.apache.maven.plugins.shade.resource.ManifestResourceTransformer;
import org.apache.maven.plugins.shade.resource.ResourceTransformer;
import org.apache.maven.plugins.shade.resource.ServicesResourceTransformer;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.repository.AuthenticationBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public final class MavenTool {

    private static final Logger PLEXUS_LOG = new ConsoleLogger(Logger.LEVEL_INFO, "streampark-maven");

    private static final List<Artifact> EXCLUDE_ARTIFACT =
            Lists.newArrayList(
                    Artifact.of("org.apache.flink:force-shading:*"),
                    Artifact.of("com.google.code.findbugs:jsr305:*"),
                    Artifact.of("org.apache.logging.log4j:*:*"));

    private MavenTool() {}

    @Nonnull
    public static File buildFatJar(
            @Nullable String mainClass, @Nonnull Set<String> jarLibs, @Nonnull String outFatJarPath)
            throws Exception {
        File uberJar = new File(outFatJarPath);
        if (!outFatJarPath.endsWith(Constants.JAR_SUFFIX) || uberJar.isDirectory()) {
            throw new IllegalArgumentException(
                    "[StreamPark] streampark-packer: outFatJarPath("
                            + outFatJarPath
                            + ") should be a JAR file.");
        }
        if (uberJar.exists()) {
            uberJar.delete();
        }
        Set<File> jarSet = new HashSet<>();
        for (String lib : jarLibs) {
            File libFile = new File(lib);
            if (!libFile.exists()) {
                continue;
            }
            if (isJarFile(libFile)) {
                jarSet.add(libFile);
            } else if (libFile.isDirectory()) {
                File[] files = libFile.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (isJarFile(f)) {
                            jarSet.add(f);
                        }
                    }
                }
            }
        }
        log.info("start shaded fat-jar: {}", jarLibs);
        ShadeRequest shadeRequest = new ShadeRequest();
        shadeRequest.setJars(jarSet);
        shadeRequest.setUberJar(uberJar);
        List<ResourceTransformer> transformers = new ArrayList<>();
        transformers.add(new ServicesResourceTransformer());
        if (mainClass != null) {
            ManifestResourceTransformer manifest = new ManifestResourceTransformer();
            manifest.setMainClass(mainClass);
            transformers.add(manifest);
        }
        shadeRequest.setResourceTransformers(transformers);
        shadeRequest.setFilters(Lists.newArrayList(new ShadeFilter()));
        shadeRequest.setRelocators(Lists.newArrayList());
        DefaultShader shader = new DefaultShader();
        shader.enableLogging(PLEXUS_LOG);
        shader.shade(shadeRequest);
        log.info("finish build fat-jar: {}", uberJar.getAbsolutePath());
        return uberJar;
    }

    @Nonnull
    public static File buildFatJar(
            @Nullable String mainClass,
            @Nonnull DependencyInfo dependencyInfo,
            @Nonnull String outFatJarPath)
            throws Exception {
        Set<String> jarLibs = dependencyInfo.extJarLibs();
        Set<Artifact> arts = dependencyInfo.mavenArts();
        AssertUtils.required(
                !(jarLibs.isEmpty() && arts.isEmpty()),
                "[StreamPark] streampark-packer: empty artifacts.");
        List<String> artFilePaths =
                resolveArtifacts(arts).stream()
                        .map(File::getAbsolutePath)
                        .collect(Collectors.toList());
        Set<String> allLibs = new HashSet<>(jarLibs);
        allLibs.addAll(artFilePaths);
        return buildFatJar(mainClass, allLibs, outFatJarPath);
    }

    @Nonnull
    public static List<File> resolveArtifacts(Artifact mavenArtifact) throws Exception {
        return resolveArtifacts(Set.of(mavenArtifact));
    }

    @Nonnull
    public static List<File> resolveArtifacts(Set<Artifact> mavenArtifacts) throws Exception {
        if (mavenArtifacts == null || mavenArtifacts.isEmpty()) {
            return List.of();
        }
        RepositorySystem repoSystem;
        RepositorySystemSession session;
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(
                RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
        repoSystem = locator.getService(RepositorySystem.class);
        DefaultRepositorySystemSession sysSession = MavenRepositorySystemUtils.newSession();
        sysSession.setLocalRepositoryManager(
                repoSystem.newLocalRepositoryManager(
                        sysSession, new LocalRepository(Workspace.MAVEN_LOCAL_PATH())));
        session = sysSession;

        List<org.eclipse.aether.artifact.Artifact> artifacts = new ArrayList<>();
        for (Artifact e : mavenArtifacts) {
            artifacts.add(new DefaultArtifact(e.groupId(), e.artifactId(), e.classifier(), "jar", e.version()));
        }
        log.info("start resolving dependencies: {}", artifacts);

        List<RemoteRepository> remoteRepos = getRemoteRepos();
        List<org.eclipse.aether.artifact.Artifact> resolvedArtifacts = new ArrayList<>();
        for (org.eclipse.aether.artifact.Artifact artifact : artifacts) {
            ArtifactDescriptorRequest descriptorRequest =
                    new ArtifactDescriptorRequest(artifact, remoteRepos, null);
            resolvedArtifacts.addAll(
                    repoSystem
                            .readArtifactDescriptor(session, descriptorRequest)
                            .getDependencies()
                            .stream()
                            .filter(d -> "compile".equals(d.getScope()))
                            .filter(
                                    x ->
                                            EXCLUDE_ARTIFACT.stream()
                                                    .noneMatch(e -> e.eq(x.getArtifact())))
                            .map(d -> d.getArtifact())
                            .collect(Collectors.toList()));
        }
        resolvedArtifacts.addAll(artifacts);
        log.info("resolved dependencies: {}", resolvedArtifacts);

        List<ArtifactRequest> artReqs = new ArrayList<>();
        for (org.eclipse.aether.artifact.Artifact artifact : resolvedArtifacts) {
            artReqs.add(new ArtifactRequest(artifact, remoteRepos, null));
        }
        List<ArtifactResult> results = repoSystem.resolveArtifacts(session, artReqs);
        return results.stream().map(r -> r.getArtifact().getFile()).collect(Collectors.toList());
    }

    private static List<RemoteRepository> getRemoteRepos() {
        RemoteRepository.Builder builder =
                new RemoteRepository.Builder(
                        "central", "default", InternalConfigHolder.get(CommonConfig.MAVEN_REMOTE_URL()));
        String user = InternalConfigHolder.get(CommonConfig.MAVEN_AUTH_USER());
        String password = InternalConfigHolder.get(CommonConfig.MAVEN_AUTH_PASSWORD());
        RemoteRepository remoteRepository;
        if (user == null || password == null) {
            remoteRepository = builder.build();
        } else {
            remoteRepository =
                    builder.setAuthentication(
                                    new AuthenticationBuilder()
                                            .addUsername(user)
                                            .addPassword(password)
                                            .build())
                            .build();
        }
        return List.of(remoteRepository);
    }

    private static boolean isJarFile(File file) {
        try {
            Utils.requireCheckJarFile(file.toURI().toURL());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static final class ShadeFilter implements Filter {
        @Override
        public boolean canFilter(File jar) {
            return true;
        }

        @Override
        public boolean isFiltered(String name) {
            boolean filtered =
                    name.startsWith("META-INF/")
                            && (name.endsWith(".SF")
                                    || name.endsWith(".DSA")
                                    || name.endsWith(".RSA"));
            if (filtered) {
                log.info("shade ignore file: {}", name);
            }
            return filtered;
        }

        @Override
        public void finished() {}
    }
}
