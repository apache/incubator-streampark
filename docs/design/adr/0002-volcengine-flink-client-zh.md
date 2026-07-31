<!--
  Licensed to the Apache Software Foundation (ASF) under one
  or more contributor license agreements.  See the NOTICE file
  distributed with this work for additional information
  regarding copyright ownership.  The ASF licenses this file
  to you under the Apache License, Version 2.0 (the
  "License"); you may not use this file except in compliance
  with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing,
  software distributed under the License is distributed on an
  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  KIND, either express or implied.  See the License for the
  specific language governing permissions and limitations
  under the License.
-->

# ADR-0002：火山引擎 Flink Provider Client 选型

- 状态：Accepted，维护者已批准新增依赖
- 评审准备度：Dependency accepted，进入 Provider 适配
- 日期：2026-07-28

## 候选方案

1. 官方 Java SDK：`com.volcengine:volcengine-java-sdk-flink20250101`
2. 基于通用 HTTP Client 自行实现签名、序列化和重试
3. 由 StreamPark 服务调用外部 `volc-flink` CLI

## 决策

推荐使用官方 Java SDK，并在 Provider 内做适配；不在服务端执行外部 CLI，不自行实现
签名算法。

新增第三方依赖必须在实际修改 POM 前获得 StreamPark 维护者明确批准。

## 证据

- 官方仓库存在独立的 `volcengine-java-sdk-flink20250101` 模块。
- Maven Central 提供同名制品，模块只直接依赖 `volcengine-java-sdk-core`。
- 仓库和 Maven POM 均声明 Apache License 2.0。
- SDK 支持 Java 8，满足 StreamPark 当前运行基线。
- 官方 SDK 已提供 credentials、region、endpoint、timeout、retry 和 exception 模型。

参考：

- https://github.com/volcengine/volcengine-java-sdk
- https://central.sonatype.com/artifact/com.volcengine/volcengine-java-sdk-flink20250101
- https://api.volcengine.com/api-docs/view?serviceCode=flink&version=2025-01-01

## 为什么不自研签名

- 签名规范、公共参数和错误响应可能演进。
- 容易在 canonical request、编码、时钟偏移和临时 token 上出现兼容问题。
- 重复维护生成模型和服务 Action。
- 安全审计范围显著扩大。

## 为什么不调用 CLI

- 外部进程、配置文件和二进制分发不适合作为 Web 服务运行时依赖。
- 不利于连接池、超时、指标、trace 和异常分类。
- CLI profile 与 StreamPark 多租户 Cloud Account 模型不匹配。
- CLI 可以用于 M0 验证，但不进入产品运行时。

## 依赖控制

只引入：

```xml
<dependency>
  <groupId>com.volcengine</groupId>
  <artifactId>volcengine-java-sdk-flink20250101</artifactId>
  <version>${volcengine-java-sdk.version}</version>
</dependency>
```

版本通过项目 dependency management 固定，不使用动态版本。引入前执行：

```shell
./mvnw dependency:tree \
  -pl streampark-console/streampark-console-service \
  -Dincludes=com.volcengine,com.squareup.okhttp,com.google.code.gson,io.gsonfire
```

必须检查：

- Apache 2.0 许可证兼容性和 NOTICE 要求。
- okhttp、gson、swagger annotations、threetenbp、commons-lang 的版本收敛。
- CVE 扫描。
- SDK 默认凭证链是否可能绕过 StreamPark Cloud Account。

## 凭证策略

禁止使用 SDK 默认凭证链。Provider 必须显式注入从
`CredentialCryptoService` 解密得到的短生命周期内存对象：

```java
Credentials.getCredentials(accessKey, secretKey, sessionToken)
```

要求：

- 不从 `~/.volcengine`、环境变量、ECS metadata 或 CLI profile 自动发现凭证。
- 每个 Cloud Account 构造隔离的 client/context。
- client cache 的 key 不包含明文密钥。
- 密钥轮换后使旧 client cache 失效。

## Endpoint 和 HTTP

- 默认 endpoint 由 provider 根据 region 和官方规则生成。
- 自定义 endpoint 必须通过安全 allowlist。
- connect/read/write/call timeout 分别配置。
- SDK 自动重试默认关闭或限制为只读/明确幂等请求。
- 所有写请求由 Orchestrator 控制重试和幂等。
- 禁止 SDK debug 输出请求头或签名。

## Provider 隔离

SDK DTO 只允许出现在：

```text
org.apache.streampark.console.core.managed.provider.volcengine
```

Provider SPI、controller、service、entity 和前端不得暴露 SDK 类型。

## 退出标准

ADR 转为 Accepted 前需要：

1. 维护者批准新增依赖。
2. dependency tree 无不可接受冲突。
3. Apache RAT、Spotless、Checkstyle 和许可证检查通过。
4. 使用 fake HTTP server 验证 timeout、endpoint、异常和脱敏。
5. 真实测试账号完成一个只读和一个写请求。

当前状态：

- 官方 SDK 模块、Apache 2.0 许可证和 Java 8 基线已确认。
- 真实测试账号的只读、SQL/JAR 写请求和 Savepoint 请求已通过 CLI 验证。
- 维护者已于 2026-07-29 明确批准新增依赖，版本固定为 Maven Central 正式版
  `2.0.20`，未使用动态版本。
- 实际 dependency tree 已验证：Flink 模块仅直接依赖同版本 core；SDK 的 SLF4J 和 Gson
  由项目 dependency management 收敛，未发现版本仲裁冲突。
- SDK 两个制品均声明 Apache License 2.0，JAR 内无额外 NOTICE 文件。新增制品及
  OkHttp、Gson、Gson Fire、Swagger annotations、ThreeTenBP、commons-lang 已完成
  OSV 批量核对。
- OSV 唯一命中为既有 `commons-lang:2.6` 的 CVE-2025-48924。SDK 源码只调用
  `StringUtils`，不调用受影响的 `ClassUtils.getClass(...)`；该 JAR 已存在于项目发布
  依赖清单。本次按“非新增且当前调用路径不可达”记录风险，后续升级 SDK core 时移除。
- SDK `ApiClient` 将显式调用 `setCredentials`、`setAutoRetry(false)`、region、endpoint
  和 timeout setter；不设置 CredentialProvider，不启用默认凭证链。
- `LICENSE` 和 `tools/dependencies/known-dependencies.txt` 已登记 SDK 及新增传递制品；
  ThreeTenBP 的 BSD 3-Clause 原文已纳入发行许可证。SDK 安全工厂已实现逐请求隔离
  client、官方 Endpoint allowlist、显式凭证、零次 SDK 重试、请求结束解除凭证引用和
  plaintext 字符数组清零，对应 2 个安全契约测试通过。

## 维护者审批清单

| 决议项 | 推荐结论 | 状态 |
| --- | --- | --- |
| 允许引入 `volcengine-java-sdk-flink20250101` | 接受 | 已确认 |
| SDK DTO 仅限 Volcengine Provider 包 | 接受 | 已确认 |
| 禁用默认凭证链并显式注入凭证 | 接受 | 已确认 |
| SDK 写请求自动重试默认关闭 | 接受 | 已确认 |
| 批准后再固定版本并执行 dependency/CVE/NOTICE 检查 | 接受 | 已完成 |
