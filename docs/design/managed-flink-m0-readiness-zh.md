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

# 云厂商托管 Flink M0 技术准入包

## 1. 文档目的

本文记录 StreamPark 对接火山引擎流式计算 Flink 版在进入主功能研发前的
M0 验证证据、能力矩阵、未决阻塞项和准入结论。

关联文档：

- [总体技术设计](managed-flink-cloud-provider-technical-design-zh.md)
- [异步 Operation API ADR](adr/0001-managed-flink-operation-api-zh.md)
- [火山 Provider Client 选型 ADR](adr/0002-volcengine-flink-client-zh.md)
- [安全设计与威胁模型](managed-flink-security-review-zh.md)
- [Deploy Mode 全仓审计](managed-flink-deploy-mode-audit-zh.md)

## 2. 验证范围和时间

| 项目 | 值 |
| --- | --- |
| 验证日期 | 2026-07-28 |
| 验证方式 | `volc-flink` 命令 schema、只读查询、真实 SQL/JAR/Savepoint E2E、官方文档和 Maven Central |
| E2E CLI profile | `test.json` |
| E2E 地域 | `cn-beijing` |
| E2E 项目 | `cwz-test`（`vi366dwkhngh`） |
| E2E 资源池 | `paimon-test2`（`o-00g0ok9qhjcc`） |
| 资源前缀 | `streampark-m0-20260728-*` |
| 写操作 | 已完成创建、上传、发布、启动、停止、Savepoint 创建和恢复 |

安全说明：

- 未读取、记录或输出 AK/SK 明文。
- 所有写操作均限定在用户确认的 `cwz-test / paimon-test2`，并使用独立目录和前缀。
- 测试结束后 SQL/JAR 作业均为 `STOPPED`，资源池恢复为 `0/3000 CU`。
- 草稿、作业、私有 JAR 和 Savepoint 保留为评审证据，未删除既有或测试资源。

## 3. 已取得的环境证据

### 3.1 身份和项目访问

| 验证项 | 结果 | 状态 |
| --- | --- | --- |
| 当前凭证完整性 | `context_complete=true`，credential status valid | PASS |
| 广州项目查询 | 可查询项目 `cwz-gz` | PASS |
| 北京项目查询 | 可通过 `test.json` 查询 `cwz-test` | PASS |
| 资源池查询 | 两个项目均可列出 RUNNING 资源池及容量 | PASS |
| 草稿查询 | SQL/JAR/CDC/Batch 草稿均可见 | PASS |
| 作业查询 | 可读取 CREATED/RUNNING/STOPPED/FAILED/SUCCEEDED 等状态 | PASS |

这证明当前身份具备首期 SQL/JAR/Savepoint 所需的实际写权限，但不能证明账号只拥有
最小权限，也不能证明未测试的 OpenAPI Action 均已授权。

### 3.2 当前可用资源

广州项目存在三个 RUNNING 的 POST 资源池，其中 `cwz-test` 资源池当前使用量为
`0/1000 CU`。北京项目存在三个 RUNNING 的 POST 资源池，其中 `paimon-test2`
当前使用量为 `0/3000 CU`。

本次 E2E 已明确并使用：

| 资源 | 标识 |
| --- | --- |
| 草稿目录 | `/streampark-m0-20260728`（`2082077328433315841`） |
| 文件目录 | `streampark-m0-20260728`（`dc148756072d4a2085621e01a3c36c50`） |
| SQL Draft | `2082077399685877761` |
| SQL Job | `2082078175917604866` |
| JAR File | `60928f1c3627437d8a7816b9cd90cb3e`，PRIVATE，Version 1 |
| JAR Draft | `2082077544146370562` |
| JAR Job | `2082078246419386370` |
| Savepoint | `42d2ad1b71c94d2b964a5544ca6a986f`，AVAILABLE |

## 4. CLI Schema 能力矩阵

CLI schema 是当前可执行客户端的命令契约，可用于验证 Provider 模型和字段设计。
它不能代替服务端真实写调用。

