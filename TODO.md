# 分布式平台改进清单

基于当前代码现状梳理的分布式演进待办，按优先级分组。每项标注「现状 → 目标」与落地方向。

## P0 安全与正确性（上线前必做）

- [ ] **Token 无 TTL / 自动过期**
  现状：`RedisTokenStore` 不设 TTL，token 永驻 Redis；内存版靠重启兜底，Redis 版会无限累积。`AuthController.logout` 也只删一个 token。
  方向：`create` 写入后 `redis.expire(token, Duration)` 设会话超时；接入「滑动续期」或「活跃时自动续期」策略；明确单设备/多设备并发登录策略。

- [ ] **Redis 阻塞调用落在 Netty 事件循环**
  现状：`TokenUserDetailsService.findByUsername`（`Mono.justOrEmpty(...)` 装配阶段同步求值）、`UserHeaderGlobalFilter`、`ValidateCodeFilter` 在响应式链里同步调用 `TokenStore`/`ValidateCodeStore`，Redis 实现下每次请求把阻塞 GET/DELETE 放到事件循环线程。
  方向：Redis 实现改用 `ReactiveRedisTemplate`，接口方法返回 `Mono`/`Flux`；或至少把阻塞调用包到 `Schedulers.boundedElastic()`。这是改 Redis 存储时埋下的债，需要先确认接口签名是否 reactive 化。

- [ ] **验证码 / 短信发送无频率限制（防刷）**
  现状：`ValidateCodeService.sendCode` 无「同手机号 60s 内仅一次」「同 IP/设备日发送上限」逻辑；`/sms/{mobile}/register` 直接暴露在 permitAll，可被刷短信轰炸。
  方向：在 `sendCode` 前加频率校验（内存版用 Caffeine 滑动窗口，Redis 版用 `INCR + EXPIRE` 做分布式计数器）；按 mobile、deviceId、客户端 IP 三维度限流。

- [ ] **身份头内部链路信任问题**
  现状：`UserHeaderGlobalFilter` 用 `set` 覆盖写 `X-User-Id`/`X-User-Mobile` 防客户端伪造，但下游服务无条件信任这些头。若下游服务端口直接可达（绕过网关），或网关→下游链路上有其他注入点，身份可被伪造。
  方向：下游服务绑定监听内网/仅网关可达（网络层隔离），或网关↔下游间走 mTLS，或下游校验来源 IP 白名单。

- [ ] **权限硬编码，且为登录时刻快照**
  现状：`AuthController.issueToken` 硬编码 `userId=1 → admin`；`TokenStore` 存的是登录时刻权限快照，权限变更（角色调整、封禁）无法实时生效，需退出重登。
  方向：接入 DB 角色权限服务；权限刷新策略——要么改实时校验（网关持有角色缓存 + 订阅失效），要么 token 续期/刷新时重拉权限。

## P1 弹性与容错

- [ ] **网关无限流 / 熔断**
  现状：网关未引入 actuator/resilience4j/限流组件；`AuthController` 调 `lb://platform` 无重试/熔断，platform 抖动直接 503。
  方向：引入 `spring-cloud-starter-circuitbreaker-resilience4j`，对登录/注册/发短信等关键下游加熔断 + 有限重试；全局加限流（Spring Cloud Gateway `RequestRateLimiter` + Redis）。

- [ ] **WebClient 无超时 / 连接池配置**
  现状：`WebClientConfig` 仅 `WebClient.builder()`，无 `responseTimeout`、连接池上限、`connectTimeout`。下游慢响应可耗尽连接拖垮网关。
  方向：自定义 `HttpClient` → `connectTimeout` + `responseTimeout` + `responseTimeout`；`ConnectionProvider` 配置最大连接、pending 队列上限。

- [ ] **Dubbo 调用无超时 / 重试 / 降级**
  现状：`erp → platform` Dubbo 链路 application.yml 未配 `timeout`/`retries`/`cluster`，使用默认值；无降级策略。
  方向：按接口配 `dubbo.consumer.timeout`、`retries`（幂等才开）、`cluster=failfast/failover`；关键链路加 mock 降级。

