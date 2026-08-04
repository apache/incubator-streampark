<!--
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

      https://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->

# StreamPark Console Webapp v2

Naive UI 原生重构版前端，Maven 打包已切换至本工程。

## 当前状态

| 项 | 状态 |
|---|---|
| UI 框架 | **Naive UI 2.x**（`NButton` / `NDataTable` / `NForm` 等原生写法） |
| Ant Design / vben 壳层 | **已移除**（无 ant-design-vue、无 BasicTable/BasicForm 封装） |
| HTTP | **alova**（`src/service/api/` + `src/service/http/`） |
| 图标 | **xicons**（`@vicons/ionicons5`，Naive UI 推荐用法） |
| 业务功能 | 与旧版同一套页面与 API |
| 生产构建 | `pnpm build` 通过 |
| 开发端口 | `10002`（旧版 `10001`） |

## 技术栈

- Vue 3.5 + Vite 8 + TypeScript
- Naive UI 2.x + [@vicons/ionicons5](https://www.xicons.org/)
- Pinia + Vue Router 5 + vue-i18n + Monaco Editor（懒加载）
- UnoCSS + alova

## 开发

```bash
cd streampark-console/streampark-console-webapp-v2
pnpm install
pnpm dev
```

后端 Console 需在 `localhost:10000` 运行，API 代理：`/basic-api → http://localhost:10000`

## 构建

```bash
pnpm build
pnpm preview
```

## Maven / 运维集成

Console 发行包通过 `streampark-console-service` 的 `-Pwebapp` profile 构建前端：

```bash
# 仅前端（在 webapp-v2 目录）
pnpm install && pnpm build

# 完整 Console 包（含 webapp-v2 dist）
./mvnw -pl streampark-console/streampark-console-service -am clean package -Pwebapp,dist -DskipTests
```

| 项 | 说明 |
|---|---|
| Maven 变量 | `frontend.project.name=streampark-console-webapp-v2` |
| 静态资源 | `streampark-console-service` assembly 拷贝 `../streampark-console-webapp-v2/dist` |
| CI | `.github/workflows/frontend.yml` — Node 20、`pnpm typecheck` + `pnpm build` only |
| 开发端口 | v2: `10002`；后端 API: `10000` |

## Quality checks

| Script | CI | Notes |
|---|---|---|
| `pnpm typecheck` | Yes | Vue/TS type check |
| `pnpm build` | Yes | Production build |
| `pnpm check:i18n` | No | Requires dev server at `localhost:10002` |
| `pnpm check:routes` | No | Requires dev server at `localhost:10002` |
| `pnpm check:login` | No | Requires dev server at `localhost:10002` |
| `pnpm lint:eslint` | No | ESLint (local) |
| `pnpm lint:prettier` | No | Prettier (local) |

## 目录说明

```
src/
├── service/            # alova HTTP 与 API 模块
├── types/api/          # 业务 DTO / 类型定义（无 axios 实现）
├── views/              # 业务页面（Naive UI 原生）
├── layouts/            # Nova 壳层布局（Tab / 主题 / 侧栏）
├── components/         # Icon、IonIcon 等共享组件
├── locales/            # i18n 文案
└── store/              # Pinia 状态
```

## 开发约定

- 列表页使用 `NDataTable` + 列 `render` / `h()`，分页用 `NPagination`
- 表单使用 `NForm` / `NFormItem`，字段绑定 `v-model:value` 或 `onUpdateValue`
- 布局与业务页图标统一 **@vicons/ionicons5**（`<n-icon><IonIcon /></n-icon>`）
- `IconSelect` 菜单图标选择器使用 Iconify `ion` / `carbon` 集合（兼容历史 `icon-park-outline:` 存量数据）
- Monaco 通过 `useMonaco().onUpdateValue` 订阅内容变更；编辑器懒加载见 `src/monaco/`
