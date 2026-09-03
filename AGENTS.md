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

# Apache StreamPark — Agent Instructions

Project conventions, architecture, and coding patterns for the StreamPark codebase.

## Architecture

### Module Boundaries

StreamPark is a Maven multi-module project with five top-level reactor modules. Each has a clear responsibility boundary.

- **`streampark-common`** (`streampark-common/`): Engine-API-free foundation layer. It owns the immutable configuration model (`ConfigOption`, `Configuration`, `ConfigurationLoader`, and `GlobalConfiguration`), shared option catalogs, workspace layout, utilities, file system abstractions, and common enums. It may understand external formats such as Flink YAML, but must not depend on Flink or Spark runtime APIs.

- **`streampark-scala`** (`streampark-scala/`): Small Scala compatibility layer containing shared Scala logging and implicit utilities. Java-only common code belongs in `streampark-common`; reusable Scala code belongs here instead of being embedded in an engine module.

- **`streampark-flink`** (`streampark-flink/`): Flink runtime integration. Its reactor contains the version-isolated shims, SQL client, submission client, packer, and Kubernetes integration. Connector modules are enabled by the module's `shaded` profile. `FlinkShimsProxy` in the client API is the entry point for executing version-specific code.

- **`streampark-spark`** (`streampark-spark/`): Spark runtime integration. It contains Spark configuration and SQL utilities, submission client API/core, and SQL client. It is a regular root reactor module; the retained `spark` profile is not required to include it in a normal build.

- **`streampark-console`** (`streampark-console/`): Web management platform. Its Maven reactor contains the Spring Boot service. The sibling `streampark-console-webapp/` Vue 3 application is built into the service only when the service's `webapp` profile is enabled; it is not a Maven child module.

The strongest compatibility contracts are the common configuration keys and value semantics, Flink client request/response types, shims proxy serialization boundary, database schema, and REST API. Changes to these surfaces require cross-module impact analysis and focused compatibility tests.

### High-Sensitivity Areas

- **Configuration model** (`org.apache.streampark.common.configuration`): `ConfigOption` declares typed metadata, `ConfigurationParser` parses raw documents, `ConfigurationLoader` composes ordered sources, and immutable `Configuration` snapshots perform typed reads. `GlobalConfiguration` is the process-boundary atomic reference, not a mutable property bag. Option keys and fallback keys are user-facing contracts.

- **Configuration ownership**: Common option catalogs contain only settings owned by common infrastructure. Console and engine settings stay in their owning modules. `SpringConfigurationInitializer` is the Console composition root and owns the explicit list of options bound from Spring; do not add global `ALL` registries to option catalogs.

- **Workspace initialization**: `Workspace.LOCAL`, `Workspace.REMOTE`, and derived constants are initialized from one immutable snapshot. Spring bootstrap must publish its completed configuration before workspace paths are first accessed, and must retain the `Workspace.verifyInitializedFrom` guard.

- **Flink YAML compatibility** (`FlinkConfigurationUtils`): Before Flink 1.19, load only `flink-conf.yaml` with the legacy line parser. Flink 1.19 and 1.20 support both names and prefer `flink-conf.yaml` when both exist; parser selection follows the selected filename. Flink 2.0 and later load only `config.yaml` with standard nested YAML parsing. Directory-based loading must always receive the target Flink version. This common utility returns engine-neutral maps; conversion to Flink's `Configuration` belongs in a Flink module.

- **`FlinkShimsProxy`**: The multi-version classloader isolation mechanism uses `ChildFirstClassLoader` and serializes request/response objects across the boundary. Classloaders are cached by concrete Flink version. Never pass target-runtime Flink objects into the parent classloader or introduce target-specific static state that can leak across classloaders.

- **Shims modules**: `streampark-flink-shims-base` contains only contracts and implementation shared by every supported shim. Supported concrete modules are Flink 1.18, 1.19, 1.20, 2.0, 2.1, 2.2, and 2.3. APIs removed from Flink 2.x, such as legacy `registerDataStream` and table-function `registerFunction` overloads, belong only in the compatible 1.x implementations. Do not add classes or methods anywhere under `streampark-flink-shims` without an explicit shims architecture decision.