| 能力 | CLI 命令 | 关键输入 | 关键输出 | 当前结论 |
| --- | --- | --- | --- | --- |
| 项目列表 | `project list` | search、分页 | id、name、displayName、status、owner | 已实测 |
| 资源池列表 | `resource-pool list` | nameKey、分页 | id、billingType、status、CU/CPU/Memory | 已实测 |
| 创建草稿 | `draft create` | jobType、engineVersion、SQL/JAR | draftId、jobType、artifact reference | SQL/JAR 已实测 |
| 更新草稿 | `draft update` | draftId/path、SQL/JAR | draftId、resourceVersion | schema 已确认 |
| SQL 校验 | `draft validate` | draftId/path | valid、type、reason、solution | 已实测，`valid=true` |
| 上线 | `draft publish` | draft、resourcePool、priority、policy | jobId、queue、policy | SQL/JAR 已实测 |
| 文件上传 | `file upload` | localFile、dirId、permission | fileId、URI、TOS URI、size | 私有 JAR 已实测 |
| 添加依赖 | `draft dependency add` | TOS URI、本地 JAR 或 fileId/version | jars、dependencyVersions | schema 已确认 |
| 启动作业 | `job start` | jobId、start mode、savepointId、资源 | success、from、params | SQL/JAR `new` 已实测 |
| 停止作业 | `job stop` | jobId、withSavepoint | success | SQL/JAR 已实测 |
| 查询作业 | `job get/list` | jobId | state、version、deploymentId、REST URL | SQL/JAR 作业已实测 |
| 创建快照 | `savepoint create` | jobId、description | instanceId、success | JAR 已实测 |
| 快照列表 | `savepoint list` | jobId | savepointId、status、type、path、时间 | 已实测，状态 AVAILABLE |
| 从快照恢复 | `job restart --from savepoint` | jobId、savepointId | success、新实例 | 已实测 |

### 4.1 已确认的枚举和约束

草稿类型：

```text
FLINK_STREAMING_SQL
FLINK_BATCH_SQL
FLINK_STREAMING_JAR
FLINK_BATCH_JAR
FLINK_CDC_JAR
```

当前 CLI 暴露的引擎版本：

```text
FLINK_VERSION_1_16
FLINK_VERSION_1_17
FLINK_VERSION_1_20
```

上线调度策略：

```text
GANG
DRF
```

启动模式：

```text
new
latest
savepoint
```

`savepoint` 模式必须提供 `savepoint_id`。资源池 ID 和资源池名称必须二选一。
JAR 草稿可以使用：

1. 本地 JAR，由客户端上传并登记为文件资源。
2. `tos://` URI。
3. 文件管理的 `fileId + fileVersion`。

这确认了技术设计中的 `ArtifactStagingService` 必须同时支持 URI 和版本化文件资源，
不能只保存一个不带版本的 URL。

### 4.2 真实 E2E 证据

已完成一个 Streaming SQL 和一个 Streaming JAR 的真实写链路，确认：

- SQL 草稿包含 SQL 定义、引擎版本、资源版本和部署状态。
- JAR 草稿包含主类、主 JAR URI、依赖 JAR 列表、引擎版本和资源版本。
- 发布后的作业包含独立 job ID、版本、资源池/队列、运行状态和 deployment ID。
- SQL 作业完成 create、validate、publish、start、observe、stop。
- JAR 作业完成 upload、create、publish、start、observe、stop。
- 文件资源列表确认资源具有稳定 file ID、目录 ID、类型、权限、引用状态和 TOS URI；
  Artifact 缓存不能只依赖可变文件名。
- JAR 作业创建的 Savepoint 状态为 `AVAILABLE`，并成功通过
  `job restart --from savepoint` 恢复。原实例转为 `STOPPED`，新实例进入 `RUNNING`。
- 首次 SQL 启动暴露资源池 HostAlias 中含下划线的非法 DNS 配置。平台返回明确事件，
  修复 DNS 后同一 Job 重试成功，验证了失败诊断和人工修复后的恢复路径。

作业详情还可能返回携带短期访问令牌的完整控制台/REST 地址。该字段只能用于瞬时跳转，
不得进入数据库、Operation JSON、日志、异常、指标或前端持久化。Provider 应丢弃 query
和 fragment，持久化时优先只保存 external ID；需要跳转时由后端按需生成短 TTL 地址。

## 5. 状态和错误分类矩阵

### 5.1 已观察到的作业状态

只读列表实际观察到：

```text
CREATED
RUNNING
STOPPED
FAILED
SUCCEEDED
```

真实 E2E 还观察到 `STARTING`。Provider 规范化层仍需验证以下其余中间态：

```text
DEPLOYING
RESTARTING
STOPPING
SAVEPOINTING
```

若服务端返回未识别状态，必须保存 `providerRawState` 并映射为 `OTHER`，不得把未知态
直接归类为 FAILED。

### 5.2 CLI 暴露的错误类别

