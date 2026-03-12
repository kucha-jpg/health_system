# 统一错误码对照表

## 1. 目的
- 统一后端错误语义，减少接口联调中的歧义。
- 为前端提示文案、重试策略和路由跳转提供稳定依据。
- 支撑报告/答辩中的“异常处理规范化”证据引用。

## 2. 通用返回结构
- 失败响应统一结构：`{ code, msg, data }`
- 失败时 `data` 固定为 `null`。

示例：
```json
{
  "code": 404,
  "msg": "用户不存在",
  "data": null
}
```

## 3. 错误码总览

| 错误码 | 名称 | 语义 | 典型触发场景 | 前端建议处理 |
|---|---|---|---|---|
| 400 | BAD_REQUEST | 请求参数不合法 | 指标格式错误、状态值非法、必填参数缺失 | 就地提示用户修正输入，不重试 |
| 401 | UNAUTHORIZED | 未登录或登录已过期 | token 缺失/过期/解析失败 | 清理会话并跳转登录 |
| 403 | FORBIDDEN | 已登录但无权执行 | 角色越权、账号禁用、资源无访问权限 | 提示无权限，保持当前页或回首页 |
| 404 | NOT_FOUND | 目标资源不存在 | 用户/规则/反馈/公告不存在 | 提示资源不存在并刷新列表 |
| 409 | CONFLICT | 状态冲突或唯一性冲突 | 用户名重复、规则重复、预警已处理 | 提示冲突原因，建议用户改名或刷新后重试 |
| 500 | INTERNAL_ERROR | 系统内部异常 | 非预期异常、未分类错误 | 展示通用失败文案并记录日志 |

## 4. 代码落点
- 错误码枚举：`backend/src/main/java/com/health/system/common/ErrorCode.java`
- 业务异常：`backend/src/main/java/com/health/system/common/BusinessException.java`
- 全局异常处理：`backend/src/main/java/com/health/system/common/GlobalExceptionHandler.java`
- 认证入口与拒绝处理：`backend/src/main/java/com/health/system/config/SecurityConfig.java`
- JWT 过滤器未授权返回：`backend/src/main/java/com/health/system/security/JwtAuthenticationFilter.java`

## 5. 联调检查清单
1. 触发参数错误时返回 `400`，且 `msg` 可直接展示给用户。
2. token 失效时返回 `401`，前端需清理会话并跳转登录页。
3. 越权访问管理员/医生接口时返回 `403`。
4. 删除或查询不存在资源时返回 `404`。
5. 重复创建（如用户名、规则）时返回 `409`。
6. 非预期异常保底返回 `500`。