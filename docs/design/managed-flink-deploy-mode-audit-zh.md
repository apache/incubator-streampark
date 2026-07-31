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

# MANAGED_APPLICATION Deploy Mode 全仓影响审计

## 1. 审计结论

新增 `MANAGED_APPLICATION(7, "managed-application")` 可行，但必须在 Console 层提前分流，
不能让新模式进入现有 Flink Client、workspace、Flink Home、Flink REST 或 K8s watcher。

静态扫描结果：

| 范围 | 命中文件 |
| --- | ---: |
| `streampark-common` | 1 |
| `streampark-flink` | 17 |
| `streampark-console-service` | 43 |
| `streampark-console-webapp` | 32 |
| 合计 | 93 |

其中至少 27 个文件包含 Deploy Mode switch 或 `isYarn/isKubernetes/isRemote/isSession`
分支，需要逐一确认默认行为。

## 2. 核心原则

1. `streampark-flink-client` 不注册 managed client。
2. `streampark-flink-packer` 不接收 managed build request。
3. managed 模式在访问 FlinkEnv、Flink Home、workspace、FsOperator 前分流。
4. managed 作业不进入现有 HTTP/K8s watcher。
5. managed cluster 不进入现有 cluster watcher。
6. managed Flink UI 使用外部控制台链接，不进入反向代理。
7. managed snapshot 不进入本地 savepoint path 和 `FsOperator.delete`。
8. Spark 代码和 Spark Deploy Mode 不变。

## 3. P0：未分流会直接失败或产生错误副作用

| 文件/位置 | 当前行为 | Managed 要求 | 测试 |
| --- | --- | --- | --- |
| `FlinkApplication.getAppHome()` | 未识别模式抛异常 | managed 调用链不得进入 | managed release/start 不调用 |
| `FlinkApplication.getStorageType()` | 未识别模式抛异常 | managed 不解析 LFS/HDFS | managed CRUD/详情不调用 |
| `FlinkApplication.getFsOperator()` | 依赖 storage type | managed 不使用 | 删除/回滚不操作文件系统 |
| `FlinkApplicationBackup.renderPath()` | 未识别模式抛异常 | managed 使用配置版本，不创建路径备份 | release/revoke 测试 |
| `FlinkApplicationActionServiceImpl.start()` | 强依赖 pipeline、FlinkEnv、FlinkClient | 方法入口路由 managed service | 旧模式契约不变 |
| `FlinkApplicationActionServiceImpl.cancel()` | 构造 Flink cancel request/savepoint path | 方法入口路由 managed service | managed stop 不进入 |
| `FlinkApplicationActionServiceImpl.restart()` | cancel 后立即 start | managed 使用 Operation 状态机 | STOP 完成后才能 START |
| `FlinkApplicationBuildPipelineServiceImpl` | 创建本地 packer pipeline | managed 走 artifact+draft+deploy | 不创建本地 pipeline |
| `FlinkSavepointServiceImpl` | 使用本地/HDFS path 和 FlinkEnv | managed 路由 snapshot service | 不访问 versionId |
| `ProxyServiceImpl` | default 抛 unsupported | managed 返回外部链接/明确不代理 | SSRF/开放重定向测试 |
| `FlinkApplicationHistoryController` | Deploy Mode switch | managed 历史不依赖 workspace | 历史查询/回滚 |
| `FlinkAppHttpWatcher` | 排除 K8s 后跟踪其他模式 | 必须显式排除 managed | managed 不发 Flink REST |
| `FlinkClusterWatcher` | 非 K8s cluster 可能进入 | 必须显式排除 managed | managed env 独立 probe |

## 4. P1：数据和查询语义

| 文件/区域 | 风险 | 修改要求 |
| --- | --- | --- |
| `FlinkApplicationManageServiceImpl` | create/update/remove/mapping 假设 version/workspace | DTO 校验按 mode 分支；删除只解绑 |
| `FlinkApplicationInfoServiceImpl` | 拼装 Yarn/Flink REST 信息 | managed 聚合 external state/sync state |
| `FlinkApplicationMapper.xml` | 统计和分页可能遗漏新模式 | 确认统计、状态筛选和 Team 条件 |
| `FlinkClusterServiceImpl` | start/shutdown/probe 假设 remote/session | managed 禁止 start/shutdown，独立探活 |
| `FlinkCluster` | remote URI/K8s/Yarn 属性 | managed 通用字段保持空值安全 |
| Alert template | Yarn URL 和 cluster 信息分支 | 增加 provider、externalId、consoleUrl |
| schema/data SQL | deploy mode 字段、菜单权限 | MySQL/PostgreSQL/H2 同步 |