```text
invalid_credentials
permission_denied
missing_context
not_logged_in
validation_error
not_found
ambiguous_reference
precondition_failed
confirmation_required
network_error
platform_error
```

建议 Provider 映射：

| CLI/服务错误或条件 | ProviderErrorCategory | 重试 | 证据级别 |
| --- | --- | --- | --- |
| invalid_credentials、not_logged_in、签名错误 | AUTHENTICATION | 否 | CLI schema；待最小权限账号演练 |
| permission_denied | AUTHORIZATION | 否 | CLI schema；待最小权限账号演练 |
| validation_error、ambiguous_reference、服务端参数反序列化失败 | VALIDATION | 否 | 已实测缺少 file 参数、相对 draft path、超范围 long |
| precondition_failed、重复资源、状态冲突 | CONFLICT | 查询状态后决定 | 已实测同名文件 `meta resource already exist` |
| not_found | NOT_FOUND | 否 | 已实测 file、draft、job、resource pool |
| 非法 HostAlias 等云环境配置 | PROVIDER_CONFIGURATION | 人工修复后重试 | 已实测 STARTING -> FAILED |
| 配额或容量不足 | QUOTA | 否；容量变化后人工重试 | 契约级，待故障注入 |
| HTTP 429、Throttling、RequestLimitExceeded、TooManyRequests | RATE_LIMIT | 是；遵守 Retry-After，否则指数退避+jitter | 契约级，不主动压测制造 |
| network_error（DNS、连接失败） | TRANSIENT | 有界重试；写请求先查询幂等结果 | 已实测 DNS lookup failed |
| HTTP 5xx、ServiceUnavailable、超时 | TRANSIENT | 有界重试 | 契约级，待 fake server |
| 未识别 | UNKNOWN | 有界重试后转 UNKNOWN | 默认兜底 |

本次还实际录制到 CLI 访问 OpenAPI Endpoint 时的 `network_error`（DNS lookup failed）。
CLI 错误信封稳定包含 `error.type`、`error.message` 和 `meta.command`。本次已观察到两类
服务端业务错误在 `error.detail.status=200` 的同时返回 `request_id`：同名资源冲突，以及
超范围 long 导致的服务端反序列化失败。这证明 Provider 必须先解析 SDK 响应元数据和业务
错误，不能把 HTTP 2xx 直接判为成功。

`providerRequestId` 必须是可空字段：本地校验、not-found 和 DNS 失败样本均可能没有
requestId；服务端业务错误样本则包含 requestId。日志只记录 requestId、action、
errorCategory 和脱敏消息，不记录原始响应或带令牌 URL。

重试判定顺序固定为：

1. 先按明确服务端 error code 映射。
2. 再按 HTTP status 映射。
3. 再按受控 message pattern 做兼容映射，并记录未知 code 指标。
4. 最后进入 UNKNOWN；禁止仅凭 message 全文决定自动重试。

限流不通过高频调用测试账号主动制造。RATE_LIMIT 作为契约级映射进入 fake HTTP server
测试：优先使用 `Retry-After`，缺失时指数退避并增加 jitter；写请求必须先按幂等键查询，
不得直接重放。真实限流与配额演练保留在 Q-03 集成测试，不阻塞 Provider SPI 和状态机
受控开发。

本次真实故障还观察到：平台接受启动请求后，Kubernetes 因非法 HostAlias hostname
拒绝创建 AppMaster Pod，作业从 `STARTING` 转为 `FAILED`。这类失败应归入
`PLATFORM_CONFIGURATION` 或等价的不可自动重试类别，并保留脱敏事件摘要。

## 6. Go/No-Go 证据

| 条件 | 当前证据 | 状态 |
| --- | --- | --- |
| D-01～D-10 有书面结论 | 飞书 revision 65 已全部确认 | PASS |
| 火山测试账号和最小权限策略可用 | 首期关键写 Action 已验证；账号是否最小权限仍待 IAM 审核 | PARTIAL |
| SQL 全生命周期真实链路 | create、validate、publish、start、observe、stop 已通过 | PASS |
| JAR/依赖上传方案 | main JAR 全链路和 dependency fileId/version 绑定、回读、移除已通过；首期采用不可变 checksum 资源 | PASS |
| Savepoint 创建与恢复 | AVAILABLE Savepoint 创建成功，并恢复出新 RUNNING 实例 | PASS |
| Provider 状态和错误码映射 | 状态、错误优先级、可空 requestId、HTTP 2xx 业务失败和限流契约已固化；真实限流/权限演练转入 Q-03 | PASS |
| SDK/HTTP client 选型及许可证 | 技术和许可证评估完成，待维护者批准新增依赖 | PARTIAL |
| 凭证主密钥安全评审 | 设计完成，待安全责任人批准 | PARTIAL |