- **Flink submission flow**: `FlinkClient` and its request/response packages form the stable API. `FlinkClientEntrypoint`, `SubmitRequestResolver`, `SubmissionConfigurationBuilder`, and deployment-specific clients own request normalization, configuration assembly, and submission. Keep Flink-version calls behind the shims boundary.

- **`FlinkApplicationController` / `FlinkApplicationManageService` / `FlinkApplicationActionService`**: The core application management flow. Operations (start, stop, cancel, deploy) must be idempotent and handle all Flink states correctly. The `AppChangeEvent` annotation triggers state synchronization.

- **Persistence entities** (`console/core/entity`): These classes map database rows. Keep configuration discovery, parsing, filesystem access, and other business logic in assemblers, services, or utility classes such as `FlinkEnvUtils` and `FlinkApplicationConfigUtils`. Existing small mapping operations may remain, but new domain workflows must not be added to entities.

- **SQL parsing and validation**: Flink SQL validation is version-aware and executes through the selected 1.18-2.3 shim. `FlinkSql`, `FlinkSqlService`, and the SQL client have different persistence, orchestration, and execution responsibilities. `sql-rev.dict` handles database SQL differences between MySQL and PostgreSQL; it is unrelated to Flink SQL syntax compatibility.

- **Kubernetes integration** (`FlinkKubernetesWatchController`): Uses Caffeine caches (`TrackIdCache`, `JobStatusCache`, `MetricCache`) for tracking K8s-deployed Flink jobs. Cache invalidation and TTL must be correct to avoid stale state.

- **HTTP client and proxying**: `OkHttpUtils` in common owns the shared connection pool, timeouts, and bounded retries for idempotent requests. Console proxy code in `WebUtils` owns servlet adaptation, hop-by-hop header filtering, response streaming, and response closure. Do not create ad hoc clients per request or retry non-idempotent methods implicitly.

- **Database schema changes**: All schema changes must have corresponding upgrade scripts under `streampark-console/streampark-console-service/src/main/assembly/script/upgrade/` for both `mysql/` and `pgsql/`. Update `sql-rev.dict` when a mapper or initialization statement needs a MySQL-to-PostgreSQL rewrite.

- **Authentication & Authorization**: `ShiroConfig`, `JWTUtil`, `ShiroRealm` — changes here affect all user access. The `@Permission` annotation and `PermissionAspect` enforce team-level resource isolation. Never weaken RBAC checks.

## Design Patterns

- **Immutable configuration pipeline**: Declare typed options in the owning module, parse external input without a central registry, compose sources by precedence, and capture one immutable snapshot at the start of a multi-step operation. Do not repeatedly read mutable global state inside a workflow.

- **Shims / Proxy pattern**: `FlinkShimsProxy.proxy(flinkVersion, function)` loads the target installation and matching shim behind a child-first classloader. Shared behavior stays in shims-base; version-only API calls stay in the concrete version module. Cross-boundary values must use stable serializable StreamPark request/response types.

- **Initializer pattern**: `FlinkStreamInitializer` and `FlinkTableInitializer` assemble application configuration namespaces and construct Flink environments inside the target classloader. Native `flink.property.*`, user `app.*`, and command-line sources remain distinct until their documented composition point.

- **Service layer separation**: Console services are split by responsibility — `FlinkApplicationManageService` (CRUD), `FlinkApplicationActionService` (start/stop/cancel), `FlinkApplicationInfoService` (query/info). Follow this pattern when adding new application operations.

- **Scala compatibility layer**: Shared Scala helpers live in `streampark-scala`. New implicits must be narrowly scoped and must not introduce engine dependencies into the common foundation.

- **MyBatis-Plus entity pattern**: Mappers extend MyBatis-Plus `BaseMapper`; entities use `@TableName` and related mapping annotations. Only entities that need the shared audit fields extend `BaseEntity`. Pagination uses `MybatisPager` and `PaginationInterceptor`. Keep entities focused on persistence data.

- **Typed request and option APIs**: Prefer typed enums, request objects, `ConfigOption`, and immutable maps over unstructured string bags. Preserve serialized field names across the Console-to-client and client-to-shims boundaries.