## 5. P1：Watcher 筛选

### 5.1 HTTP watcher

当前查询仅排除 Kubernetes：

```java
.notIn(FlinkApplication::getDeployMode, FlinkDeployMode.getKubernetesMode())
```

新增模式后会错误进入 HTTP watcher。应改为显式 legacy set，或同时排除 managed：

```java
FlinkDeployMode.getHttpWatcherModes()
```

推荐使用 include list，防止未来新模式默认被 watcher 接管。

### 5.2 Cluster watcher

同样使用显式 include list：

```text
REMOTE
YARN_SESSION
```

managed environment 由 `ManagedFlinkEnvironmentProbe` 负责。

### 5.3 K8s watcher

当前使用 `getKubernetesMode()` include list，天然不会纳入 managed。仍需增加回归测试，
确保 `toTrackId()` 不被 managed 调用。

## 6. P1：前端

### 6.1 枚举

在 `src/enums/flinkEnum.ts` 增加：

```text
MANAGED_APPLICATION = 7
```

Provider 不是 Deploy Mode，不增加 VOLCENGINE/AWS/ALIYUN 枚举值。

### 6.2 Cluster 表单

受影响文件：

```text
views/flink/cluster/Edit.vue
views/flink/cluster/View.vue
views/flink/cluster/useClusterSetting.ts
api/flink/flinkCluster.type.ts
```

要求：

- managed 展示 Cloud Account、region、project、resource pool。
- 隐藏 Flink Home、address、namespace、service account 和版本选择。
- start/shutdown 操作不可见，后端仍做拒绝校验。
- Project/Resource Pool 下拉处理 loading、无权限、不支持和手工 ID 降级。

### 6.3 Application 表单

高风险文件：

```text
views/flink/app/Add.vue
views/flink/app/EditFlink.vue
views/flink/app/EditStreamPark.vue
views/flink/app/hooks/useCreateAndEditSchema.ts
views/flink/app/hooks/useCreateSchema.ts
views/flink/app/hooks/useEdit.ts
views/flink/app/hooks/useEditStreamPark.ts
views/flink/app/utils/index.ts
```

要求：

- managed 不提交 `versionId`、K8s、Yarn 和 local build 字段。
- SQL/JAR 都展示 provider capability 驱动的 engine version。
- 首期隐藏 Batch/PyFlink/CDC。
- release/start/stop 调用 managed Operation API。
- 表单竞态：切换账号/region/project 时取消旧请求或比较 generation。

### 6.4 列表和详情

高风险文件：

```text
views/flink/app/Detail.vue
views/flink/app/hooks/useApp.tsx
views/flink/app/hooks/useAppTableAction.ts
views/flink/app/hooks/useFlinkRender.tsx
```

要求：

- 控制台链接不走 `/proxy/flink`。
- 展示 provider state、sync state、last sync、external IDs。
- 不展示依赖本地 FlinkEnv 的字段。
- action availability 由 managed state + active operation 决定。

## 7. P2：Flink 模块

`streampark-flink-client`、`streampark-flink-packer` 和
`streampark-flink-kubernetes` 不应实现 managed 分支。

必须通过 Console 入口提前分流，确保：

- `FlinkClientEntrypoint` map 不包含 MANAGED_APPLICATION。
- 不构造 `SubmitRequest/CancelRequest/TriggerSavepointRequest`。
- 不调用 `FlinkK8sDeployMode.of(MANAGED_APPLICATION)`。
- 不创建 `BuildRequest`。

建议增加负向测试：若 managed 意外进入 FlinkClientEntrypoint，应快速失败并给出明确的
Console routing error，而不是空指针或 unsupported default。

## 8. 数据库和升级

必须同步：

```text
mysql-schema.sql
pgsql-schema.sql
mysql-data.sql
pgsql-data.sql
H2 schema/data
MySQL upgrade
PostgreSQL upgrade
sql-rev.dict（仅有方言差异时）
```

兼容要求：

- 既有 Deploy Mode 数值不变。
- 新值为 7。
- 旧版本 StreamPark 读取值 7 会得到 UNKNOWN，因此禁止应用先回滚、数据库仍保留可写
  managed 数据的无保护组合。
- feature flag 关闭时 managed 数据只读可见且禁止操作。

## 9. 必须新增的测试

### 9.1 Enum

