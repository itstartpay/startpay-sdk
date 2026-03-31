# StartPay Go SDK 接入指南

## 安装

```bash
go get github.com/itstartpay/startpay-sdk/startpay-go
```

## 初始化

```go
import "github.com/itstartpay/startpay-sdk/startpay-go/startpay"

apiKey := "your_api_key"
apiSecret := "your_api_secret"
```

## 发送 GET 请求

```go
params := map[string]interface{}{
    "mchOrderNo": "ORDER123456",
}

resp, err := startpay.SpGet(
    "https://w3api.startpay.ai/api/public/query_dc_order",
    params,
    apiSecret,
    apiKey,
    30, // timeout seconds
)
if err != nil {
    log.Fatal(err)
}
fmt.Println(resp)
```

## 发送 POST 请求

```go
params := map[string]interface{}{
    "mchOrderNo":      "ORDER123456",
    "digitalCurrency": "USDT",
    "chain":           "TRX",
    "orderAmount":     100,
    "subject":         "商品名称",
    "notifyUrl":       "https://your-domain.com/notify",
}

resp, err := startpay.SpPostJson(
    "https://w3api.startpay.ai/api/public/create_dc_order",
    params,
    apiSecret,
    apiKey,
    30,
)
if err != nil {
    log.Fatal(err)
}
fmt.Println(resp)
```

## 回调验签

```go
func HandleCallback(c *gin.Context) {
    method := "POST"
    apiURL := "https://your-domain.com/notify"
    
    // 从 body 解析参数
    var params map[string]interface{}
    json.NewDecoder(c.Request.Body).Decode(&params)
    
    timestamp := c.GetHeader("SP-TIMESTAMP")
    sign := c.GetHeader("SP-SIGN")
    
    valid := startpay.VerifySign(method, apiURL, params, timestamp, sign, apiSecret)
    if !valid {
        c.JSON(401, gin.H{"error": "invalid signature"})
        return
    }
    
    // 处理业务逻辑
    c.JSON(200, gin.H{"status": "success"})
}
```

## 签名算法说明

| 项目 | 说明 |
|------|------|
| 算法 | HMAC-SHA1 + Base64 |
| 参数排序 | 按 key 字母升序 |
| 签名字符串 | `METHOD + URL + ? + sortedParams + timestamp` |
| Timestamp | 秒级 Unix 时间戳 |

## 嵌套结构格式化

嵌套 Map/List 使用 `fmt.Sprintf("%v")` 格式化：

```
Map:   map[key1:val1 key2:val2]
List:  [val1 val2 val3]
```
