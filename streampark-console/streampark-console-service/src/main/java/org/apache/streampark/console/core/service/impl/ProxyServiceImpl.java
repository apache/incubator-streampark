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

package org.apache.streampark.console.core.service.impl;

import org.apache.streampark.common.util.HadoopConfigUtils;
import org.apache.streampark.common.util.HadoopUtils;
import org.apache.streampark.common.util.YarnUtils;
import org.apache.streampark.console.base.util.WebUtils;
import org.apache.streampark.console.core.entity.ApplicationLog;
import org.apache.streampark.console.core.entity.FlinkApplication;
import org.apache.streampark.console.core.entity.FlinkCluster;
import org.apache.streampark.console.core.entity.SparkApplication;
import org.apache.streampark.console.core.service.FlinkClusterService;
import org.apache.streampark.console.core.service.ProxyService;
import org.apache.streampark.console.core.watcher.FlinkK8sWatcherWrapper;
import org.apache.streampark.flink.kubernetes.FlinkKubernetesWatcher;

import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.security.UserGroupInformation;

import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;

/** Routes console proxy requests to Flink, YARN, and remote cluster endpoints. */
@Slf4j
@Service
public class ProxyServiceImpl implements ProxyService {

    private static final String PROXY_PATH = "/proxy/";

    @Autowired
    private FlinkClusterService flinkClusterService;

    @Autowired
    private FlinkKubernetesWatcher flinkK8sWatcher;

    @Autowired
    private FlinkK8sWatcherWrapper k8sWatcherWrapper;

    @Override
    public void proxyFlink(
                           HttpServletRequest request,
                           HttpServletResponse response,
                           FlinkApplication app) throws Exception {
        String url;
        switch (app.getDeployModeEnum()) {
            case YARN_PER_JOB:
            case YARN_APPLICATION:
            case YARN_SESSION:
                url = YarnUtils.getRMWebAppProxyURL() + PROXY_PATH + app.getClusterId();
                proxyYarnRequest(
                    request,
                    response,
                    proxyUrl(url, request, "/proxy/flink/" + app.getId()));
                return;
            case REMOTE:
                FlinkCluster cluster = flinkClusterService.getById(app.getFlinkClusterId());
                url = cluster == null ? null : cluster.getAddress();
                break;
            case KUBERNETES_NATIVE_APPLICATION:
            case KUBERNETES_NATIVE_SESSION:
                url = flinkK8sWatcher.getRemoteRestUrl(k8sWatcherWrapper.toTrackId(app));
                break;
            default:
                throw new UnsupportedOperationException(
                    "unsupported deployMode ".concat(app.getDeployModeEnum().getName()));
        }

        if (StringUtils.isBlank(url)) {
            unavailableResponse(response, "The flink job manager url is not ready");
            return;
        }
        proxyRequest(
            request,
            response,
            proxyUrl(url, request, "/proxy/flink/" + app.getId()));
    }

    @Override
    public void proxySpark(
                           HttpServletRequest request,
                           HttpServletResponse response,
                           SparkApplication app) throws Exception {
        switch (app.getDeployModeEnum()) {
            case YARN_CLIENT:
            case YARN_CLUSTER:
                String url = YarnUtils.getRMWebAppProxyURL() + PROXY_PATH + app.getClusterId();
                proxyYarnRequest(
                    request,
                    response,
                    proxyUrl(url, request, "/proxy/spark/" + app.getId()));
                return;
            default:
                throw new UnsupportedOperationException(
                    "unsupported deployMode ".concat(app.getDeployModeEnum().getName()));
        }
    }

    @Override
    public void proxyYarn(
                          HttpServletRequest request,
                          HttpServletResponse response,
                          ApplicationLog log) throws Exception {
        String yarnId = log.getClusterId();
        if (StringUtils.isBlank(yarnId)) {
            unavailableResponse(response, "The yarn application id is null.");
            return;
        }
        String url = YarnUtils.getRMWebAppProxyURL() + PROXY_PATH + yarnId + "/";
        proxyYarnRequest(
            request,
            response,
            proxyUrl(url, request, "/proxy/yarn/" + log.getId()));
    }

    @Override
    public void proxyHistory(
                             HttpServletRequest request,
                             HttpServletResponse response,
                             ApplicationLog log) throws Exception {
        String url = log.getTrackingUrl();
        if (StringUtils.isBlank(url)) {
            unavailableResponse(response, "The jobManager url is null.");
            return;
        }
        proxyRequest(
            request,
            response,
            proxyUrl(url, request, "/proxy/history/" + log.getId()));
    }