- [ ] **网关多实例部署未明确**
  现状：token/验证码已可切 Redis 支持多实例共享，但网关本身无多实例 + 前置 LB 的部署说明，仍是事实单点。
  方向：网关多实例 + 前置 SLB/Nginx；session 频率限制等走 Redis；文档化部署拓扑。

## P2 可观测性

- [ ] **无健康检查 / 就绪探针**
  现状：网关与各服务均未引入 `spring-boot-starter-actuator`，无 `/health` 端点，K8s 无 liveness/readiness 探针可用。
  方向：引入 actuator，暴露 `health`、`info`；区分 liveness（进程存活）与 readiness（Nacos 注册完成、Redis 连通）。

- [ ] **无指标 / Prometheus 接入**
  现状：无 `micrometer-registry-prometheus`，无 QPS/延迟/错误率/熔断态指标。
  方向：actuator + micrometer + prometheus，网关暴露路由级指标、登录/发码/验证码校验指标。

- [ ] **无链路追踪（traceId 未贯穿）**
  现状：网关→下游 HTTP、Dubbo 链路无统一 traceId，跨服务排障只能靠日志文本拼接。
  方向：引入 `micrometer-tracing` + OpenTelemetry（或 Nacos/SkyWalking），网关注入 traceId 并经 `UserHeaderGlobalFilter` 同款机制透传 `traceparent` 头，Dubbo 经 attachment 传播；日志格式带 traceId。

## P2 配置与部署

- [ ] **无统一动态配置中心**
  现状：用 Nacos 做注册发现，但配置在本地 `application.yml` + `.env.*.properties`，多实例下改配置需重新打包/重启。
  方向：接入 Nacos config（`spring.config.import: nacos:`），验证码规则、限流阈值、token TTL 等可热更新。

- [ ] **机密管理在本地文件**
  现状：`ID`/`SECRET`/`SIGN_NAME`/`JDBC.*` 走 gitignored 的 `.env.*.properties`，分布式部署/多实例分发不安全。
  方向：K8s Secret 挂载，或 Nacos 加密配置，或 Vault；CI/CD 注入。

- [ ] **无容器化 / 部署清单**
  现状：仓库无 `Dockerfile`、`docker-compose`、K8s manifests。
  方向：各模块 `bootBuildImage` 或多阶段 Dockerfile；docker-compose 一键拉起 Nacos/PG/Redis/服务；K8s Deployment + Service + Ingress。

## P3 测试与质量

- [ ] **Redis 存储实现无测试**
  现状：`RedisTokenStore` / `RedisValidateCodeStore` 无测试覆盖（项目测试约定自足、不依赖真实中间件）。
  方向：引入 `testcontainers-redis` 或 embedded-redis，覆盖 put/get/consume/remove + TTL 过期 + 并发消费语义。

- [ ] **缺分布式场景测试**
  现状：无多实例 token 一致性、限流计数器跨实例、Dubbo 身份透传 attachment 的集成测试。
  方向：补 `ValidateCodeIntegrationTest` 同形态的端到端用例覆盖跨服务身份链路。

## P3 其他

- [ ] **跨服务事务 / 数据一致性**
  现状：`/auth/register` 涉及 platform 写库 + 网关签发 token，无补偿/幂等机制；失败语义靠 `onErrorResume` 兜底。
  方向：注册等跨写操作加幂等键 + 失败补偿；或对齐业务接受最终一致。

- [ ] **安全响应头缺失**
  现状：SecurityConfig 仅处理 401/403 JSON，未加 `X-Content-Type-Options`、`X-Frame-Options`、`Strict-Transport-Security`、`Content-Security-Policy` 等响应头。
  方向：在 security 链加 `ServerHttpSecurity.headers(...)` 统一下发。

- [ ] **CORS 策略过于宽松**
  现状：`allowedOriginPatterns: ["*"]` + `allowCredentials: true`，生产应收敛为可信域名列表。
  方向：按环境配置允许的 origin 列表。

- [ ] **logout 调试输出残留**
  现状：`AuthController.logout` 有 `System.out.println(principal)`，应改为结构化日志或移除。