- **REST response pattern**: Normal API endpoints return `RestResponseBody<T>` created through `RestResponseBody.success(...)` or `RestResponseBody.fail(...)`. The map-based `RestResponse` is a deprecated compatibility type and must not be introduced at new controller boundaries. Streaming proxy endpoints may write directly to `HttpServletResponse`. Use `@Permission` for protected resources and `@AppChangeEvent` for audited application changes.

- **File system abstraction**: Use `FsOperator` (with `HdfsOperator` / `LfsOperator` implementations) for file operations. Never use raw `java.io.File` or Hadoop `FileSystem` directly in business logic.

## Coding Conventions

### Java (Backend)

- **Formatting**: Eclipse formatter via Spotless (`tools/checkstyle/spotless_streampark_formatter.xml`). Run `./mvnw spotless:apply` before committing.
- **Import order**: `org.apache.streampark`, `org.apache.streampark.shaded`, `org.apache`, `javax`, `java`, `scala`, `\#` (all others).
- **Static checks**: Checkstyle (`tools/checkstyle/checkstyle.xml`) + Spotless. No wildcard imports. No `@author` tags. No JUnit 4 imports.
- **Lombok**: Use `@Slf4j`, `@Data`, `@Builder` where appropriate. Do not use `@EqualsAndHashCode` on JPA/Hibernate entities.
- **Testing**: JUnit 5 (`org.junit.jupiter`) + AssertJ. Use `@Test` (not `@Test` from JUnit 4). Use `assertThat(...).isEqualTo(...)` style. Test classes should be in the same package as the code under test.
- **Method names**: New production and test method names must be concise, readable, and no longer than 40 characters.
- **Package structure**: Controllers in `controller/`, service interfaces in `service/`, implementations in `service/impl/`, entities in `entity/`, mappers in `mapper/`, enums in `enums/`.

### Scala (Scala Support / Spark)

- **Formatting**: Scalafmt 3.7.5 (`tools/checkstyle/.scalafmt.conf`). Max column 160. Run `./mvnw spotless:apply` to format.
- **Import ordering**: `org.apache.streampark.*` first, then other third-party, then `javax.*`, `java.*`, `scala.*`.
- **Static checks**: Scalastyle (`tools/checkstyle/scalastyle-config.xml`). No wildcard imports. No `println` statements (use `Logger` trait).
- **Testing**: ScalaTest 3.2.9. Use `FlatSpec` or `FunSuite` style consistent with existing tests.
- **Style**: Use `val` over `var`. Prefer `Option` over `null` in public APIs. Use `lazy val` for expensive initialization. Use pattern matching instead of `isInstanceOf`/`asInstanceOf`.

### TypeScript / Vue (Frontend)

- **Formatting**: ESLint + Prettier. Run `pnpm lint:eslint` and `pnpm lint:prettier`.
- **Vue 3 Composition API**: Use `<script setup lang="ts">` for new components. Use Pinia for state management (not Vuex).
- **API layer**: All HTTP calls go through `src/api/` modules using `defHttp`. API URLs are defined as enum constants. Never use `axios` or `fetch` directly in components.
- **Component naming**: PascalCase for component files and names. Use `index.ts` barrel exports in component directories.
- **Unused variables**: Prefix with `_` to suppress ESLint warnings (`argsIgnorePattern: '^_'`).

### General

- **Apache License header**: Required on all new files. Use the copyright header from `tools/checkstyle/copyright.txt`. Enforced by Spotless and Apache RAT.
- **No wildcard imports**: Prohibited in both Java and Scala (enforced by Spotless).
- **No personal pronouns in comments**: Use descriptive, impersonal documentation.
- **Database**: Support both MySQL and PostgreSQL. All new SQL must be tested against both. Use `sql-rev.dict` for dialect differences.
- **Logging**: Use Lombok `@Slf4j` (Java) or `Logger` trait (Scala). Use `log.info`/`log.error` with parameterized messages. Never log credentials or sensitive data.

## Commands

- **Full build (backend + frontend, skip tests):**
  ```shell
  ./build.sh
  ```
  Equivalent to: `./mvnw -Pshaded,webapp,dist -DskipTests clean install`

- **Fast build (backend only, skip all checks):**
  ```shell
  ./mvnw -Pfast clean install -DskipTests
  ```

