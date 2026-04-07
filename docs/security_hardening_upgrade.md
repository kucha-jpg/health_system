# 系统安全机制完善说明（第四点）

日期：2026-04-07

## 1. 改造目标

针对“系统安全机制仍需进一步完善（优先级：中）”，本次改造围绕以下四点实施：

1. 完善接口权限控制与防护能力
2. 加强敏感数据加密存储与传输保护
3. 强化输入校验，降低注入风险
4. 增强日志审计，提高可追溯性

## 2. 已落地改造

### 2.1 安全头与传输侧防护

在安全配置中增加浏览器侧安全头：

- `X-Content-Type-Options`
- `X-Frame-Options: DENY`
- `Strict-Transport-Security`
- `Referrer-Policy: strict-origin-when-cross-origin`
- `Content-Security-Policy: default-src 'self'; frame-ancestors 'none'`

实现文件：

- `backend/src/main/java/com/health/system/config/SecurityConfig.java`

### 2.2 敏感数据加密存储（患者档案）

新增 AES-GCM 敏感数据加密组件，对患者档案中的敏感字段进行“写入加密、读取解密”：

- `medicalHistory`
- `medicationHistory`
- `allergyHistory`

实现文件：

- 组件：`backend/src/main/java/com/health/system/common/SensitiveDataCipher.java`
- 患者档案服务：`backend/src/main/java/com/health/system/service/impl/PatientArchiveServiceImpl.java`
- 医生洞察服务：`backend/src/main/java/com/health/system/service/impl/DoctorPatientInsightServiceImpl.java`
- 配置项：`backend/src/main/resources/application.yml` 中 `security.data.aes-key`

说明：当未配置密钥时，组件以兼容模式运行（不加密），避免历史环境直接中断。

### 2.3 输入校验与注入防护增强

新增统一输入清洗工具，对关键词和角色参数进行合法性校验：

- 关键字长度限制
- 常见 SQL 注入关键片段拦截（如 `--`, `/*`, `;`, `union`, `drop` 等）
- 角色参数白名单校验（ADMIN/DOCTOR/PATIENT）

实现文件：

- `backend/src/main/java/com/health/system/common/SecurityInputSanitizer.java`
- `backend/src/main/java/com/health/system/common/QueryDtoBuilders.java`
- `backend/src/main/java/com/health/system/service/impl/UserServiceImpl.java`

### 2.4 安全审计日志增强

在认证流程中增加安全事件日志记录，覆盖登录与注册关键事件：

- 登录成功：`LOGIN_SUCCESS`
- 登录失败（凭据错误）：`LOGIN_FAILED_BAD_CREDENTIALS`
- 登录失败（账号禁用）：`LOGIN_FAILED_DISABLED_ACCOUNT`
- 注册成功：`REGISTER_SUCCESS`
- 注册失败（用户名重复）：`REGISTER_FAILED_DUPLICATE_USERNAME`

实现文件：

- `backend/src/main/java/com/health/system/service/impl/AuthServiceImpl.java`

## 3. 预期效果

1. 提升浏览器与接口层面的基础防护能力。
2. 降低患者敏感健康信息明文存储风险。
3. 提前阻断可疑输入，降低注入攻击面。
4. 形成更完整的认证审计轨迹，便于追溯与运维排障。

## 4. 配置建议

生产环境建议配置：

- `APP_DATA_AES_KEY`：建议使用长度不低于 16 字节的随机高强度密钥
- 强制 HTTPS 终端与反向代理传输
- 按周期轮换 JWT 与数据加密密钥（配合密钥托管策略）