    @Override
    public void proxyFlinkCluster(
                                  HttpServletRequest request,
                                  HttpServletResponse response,
                                  Long clusterId) throws Exception {
        FlinkCluster cluster = flinkClusterService.getById(clusterId);
        if (cluster == null) {
            unavailableResponse(response, "The cluster not found.");
            return;
        }
        String url = cluster.getAddress();
        if (StringUtils.isBlank(url)) {
            unavailableResponse(response, "The cluster address is invalid.");
            return;
        }

        HttpUrl target = proxyUrl(url, request, "/proxy/flink_cluster/" + clusterId);
        switch (cluster.getFlinkDeployModeEnum()) {
            case YARN_PER_JOB:
            case YARN_APPLICATION:
            case YARN_SESSION:
                proxyYarnRequest(request, response, target);
                return;
            case REMOTE:
            case KUBERNETES_NATIVE_APPLICATION:
            case KUBERNETES_NATIVE_SESSION:
                proxyRequest(request, response, target);
                return;
            default:
                throw new UnsupportedOperationException(
                    "unsupported deployMode ".concat(cluster.getFlinkDeployModeEnum().getName()));
        }
    }

    /** Proxies a request and converts transport failures into a gateway error. */
    private void proxyRequest(
                              HttpServletRequest request,
                              HttpServletResponse response,
                              HttpUrl url) throws Exception {
        try {
            WebUtils.http(url, request, response);
        } catch (Exception e) {
            log.error("Proxy request failed for {}", request.getRequestURI(), e);
            if (!response.isCommitted()) {
                response.sendError(HttpStatus.BAD_GATEWAY.value(), "Unable to reach upstream service");
            }
        }
    }

    /** Executes YARN requests under the configured HTTP authentication context. */
    private void proxyYarnRequest(
                                  HttpServletRequest request,
                                  HttpServletResponse response,
                                  HttpUrl url) throws Exception {
        if (YarnUtils.hasYarnHttpKerberosAuth()) {
            final HttpUrl kerberosUrl = url;
            UserGroupInformation ugi = HadoopUtils.getUgi();
            try {
                ugi.doAs(
                    (PrivilegedExceptionAction<Void>) () -> {
                        proxyRequest(request, response, kerberosUrl);
                        return null;
                    });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw e;
            } catch (Exception e) {
                log.error("Kerberos YARN proxy request failed for {}", request.getRequestURI(), e);
                if (!response.isCommitted()) {
                    response.sendError(HttpStatus.BAD_GATEWAY.value(), "Unable to reach YARN service");
                }
            }
            return;
        }

        if (YarnUtils.hasYarnHttpSimpleAuth()) {
            url = url.newBuilder()
                .addQueryParameter("user.name", HadoopConfigUtils.hadoopUserName())
                .build();
        }
        proxyRequest(request, response, url);
    }

    /** Builds a target while keeping request data out of the upstream authority. */
    HttpUrl proxyUrl(
                     String baseUrl,
                     HttpServletRequest request,
                     String proxyPrefix) {
        HttpUrl base = HttpUrl.parse(baseUrl);
        if (base == null || !base.username().isEmpty() || !base.password().isEmpty()) {
            throw new IllegalArgumentException("Invalid proxy upstream URL");
        }

        String route = request.getContextPath() + proxyPrefix;
        String requestPath = request.getRequestURI();
        if (!requestPath.equals(route) && !requestPath.startsWith(route + "/")) {
            throw new IllegalArgumentException("Request path is outside the proxy route");
        }
        String path = requestPath.substring(route.length());
        rejectPathTraversal(path);

        HttpUrl.Builder target = base.newBuilder().fragment(null);
        if (!path.isEmpty()) {
            target.addEncodedPathSegments(path.substring(1));
        }
        target.encodedQuery(request.getQueryString());
        return target.build();
    }

    /** Rejects traversal before OkHttp normalizes encoded path segments. */
    private void rejectPathTraversal(String path) {
        String decoded = path;
        for (int pass = 0; pass < 3; pass++) {
            String normalized = decoded.replace('\\', '/');
            boolean hasTraversal =
                Arrays.stream(normalized.split("/", -1))
                    .anyMatch(segment -> ".".equals(segment) || "..".equals(segment));
            if (hasTraversal) {
                throw new IllegalArgumentException("Proxy path traversal is not allowed");
            }
            String next = URLDecoder.decode(decoded, StandardCharsets.UTF_8);
            if (next.equals(decoded)) {
                return;
            }
            decoded = next;
        }
        throw new IllegalArgumentException("Proxy path contains excessive encoding");
    }

    private void unavailableResponse(HttpServletResponse response, String body) throws IOException {
        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        response.setContentType("text/plain;charset=UTF-8");
        PrintWriter writer = response.getWriter();
        writer.write(body);
        writer.flush();
    }
}