- **Build backend reactor with shaded artifacts:**
  ```shell
  ./mvnw -Pshaded clean install -DskipTests
  ```

- **Build backend only:**
  ```shell
  ./mvnw clean install -DskipTests
  ```

- **Run single test class (Java):**
  ```shell
  ./mvnw test -pl streampark-console/streampark-console-service -Dtest=FlinkSavepointServiceTest
  ```

- **Run common configuration tests:**
  ```shell
  ./mvnw test -pl streampark-common -Dtest=ConfigurationTest,FlinkConfigurationUtilsTest
  ```

- **Build all supported Flink shims:**
  ```shell
  ./mvnw -f streampark-flink/streampark-flink-shims/pom.xml clean install
  ```

- **Format code (Java + Scala):**
  ```shell
  ./mvnw spotless:apply
  ```

- **Check formatting:**
  ```shell
  ./mvnw spotless:check
  ```

- **Run Checkstyle:**
  ```shell
  ./mvnw checkstyle:check
  ```

- **Frontend development server:**
  ```shell
  cd streampark-console/streampark-console-webapp && pnpm dev
  ```

- **Frontend lint:**
  ```shell
  cd streampark-console/streampark-console-webapp && pnpm lint:eslint && pnpm lint:prettier
  ```

- **Frontend build:**
  ```shell
  cd streampark-console/streampark-console-webapp && pnpm build
  ```

- **Run Docker Compose (local):**
  ```shell
  docker compose -f docker/docker-compose.yaml up -d
  ```

## PR & Commit Conventions

- **PR title format**: `[Module] Description` (e.g., `[Flink] Fix shims classloader isolation`, `[Console] Add team-level resource filtering`, `[Common] Support Hadoop 3.x configuration`).
- **Module prefixes**: `[Common]`, `[Flink]`, `[Spark]`, `[Console]`, `[K8s]`, `[Docs]`, `[CI]`, `[Build]`.
- **One concern per PR**: Unrelated whitespace, import, or formatting changes go in separate PRs. Do not mix refactoring with feature work.
- **Commit messages**: Describe the *what* and *why*, not implementation details. Reference related GitHub issues with `#xxx`.
- **Apache License header**: Required on all new files (enforced by Spotless and Apache RAT). The `spotless:check` and `apache-rat:check` goals run in CI.
- **Schema changes**: Must include matching scripts for MySQL and PostgreSQL under `streampark-console/streampark-console-service/src/main/assembly/script/upgrade/`.
- **Shims changes**: Keep shared methods in shims-base only when every supported version can implement them. Put version-specific Flink APIs only in the applicable 1.18-2.3 concrete modules, and verify the full shims reactor.

## Boundaries

### Never Without Explicit Discussion

- **Never** modify `.asf.yaml`, `LICENSE`, `NOTICE`, or `.gitignore` without explicit discussion.
- **Never** upgrade Flink, Spark, Scala, Spring Boot, or other major dependency versions without discussion — these changes have broad impact across the entire project.
- **Never** remove or rename canonical or fallback configuration keys without an explicit compatibility decision; they are user-facing contracts.
- **Never** add classes or methods under `streampark-flink-shims` without an explicit architecture decision. General utilities, Console logic, and configuration-file parsing belong outside shims.
- **Never** add business workflows or infrastructure access to `streampark-console-service/.../core/entity`.
- **Never** commit secrets, credentials, API keys, or cloud-specific tokens.
- **Never** introduce a new Flink version shims module without adding the corresponding CI build configuration.
- **Never** add a new database migration without providing both MySQL and PostgreSQL upgrade scripts.
- **Never** change the `SqlConvertUtils` dialect conversion logic without testing against both MySQL and PostgreSQL.

### Ask First

- **Ask first** before adding new third-party dependencies — license compatibility with Apache 2.0 matters.
- **Ask first** before promoting package-private classes/methods to public.
- **Ask first** before adding new Maven modules or restructuring the module hierarchy.
- **Ask first** before introducing new Scala implicits in `streampark-scala` — they affect all downstream Scala code.
- **Ask first** before changing the authentication model (Shiro/JWT/Pac4j configuration).
