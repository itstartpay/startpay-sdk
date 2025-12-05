# StartPay Python SDK

这是StartPay的Python SDK，提供了与StartPay API交互的便捷方法。

## 功能特性

- HMAC-SHA1签名生成和验证
- GET/POST请求封装
- 回调签名验证
- 纯Python实现，无外部依赖

## 安装

将`startpay.py`文件复制到您的项目中，然后导入即可使用。

## 使用方法

### 导入模块

```python
from startpay import sp_get, sp_post_json, verify_sign
```

### GET请求示例

```python
# 配置
api_secret = "your_api_secret"
api_key = "your_api_key"
api_url = "https://test.bakbak.cn/api/public/query_dc_order"

# 请求参数
params = {
    "mchOrderNo": "ORDER1872983743393"
}

# 发送GET请求
try:
    response = sp_get(api_url, params, api_secret, api_key)
    print(response)
except Exception as e:
    print(f"请求失败: {str(e)}")
```

### POST JSON请求示例

```python
# 配置
api_secret = "your_api_secret"
api_key = "your_api_key"
api_url = "https://test.bakbak.cn/api/public/create_dc_order"

# 请求参数
params = {
    "mchOrderNo": "ORDER1239234230",
    "digitalCurrency": "USDT",
    "chain": "BSC",
    "orderAmount": 100,
    "subject": "游戏点卡",
    "detail": "游戏点卡3个",
    "notifyUrl": "https://example.com/callback"
}

# 发送POST请求
try:
    response = sp_post_json(api_url, params, api_secret, api_key)
    print(response)
except Exception as e:
    print(f"请求失败: {str(e)}")
```

### 签名验证示例（回调处理）

```python
from startpay import verify_sign

# 从请求中获取参数
method = "POST"  # 请求方法
url = "https://example.com/callback"  # 你的回调URL
params = {
    "orderId": "2021102200001",
    "amount": 100.50,
    "status": "SUCCESS"
}
timestamp = "1634870400"  # 从SP-TIMESTAMP头获取
sign = "received_signature"  # 从SP-SIGN头获取
api_secret = "your_api_secret"

# 验证签名
is_valid = verify_sign(method, url, params, timestamp, sign, api_secret)

if is_valid:
    print("签名验证成功")
    # 处理业务逻辑
else:
    print("签名验证失败")
    # 拒绝请求
```

## API参考

### sp_get(api_url, params, api_secret, api_key, timeout_sec=10)

发送GET请求到StartPay API。

**参数:**
- `api_url` (str): API地址
- `params` (dict): 请求参数
- `api_secret` (str): API密钥
- `api_key` (str): API Key
- `timeout_sec` (int, 可选): 超时时间（秒），默认为10

**返回:**
- `str`: 响应内容

**异常:**
- `Exception`: 请求失败时抛出异常

### sp_post_json(api_url, params, api_secret, api_key, timeout_sec=10)

发送POST JSON请求到StartPay API。

**参数:**
- `api_url` (str): API地址
- `params` (dict): 请求参数
- `api_secret` (str): API密钥
- `api_key` (str): API Key
- `timeout_sec` (int, 可选): 超时时间（秒），默认为10

**返回:**
- `str`: 响应内容

**异常:**
- `Exception`: 请求失败时抛出异常

### verify_sign(method, api_url, params, timestamp, sign, api_secret)

验证StartPay回调签名。

**参数:**
- `method` (str): 请求方法 ("POST" 或 "GET")
- `api_url` (str): 回调URL
- `params` (dict): 回调参数
- `timestamp` (str): 请求头中的SP-TIMESTAMP
- `sign` (str): 请求头中的SP-SIGN
- `api_secret` (str): API密钥

**返回:**
- `bool`: True表示验签成功，False表示验签失败

## 环境说明

- 测试环境域名: `test.bakbak.cn`
- 正式环境域名: `w3api.startpay.ai`

## 注意事项

1. 请确保在生产环境中使用正式环境的API密钥和域名
2. 回调处理时请做好幂等处理，因为网络原因可能导致多次回调
3. 响应HTTP状态码为200代表回调成功

## 示例代码

查看`example.py`文件获取完整的使用示例。

## 许可证

MIT License