## 7. 写链路 Spike 结果

### 7.1 已确认目标

已使用：

```text
profile=test.json
projectId=vi366dwkhngh
resourcePoolId=o-00g0ok9qhjcc
draftDirectory=/streampark-m0-20260728
resourceDirectoryId=dc148756072d4a2085621e01a3c36c50
resourceNamePrefix=streampark-m0-20260728
retentionPolicy=stop jobs; retain evidence resources
```

### 7.2 SQL Spike：PASS

使用 `datagen -> print`，未访问外部数据源。create、validate、publish、start、
RUNNING 观测和 stop 均通过。

1. 创建、读取并校验 Streaming SQL 草稿。
2. 发布到 `paimon-test2` 并取得 Job ID。
3. 从 `new` 启动，观察 `STARTING`。
4. 首次启动因资源池非法 HostAlias 进入 `FAILED`。
5. 修复 DNS 后重新从 `new` 启动并进入 `RUNNING`。
6. 读取运行实例作为证据。
7. 停止作业并验证 `STOPPED`。

### 7.3 JAR、依赖与 Savepoint Spike：PASS

已通过：最小 Apache License 示例 JAR 构建、PRIVATE 上传、fileId/version 读取、
Streaming JAR 草稿、publish、start、Savepoint、指定 Savepoint restart、stop。

dependency/fileVersion 补充 Spike 的第一步结果：

- PRIVATE 测试依赖上传成功，file ID 为 `a370b0307e524acda3cfcb3771314965`，
  version 为 `1`，资源尚未被草稿引用。
- 使用相同目录和相同资源名上传不同 checksum 的 v2 文件时，平台返回
  `platform_error: meta resource already exist`，没有生成 version 2。
- 当前 CLI 仅暴露 `file.upload/list/get/url`，没有文件版本更新命令。因此不能把
  “同名 upload”当作资源版本更新；Provider 的版本更新能力需要直接验证官方 SDK/OpenAPI，
  或首期采用 content-addressed 新资源并显式保存 `fileId + version=1`。
- 已将上述 file ID 的 version 1 绑定到已停止的 JAR Draft
  `2082077544146370562`。回读结果为
  `dependencyVersions[a370b0307e524acda3cfcb3771314965] = 1`，证明
  `fileId + fileVersion` 依赖契约可用，且绑定未触发作业发布或启动。

测试依赖已成功移除，回读结果恢复为 `jars=[]` 和 `dependencyVersions={}`。首期方案确定为
不可变、checksum 命名的 PRIVATE 文件资源：每种内容生成独立 fileId，并显式保存平台返回
的 version；不依赖同名资源原地更新。

后续非阻塞验证：官方 SDK/OpenAPI 是否支持已有资源新增版本、checksum 并发复用、
失败上传和未引用资源延迟清理策略。这些项进入 Artifact staging 实现与故障注入测试，
不再阻塞受控开发。

### 7.4 失败场景

- 无效资源池。
- 不支持的引擎版本。
- 重复上线。
- 重复启动和重复停止。
- 启动请求超时后查询。
- 凭证缺少单个 Action 权限。
- 文件大小边界。
- 限流响应。
- 同名文件上传冲突后的查询与幂等恢复。

## 8. 当前准入结论

结论：**云端核心技术可行性已通过，允许进入受控开发；合入主干和生产发布仍为
Conditional Go。**

可以开始：

- Provider SPI 和 fake provider。
- Deploy Mode 审计与显式分流测试。
- Operation store、状态机和 REST DTO 的独立开发。
- CredentialCryptoService 的纯本地实现和测试。
- MySQL/PostgreSQL DDL 评审。

不应开始：

- 假定服务端字段稳定的火山请求 mapper。
- 未验证 fileVersion/URI 语义的 Artifact staging 正式实现。
- 未解决 REST 兼容性的 build/start/cancel Controller 改造。
- 未完成审批前将 SDK 依赖、凭证实现或生产 IAM 策略合入主干。

主功能 Go 的剩余条件：

1. Operation API ADR 获架构评审通过。
2. 维护者批准官方 Java SDK 新增依赖。
3. 安全责任人批准主密钥、最小 IAM 和 Endpoint 方案。
