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

import org.apache.streampark.common.util.HadoopUtils;
import org.apache.streampark.common.util.SystemPropertyUtils;
import org.apache.streampark.common.util.YarnUtils;
import org.apache.streampark.console.base.util.EncryptUtils;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.entity.ApplicationLog;
import org.apache.streampark.console.core.entity.FlinkCluster;
import org.apache.streampark.console.core.service.ApplicationLogService;
import org.apache.streampark.console.core.service.ApplicationService;
import org.apache.streampark.console.core.service.FlinkClusterService;
import org.apache.streampark.console.core.service.ProxyService;
import org.apache.streampark.console.core.service.ServiceHelper;
import org.apache.streampark.console.core.task.FlinkK8sWatcherWrapper;
import org.apache.streampark.console.system.entity.Member;
import org.apache.streampark.console.system.entity.User;
import org.apache.streampark.console.system.service.MemberService;
import org.apache.streampark.flink.kubernetes.FlinkK8sWatcher;

import org.apache.commons.io.IOUtils;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.security.PrivilegedExceptionAction;
import java.util.Enumeration;

@Service
public class ProxyServiceImpl implements ProxyService {

  @Autowired private ServiceHelper serviceHelper;

  @Autowired private ApplicationService applicationService;

  @Autowired private FlinkClusterService flinkClusterService;

  @Autowired private MemberService memberService;

  @Autowired private ApplicationLogService logService;

  @Autowired private FlinkK8sWatcher flinkK8sWatcher;

  @Autowired private FlinkK8sWatcherWrapper k8sWatcherWrapper;

  private final RestTemplate proxyRestTemplate;

  private final boolean hasYarnHttpKerberosAuth;

  private String lastUsername = "";

  public ProxyServiceImpl(RestTemplateBuilder restTemplateBuilder) {
    this.proxyRestTemplate =
        restTemplateBuilder
            .errorHandler(
                new DefaultResponseErrorHandler() {
                  @Override
                  public void handleError(@Nonnull ClientHttpResponse response) {
                    // Ignore errors in the Flink Web UI itself, such as 404 errors.
                  }
                })
            .build();

    String yarnHttpAuth = SystemPropertyUtils.get("streampark.yarn.http-auth");
    this.hasYarnHttpKerberosAuth = "kerberos".equalsIgnoreCase(yarnHttpAuth);
  }

  @Override
  public ResponseEntity<?> proxyFlinkUI(HttpServletRequest request, Long appId) throws Exception {
    ResponseEntity.BodyBuilder builder = ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE);
    if (appId == null) {
      return builder.body("Invalid operation, appId is null");
    }

    User currentUser = serviceHelper.getLoginUser();
    Application app = applicationService.getById(appId);
    if (app == null) {
      return builder.body("Invalid operation, appId is invalid.");
    }
    if (!currentUser.getUserId().equals(app.getUserId())) {
      Member member = memberService.findByUserId(app.getTeamId(), currentUser.getUserId());
      if (member == null) {
        return builder.body(
            "Permission denied, this job not created by the current user, And the job cannot be found in the current user's team.");
      }
    }

