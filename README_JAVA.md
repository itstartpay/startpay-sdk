# StartPay Java SDK 接入指南

## 安装

将 `StartPayClient.java` 拷贝到你的项目中。

## 初始化

```java
import startpay.StartPayClient;

String apiKey = "your_api_key";
String apiSecret = "your_api_secret";
```

## 发送 GET 请求

```java
Map<String, Object> params = new HashMap<>();
params.put("mchOrderNo", "ORDER123456");

String resp = StartPayClient.spGet(
    "https://w3api.startpay.ai/api/public/query_dc_order",
    params,
    apiSecret,
    apiKey,
    30  // timeout seconds
);
System.out.println(resp);
```

## 发送 POST 请求

```java
Map<String, Object> params = new HashMap<>();
params.put("mchOrderNo", "ORDER123456");
params.put("digitalCurrency", "USDT");
params.put("chain", "TRX");
params.put("orderAmount", 100);
params.put("subject", "商品名称");
params.put("notifyUrl", "https://your-domain.com/notify");

String resp = StartPayClient.spPost(
    "https://w3api.startpay.ai/api/public/create_dc_order",
    params,
    apiSecret,
    apiKey,
    30
);
System.out.println(resp);
```

## 回调验签

```java
@RestController
@RequestMapping("/callback")
public class CallbackController {

    @PostMapping("/notify")
    public String handleNotify(
            @RequestHeader("SP-SIGN") String spSign,
            @RequestHeader("SP-TIMESTAMP") String spTimestamp,
            @RequestBody Map<String, Object> body) {

        String method = "POST";
        String apiURL = "https://your-domain.com/callback/notify";
        String apiSecret = "your_api_secret";

        boolean valid = StartPayClient.verifySign(
            method, apiURL, body, spTimestamp, spSign, apiSecret);

        if (!valid) {
            return "Signature verification failed";
        }

        // 处理业务逻辑
        System.out.println("订单: " + body.get("orderId"));
        return "SUCCESS";
    }
}
```

## 签名算法说明

| 项目 | 说明 |
|------|------|
| 算法 | HMAC-SHA1 + Base64 |
| 参数排序 | 按 key 字母升序 (TreeMap) |
| 签名字符串 | `METHOD + URL + ? + sortedParams + timestamp` |
| Timestamp | 秒级 (System.currentTimeMillis() / 1000) |

## 嵌套结构格式化

嵌套 Map/List 使用 Go fmt.Sprintf("%v") 格式化规则：

```
Map:   map[key1:val1 key2:val2]
List:  [val1 val2 val3]
```