- `of(7)` 返回 MANAGED_APPLICATION。
- 既有 0～6 映射不变。
- `isManagedMode(7)` 为 true。
- `isYarn/isKubernetes/isRemote/isSession` 均为 false。

### 9.2 路由

- managed start/cancel/release 不调用 FlinkClient。
- managed 不调用 `getAppHome/getStorageType/getFsOperator`。
- legacy 模式不调用 ManagedFlinkProvider。
- feature flag 关闭时写操作拒绝。

### 9.3 Watcher

- HTTP watcher 查询不包含 mode 7。
- K8s watcher查询不包含 mode 7。
- Cluster watcher 查询不包含 mode 7。
- Managed watcher 只包含 mode 7。

### 9.4 Proxy/History/Savepoint

- managed UI 链接是受控外部链接。
- managed proxy 请求返回明确错误或 redirect view，不代理用户输入 URL。
- managed snapshot 不触发 FsOperator。
- managed 删除不删除云资源。

### 9.5 前端

- 切换模式清理互斥字段。
- capability 异步竞态。
- Managed/legacy action 路由。
- 无权限、漂移、失联和 Operation UNKNOWN 展示。

## 10. 受控开发进展

2026-07-29 已完成 B-01 的 Deploy Mode 基础切片：

- 新增 `MANAGED_APPLICATION(7, "managed-application")`，保留既有 0～6 数值映射。
- 新增 `isManagedMode`，且 managed 不属于 YARN、Kubernetes、Remote 或 Session。
- HTTP watcher 改为显式 legacy allowlist：Local、Remote、YARN Per-job、YARN Session、
  YARN Application。
- Cluster watcher 改为显式 legacy allowlist：Remote、YARN Session。
- 新增枚举与 watcher allowlist 单元测试，共 4 个测试通过。
- Console Service 已通过 Spotless、Apache RAT、Checkstyle 和 Java/Scala 编译；
  Provider 契约、Fake Provider 与 capability resolver 的 14 个回归测试通过。
- 新增 `ManagedFlinkRoutingGuard`，在 Managed Orchestrator 接管前阻止 mode 7 进入
  FlinkClient、FlinkEnv 和 FsOperator 旧链路。
- guard 已接入 start、cancel、revoke、abort、release build、application mapping/remove，
  以及 Savepoint 路径推导、触发和文件/workspace 删除入口。
- 新增 6 个 guard/入口隔离测试；连同 Provider/capability 回归共 20 个测试通过。
  Savepoint 与 Application Manage 的既有 Spring 测试共 5 个通过、1 个原有测试跳过。
- 新增 `ManagedFlinkFeatureGate`，统一绑定
  `streampark.managed-flink.enabled` 和
  `streampark.managed-flink.providers.volcengine.enabled`。两级开关默认均为 false，
  Provider 开关受全局开关约束。
- `requireWriteEnabled` 为后续 Managed Orchestrator、Controller 和 watcher 提供统一写门禁；
  配置样例与 Spring configuration metadata 已同步。
- Feature Gate 增加 6 个逻辑和 Spring 属性绑定测试；Managed 定向回归累计 26 个测试通过。

B-01 尚未整体完成。下列工作仍需后续切片完成：

- 将当前 fail-fast guard 前置接入 Managed Orchestrator，形成实际的 managed
  build、start、cancel、release、savepoint 和 remove 路由。
- history 和 proxy 的 managed 专用读取/外链行为。
- Managed watcher 与状态同步。
- 数据库双方言 schema/upgrade，以及 feature flag 在 Controller、菜单、watcher 和
  Managed Orchestrator 的实际接线与回滚保护。
- 前端 Deploy Mode、表单、操作按钮和 capability 联动。
- legacy Deploy Mode 的集成级零回归测试。

## 10. 实施顺序

1. 添加 enum 和 helper，只加测试，不接业务入口。
2. 为 watcher/query 添加显式 include/exclude 和回归测试。
3. 增加 managed persistence、feature flag 和只读展示。
4. 增加 managed service 和新 REST API。
5. 在 controller/action/build/savepoint 入口分流。
6. 增加前端 mode 和 managed 表单。
7. 最后开放 feature flag。

## 11. 审计退出标准

- 93 个命中文件均被标记为 `modify/test-only/no-change`。
- 所有 default switch 对 mode 7 的行为明确。
- P0 文件全部有 managed 负向测试。
- Remote/YARN/Kubernetes 原有测试通过。
- Spark 模块 diff 为零或只有共享类型编译调整。
