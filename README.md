# StartPay SDK

商户接入 StartPay 支付网关

## 接入指南

| 语言 | 文档 |
|------|------|
| Go | [README_GO.md](./README_GO.md) |
| Java | [README_JAVA.md](./README_JAVA.md) |

## 签名算法

所有语言的签名算法保持一致：

| 项目 | 说明 |
|------|------|
| 算法 | HMAC-SHA1 + Base64 |
| 参数排序 | 按 key 字母升序 |
| 签名字符串 | `METHOD + URL + ? + sortedParams + timestamp` |
| Timestamp | 秒级 Unix 时间戳 |
