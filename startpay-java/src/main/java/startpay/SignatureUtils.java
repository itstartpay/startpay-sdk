package startpay;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class SignatureUtils {

    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";

    /**
     * 计算签名（HMAC-SHA1 + Base64）
     *
     * @param message 待签名消息
     * @param secret 密钥
     * @return Base64 编码的签名
     */
    public static String signMessage(String message, String secret) {
        try {
            // 使用 HMAC-SHA1 算法
            SecretKeySpec signingKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA1_ALGORITHM);
            Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
            mac.init(signingKey);

            // 计算 HMAC-SHA1 原始字节数组
            byte[] rawHmac = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));

            // Base64 编码
            return Base64.getEncoder().encodeToString(rawHmac);

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            // 实际应用中应更优雅地处理异常，例如记录日志
            throw new RuntimeException("Error signing message.", e);
        }
    }

    /**
     * 将参数按 key 升序拼接为 k=v&k2=v2（不做 urlencode；用于签名）
     *
     * @param data 参数 Map
     * @return 排序后的查询字符串
     */
    public static String mapToSortedQueryString(Map<String, ?> data) {
        if (data == null || data.isEmpty()) {
            return "";
        }

        // 使用 TreeMap 保证 key 的自然升序
        Map<String, ?> sortedData = new TreeMap<>(data);

        List<String> pairs = new ArrayList<>();
        for (Map.Entry<String, ?> entry : sortedData.entrySet()) {
            String key = entry.getKey();
            // 注意：PHP 中 (string)$v 确保了值是字符串。
            // 在 Java 中，我们调用 toString() 或使用 String.valueOf()。
            Object value = entry.getValue();
            String stringValue = String.valueOf(value);
            pairs.add(key + "=" + stringValue);
        }

        return String.join("&", pairs);
    }

    /**
     * 验证签名
     *
     * @param method 请求方法 "POST" / "GET"
     * @param url 你的回调 URL
     * @param params 回调的业务参数（body 或 query 里的参数，排除 header）
     * @param timestamp Header: SP-TIMESTAMP
     * @param sign Header: SP-SIGN
     * @param apiSecret 商户密钥
     * @return 验证结果
     */
    public static boolean verifySign(String method, String url, Map<String, ?> params,
                                     String timestamp, String sign, String apiSecret) {
        
        // 1. 拼接查询字符串
        String queryForSign = mapToSortedQueryString(params);
        
        // 2. 构造待签名字符串
        // 规则: method + url + "?" + queryForSign + timestamp
        String strToSign = method + url + "?" + queryForSign + timestamp;

        // 3. 计算期望签名
        String expectSign = signMessage(strToSign, apiSecret);
        
        System.out.println("calcSign: " + expectSign);
        System.out.println("getSign: " + sign);
        

        // 4. 比较签名
        // 使用 equals 而非 == 进行字符串内容比较
        return expectSign.equals(sign);
    }

    // 示例用法（可选）
    public static void main(String[] args) {
        // 示例数据，应与你的 PHP 示例保持一致的逻辑
        String apiSecret = "your_api_secret_key";
        String method = "POST";
        String url = "https://your.domain/callback/startpay";
        String timestamp = "1634870400"; // 示例时间戳

        Map<String, Object> params = new HashMap<>();
        params.put("orderId", "2021102200001");
        params.put("amount", 100.50);
        params.put("status", "SUCCESS");

        // 验证流程示例
        String queryForSign = mapToSortedQueryString(params);
        System.out.println("Sorted Query: " + queryForSign);
        // 输出: amount=100.5&orderId=2021102200001&status=SUCCESS

        String strToSign = method + url + "?" + queryForSign + timestamp;
        System.out.println("String To Sign: " + strToSign);
        // 输出: POSThttps://your.domain/callback/startpay?amount=100.5&orderId=2021102200001&status=SUCCESS1634870400

        String calculatedSign = signMessage(strToSign, apiSecret);
        System.out.println("Calculated Sign: " + calculatedSign);
        // 这里的输出是一个 Base64 字符串，取决于你的密钥和数据。

        // 假设收到的签名是 calculatedSign
        String receivedSign = calculatedSign; 

        boolean isValid = verifySign(method, url, params, timestamp, receivedSign, apiSecret);
        System.out.println("Signature Verification Result: " + isValid);
        // 输出: Signature Verification Result: true (如果密钥和数据正确)
    }
}