    String url = null;
    switch (app.getExecutionModeEnum()) {
      case REMOTE:
        FlinkCluster cluster = flinkClusterService.getById(app.getFlinkClusterId());
        url = cluster.getAddress();
        break;
      case YARN_PER_JOB:
      case YARN_APPLICATION:
      case YARN_SESSION:
        String yarnURL = YarnUtils.getRMWebAppProxyURL();
        url = yarnURL + "/proxy/" + app.getClusterId();
        url += getRequestURL(request).replace("/proxy/flink-ui/" + appId, "");
        return proxyYarnRequest(request, url);
      case KUBERNETES_NATIVE_APPLICATION:
      case KUBERNETES_NATIVE_SESSION:
        String jobManagerUrl = app.getJobManagerUrl();
        if (jobManagerUrl == null) {
          builder = ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE);
          builder.body("The flink job manager url is not ready");
          return builder.build();
        }
        url = flinkK8sWatcher.getRemoteRestUrl(k8sWatcherWrapper.toTrackId(app));
        break;
    }

    if (url == null) {
      return builder.body("The flink job manager url is not ready");
    }

    url += getRequestURL(request).replace("/proxy/flink-ui/" + appId, "");
    return proxyRequest(request, url);
  }

  @Override
  public ResponseEntity<?> proxyYarn(HttpServletRequest request, String appId) throws Exception {
    String yarnURL = YarnUtils.getRMWebAppProxyURL();
    String url = yarnURL + "/proxy/" + appId + "/";
    url += getRequestURL(request).replace("/proxy/yarn/" + appId, "");
    return proxyYarnRequest(request, url);
  }

  @Override
  public ResponseEntity<?> proxyJobManager(HttpServletRequest request, Long logId)
      throws Exception {
    ApplicationLog log = logService.getById(logId);
    String url = log.getJobManagerUrl();
    url += getRequestURL(request).replace("/proxy/job_manager/" + logId, "");
    return proxyRequest(request, url);
  }

  private HttpEntity<?> getRequestEntity(HttpServletRequest request, String url, boolean setAuth)
      throws Exception {
    HttpHeaders headers = new HttpHeaders();
    Enumeration<String> headerNames = request.getHeaderNames();
    while (headerNames.hasMoreElements()) {
      String headerName = headerNames.nextElement();
      headers.set(headerName, request.getHeader(headerName));
    }

    // Ensure the Host header is set correctly.
    URI uri = new URI(url);
    headers.set("Host", uri.getHost());

    if (setAuth) {
      String token = serviceHelper.getAuthorization();
      if (token != null) {
        headers.set("Authorization", EncryptUtils.encrypt(token));
      }
    }

    byte[] body = null;
    if (request.getInputStream().available() > 0) {
      InputStream inputStream = request.getInputStream();
      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      IOUtils.copy(inputStream, byteArrayOutputStream);
      body = byteArrayOutputStream.toByteArray();
    }

    HttpEntity<?> requestEntity = new HttpEntity<>(body, headers);
    return requestEntity;
  }

  private ResponseEntity<?> proxyRequest(HttpServletRequest request, String url) throws Exception {
    HttpEntity<?> requestEntity = getRequestEntity(request, url, true);
    return proxyRestTemplate.exchange(
        url, HttpMethod.valueOf(request.getMethod()), requestEntity, byte[].class);
  }

  private ResponseEntity<?> proxyYarnRequest(HttpServletRequest request, String url)
      throws Exception {
    if (hasYarnHttpKerberosAuth) {
      UserGroupInformation ugi = HadoopUtils.getUgi();

      HttpEntity<?> requestEntity = getRequestEntity(request, url, false);
      setRestTemplateCredentials(ugi.getShortUserName());

      return ugi.doAs(
          new PrivilegedExceptionAction<ResponseEntity<?>>() {
            @Override
            public ResponseEntity<?> run() throws Exception {
              return proxyRestTemplate.exchange(
                  url, HttpMethod.valueOf(request.getMethod()), requestEntity, byte[].class);
            }
          });
    } else {
      return proxyRequest(request, url);
    }
  }

  private String getRequestURL(HttpServletRequest request) {
    return request.getRequestURI()
        + (request.getQueryString() != null ? "?" + request.getQueryString() : "");
  }

  private void setRestTemplateCredentials(String username) {
    setRestTemplateCredentials(username, null);
  }

  /**
   * Configures the RestTemplate's HttpClient connector. This method is primarily used to configure
   * the HttpClient authentication information and SSL certificate validation policies.
   *
   * @param username The username for HTTP basic authentication.
   * @param password The password for HTTP basic authentication.
   */
  private void setRestTemplateCredentials(String username, String password) {
    // Check if the username is not null and has changed since the last configuration
    if (username != null && !this.lastUsername.equals(username)) {
      // Create a new credentials provider
      BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
      // Add the username and password for HTTP basic authentication
      credentialsProvider.setCredentials(
          AuthScope.ANY, new UsernamePasswordCredentials(username, password));

      // Customize the HttpClient with the credentials provider
      CloseableHttpClient httpClient =
          HttpClients.custom().setDefaultCredentialsProvider(credentialsProvider).build();
      // Set the HttpClient request factory for the RestTemplate
      this.proxyRestTemplate.setRequestFactory(
          new HttpComponentsClientHttpRequestFactory(httpClient));
      // Update the last known username
      this.lastUsername = username;
    }
  }
}
