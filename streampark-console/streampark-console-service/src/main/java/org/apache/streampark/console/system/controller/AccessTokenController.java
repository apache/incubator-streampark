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

package org.apache.streampark.console.system.controller;

import org.apache.streampark.common.util.CURLBuilder;
import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.base.domain.RestResponse;
import org.apache.streampark.console.base.exception.InternalException;
import org.apache.streampark.console.core.enums.AccessTokenState;
import org.apache.streampark.console.core.service.CommonService;
import org.apache.streampark.console.system.entity.AccessToken;
import org.apache.streampark.console.system.service.AccessTokenService;

import org.apache.shiro.authz.annotation.RequiresPermissions;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Tag(name = "ACCESS_TOKEN_TAG")
@RestController
@RequestMapping("token")
public class AccessTokenController {

  @Autowired private AccessTokenService accessTokenService;

  @Autowired private CommonService commonService;

  /** generate token string */
  @Operation(summary = "Create token")
  @Parameters({
    @Parameter(
        name = "userId",
        description = "user id",
        required = true,
        example = "100000",
        schema = @Schema(implementation = Long.class)),
    @Parameter(
        name = "expireTime",
        description = "token expire time, yyyy-MM-dd HH:mm:ss",
        required = true,
        example = "9999-01-01 00:00:00",
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "description",
        description = "token description",
        schema = @Schema(implementation = String.class))
  })
  @PostMapping(value = "create")
  @RequiresPermissions("token:add")
  public RestResponse createToken(
      @NotBlank(message = "{required}") Long userId,
      String expireTime,
      @RequestParam(required = false) String description)
      throws InternalException {
    return accessTokenService.generateToken(userId, expireTime, description);
  }

  @Operation(summary = "Verify current user token")
  @PostMapping(value = "check")
  public RestResponse verifyToken() {
    Long userId = commonService.getUserId();
    RestResponse restResponse = RestResponse.success();
    if (userId != null) {
      AccessToken accessToken = accessTokenService.getByUserId(userId);
      if (accessToken == null) {
        restResponse.data(AccessTokenState.NULL.get());
      } else if (AccessToken.STATUS_DISABLE.equals(accessToken.getFinalStatus())) {
        restResponse.data(AccessTokenState.INVALID.get());
      } else {
        restResponse.data(AccessTokenState.OK.get());
      }
    } else {
      restResponse.data(AccessTokenState.INVALID.get());
    }
    return restResponse;
  }

  /** query token list */
  @Operation(summary = "List tokens")
  @Parameters({
    @Parameter(
        name = "userId",
        in = ParameterIn.QUERY,
        description = "user id",
        schema = @Schema(implementation = Long.class))
  })
  @PostMapping(value = "list")
  @RequiresPermissions("token:view")
  public RestResponse tokensList(
      RestRequest restRequest, @Parameter(hidden = true) AccessToken accessToken) {
    IPage<AccessToken> accessTokens = accessTokenService.findAccessTokens(accessToken, restRequest);
    return RestResponse.success(accessTokens);
  }

  /** update token status */
  @Operation(summary = "Toggle token")
  @Parameters({
    @Parameter(
        name = "tokenId",
        description = "token id",
        required = true,
        example = "1",
        schema = @Schema(implementation = Long.class))
  })
  @PostMapping("toggle")
  @RequiresPermissions("token:add")
  public RestResponse toggleToken(@NotNull(message = "{required}") Long tokenId) {
    return accessTokenService.toggleToken(tokenId);
  }

  /** delete token by id */
  @Operation(summary = "Delete token")
  @Parameters({
    @Parameter(
        name = "tokenId",
        description = "token id",
        required = true,
        example = "1",
        schema = @Schema(implementation = Long.class))
  })
  @DeleteMapping(value = "delete")
  @RequiresPermissions("token:delete")
  public RestResponse deleteToken(@NotBlank(message = "{required}") Long tokenId) {
    boolean res = accessTokenService.deleteToken(tokenId);
    return RestResponse.success(res);
  }

  /**
   * copy cURL, hardcode now, there is no need for configuration here, because there are several
   * fixed interfaces
   */
  @Operation(summary = "Generate api with token")
  @PostMapping(value = "curl")
  public RestResponse copyRestApiCurl(
      @NotBlank(message = "{required}") String appId,
      @NotBlank(message = "{required}") String baseUrl,
      @NotBlank(message = "{required}") String path) {
    String resultCURL = null;
    CURLBuilder curlBuilder = new CURLBuilder(baseUrl + path);

    curlBuilder
        .addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
        .addHeader(
            "Authorization", accessTokenService.getByUserId(commonService.getUserId()).getToken());

    if ("/flink/app/start".equalsIgnoreCase(path)) {
      resultCURL =
          curlBuilder
              .addFormData("allowNonRestored", "false")
              .addFormData("savePoint", "")
              .addFormData("savePointed", "false")
              .addFormData("id", appId)
              .build();
    } else if ("/flink/app/cancel".equalsIgnoreCase(path)) {
      resultCURL =
          curlBuilder
              .addFormData("id", appId)
              .addFormData("savePointed", "false")
              .addFormData("drain", "false")
              .addFormData("savePoint", "")
              .build();
    }
    return RestResponse.success(resultCURL);
  }
}
