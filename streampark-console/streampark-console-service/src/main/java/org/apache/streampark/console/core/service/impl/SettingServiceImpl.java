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

import org.apache.streampark.console.core.bean.DockerConfig;
import org.apache.streampark.console.core.bean.MavenConfig;
import org.apache.streampark.console.core.bean.ResponseResult;
import org.apache.streampark.console.core.bean.SenderEmail;
import org.apache.streampark.console.core.entity.Setting;
import org.apache.streampark.console.core.mapper.SettingMapper;
import org.apache.streampark.console.core.service.SettingService;

import org.apache.commons.lang3.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.api.model.AuthResponse;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;

import java.util.List;
import java.util.Optional;
import java.util.Properties;

@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class SettingServiceImpl extends ServiceImpl<SettingMapper, Setting>
    implements SettingService, ApplicationListener<ContextRefreshedEvent> {

  @Override
  public Setting get(String key) {
    LambdaQueryWrapper<Setting> queryWrapper =
        new LambdaQueryWrapper<Setting>().eq(Setting::getSettingKey, key);
    return this.getOne(queryWrapper);
  }

  private final Setting emptySetting = new Setting();

  @Override
  public void onApplicationEvent(ContextRefreshedEvent event) {
    List<Setting> settingList = super.list();
    settingList.forEach(x -> SETTINGS.put(x.getSettingKey(), x));
  }

  @Override
  public boolean update(Setting setting) {
    try {
      String value = StringUtils.trimToNull(setting.getSettingValue());
      setting.setSettingValue(value);

      Setting entity = new Setting();
      entity.setSettingValue(setting.getSettingValue());
      LambdaQueryWrapper<Setting> queryWrapper =
          new LambdaQueryWrapper<Setting>().eq(Setting::getSettingKey, setting.getSettingKey());
      this.update(entity, queryWrapper);

      MavenConfig.fromSetting(setting).updateConfig();

      Optional<Setting> optional = Optional.ofNullable(SETTINGS.get(setting.getSettingKey()));
      optional.ifPresent(x -> x.setSettingValue(value));
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public boolean updateDocker(DockerConfig dockerConfig) {
    List<Setting> settings = DockerConfig.toSettings(dockerConfig);
    for (Setting each : settings) {
      if (!update(each)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean updateEmail(SenderEmail senderEmail) {
    List<Setting> settings = SenderEmail.toSettings(senderEmail);
    for (Setting each : settings) {
      if (!update(each)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public SenderEmail getSenderEmail() {
    try {
      String host = SETTINGS.get(SettingService.KEY_ALERT_EMAIL_HOST).getSettingValue();
      String port = SETTINGS.get(SettingService.KEY_ALERT_EMAIL_PORT).getSettingValue();
      String from = SETTINGS.get(SettingService.KEY_ALERT_EMAIL_FROM).getSettingValue();
      String userName = SETTINGS.get(SettingService.KEY_ALERT_EMAIL_USERNAME).getSettingValue();
      String password = SETTINGS.get(SettingService.KEY_ALERT_EMAIL_PASSWORD).getSettingValue();
      String ssl = SETTINGS.get(SettingService.KEY_ALERT_EMAIL_SSL).getSettingValue();

      SenderEmail senderEmail = new SenderEmail();
      senderEmail.setHost(host);
      if (StringUtils.isNotBlank(port)) {
        senderEmail.setPort(Integer.parseInt(port));
      }
      senderEmail.setFrom(from);
      senderEmail.setUserName(userName);
      senderEmail.setPassword(password);
      if (StringUtils.isNotBlank(ssl)) {
        senderEmail.setSsl(Boolean.parseBoolean(ssl));
      }
      return senderEmail;
    } catch (Exception e) {
      log.warn("Fault Alert Email is not set.");
    }
    return null;
  }

  @Override
  public String getIngressModeDefault() {
    return SETTINGS
        .getOrDefault(SettingService.KEY_INGRESS_MODE_DEFAULT, emptySetting)
        .getSettingValue();
  }

  @Override
  public ResponseResult checkDocker(DockerConfig dockerConfig) {
    DockerClientConfig config =
        DefaultDockerClientConfig.createDefaultConfigBuilder()
            .withRegistryUrl(dockerConfig.getAddress())
            .build();

    DockerHttpClient httpClient =
        new ApacheDockerHttpClient.Builder().dockerHost(config.getDockerHost()).build();

    ResponseResult result = new ResponseResult();

    try (DockerClient dockerClient = DockerClientImpl.getInstance(config, httpClient)) {
      AuthConfig authConfig =
          new AuthConfig()
              .withUsername(dockerConfig.getUserName())
              .withPassword(dockerConfig.getPassword())
              .withRegistryAddress(dockerConfig.getAddress());
      AuthResponse response = dockerClient.authCmd().withAuthConfig(authConfig).exec();
      if (response.getStatus().equals("Login Succeeded")) {
        result.setStatus(200);
      } else {
        result.setStatus(500);
        result.setMsg("docker login failed, status: " + response.getStatus());
      }
    } catch (Exception e) {
      if (e.getMessage().contains("LastErrorException")) {
        result.setStatus(400);
      } else if (e.getMessage().contains("Status 401")) {
        result.setStatus(500);
        result.setMsg(
            "Failed to validate Docker registry, unauthorized: incorrect username or password ");
      } else {
        result.setStatus(500);
        result.setMsg("Failed to validate Docker registry, error: " + e.getMessage());
      }
    }
    return result;
  }

  @Override
  public ResponseResult checkEmail(SenderEmail senderEmail) {
    ResponseResult result = new ResponseResult();
    Properties props = new Properties();
    props.put("mail.smtp.auth", "true");
    if (senderEmail.isSsl()) {
      props.put("mail.smtp.starttls.enable", "true");
    }
    props.put("mail.smtp.host", senderEmail.getHost());
    props.put("mail.smtp.port", senderEmail.getPort());

    Session session = Session.getInstance(props);
    try {
      Transport transport = session.getTransport("smtp");
      transport.connect(
          senderEmail.getHost(), senderEmail.getUserName(), senderEmail.getPassword());
      transport.close();
      result.setStatus(200);
    } catch (MessagingException e) {
      result.setStatus(500);
      result.setMsg("connect to target mail server failed: " + e.getMessage());
    }

    return result;
  }
}
