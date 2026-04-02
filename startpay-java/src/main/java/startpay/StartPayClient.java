package startpay;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * StartPay API 客户端
 *
 * <p>提供与 Go SDK 一致的签名和请求逻辑:
 * <ul>
 *   <li>签名算法: HMAC-SHA1 + Base64</li>
 *   <li>参数排序: 按 key 字母升序</li>
 *   <li>签名字符串: METHOD + URL + "?" + sortedParams + timestamp</li>
 *   <li>嵌套结构格式化: 模拟 Go fmt.Sprintf("%v") 输出格式</li>
 * </ul>
 *
 * <p>使用示例:
 * <pre>{@code
 * String apiKey = "your_api_key";
 * String apiSecret = "your_api_secret";
 *
 * StartPayClient client = new StartPayClient(apiKey, apiSecret);
 *
 * // GET 请求
 * Map<String, Object> params = new HashMap<>();
 * params.put("mchOrderNo", "ORDER123");
 * String resp = StartPayClient.spGet(
 *     "https://w3api-test.startpay.ai/api/public/query_dc_order",
 *     params, apiSecret, apiKey, 10);
 *
 * // POST 请求
 * Map<String, Object> postParams = new HashMap<>();
 * postParams.put("mchOrderNo", "ORDER456");
 * postParams.put("digitalCurrency", "USDT");
 * String resp = StartPayClient.spPost(
 *     "https://w3api-test.startpay.ai/api/public/create_dc_order",
 *     postParams, apiSecret, apiKey, 10);
 * }</pre>
 */
public class StartPayClient {

    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";

    /**
     * 计算 HMAC-SHA1 + Base64 签名
     *
     * @param message 待签名消息
     * @param secret  API 密钥
     * @return Base64 编码的签名字符串
     */
    public static String signMessage(String message, String secret) {
        try {
            SecretKeySpec signingKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA1_ALGORITHM);
            Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
            mac.init(signingKey);
            byte[] rawHmac = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return java.util.Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("Error signing message.", e);
        }
    }

    /**
     * 将参数按 key 升序拼接为 k=v&k2=v2 格式
     *
     * <p>嵌套结构使用 Go fmt.Sprintf("%v") 格式化规则:
     * <ul>
     *   <li>Map: map[key1:val1 key2:val2]</li>
     *   <li>List: [val1 val2 val3]</li>
     *   <li>基本类型: 直接 toString()</li>
     * </ul>
     *
     * @param params 参数 Map
     * @return 排序后的查询字符串
     */
    public static String mapToSortedQueryString(Map<String, ?> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        Map<String, ?> sortedParams = new TreeMap<>(params);
        List<String> pairs = new ArrayList<>();
        for (Map.Entry<String, ?> entry : sortedParams.entrySet()) {
            pairs.add(entry.getKey() + "=" + recursiveValueToString(entry.getValue()));
        }
        return String.join("&", pairs);
    }

    /**
     * 递归转换值，模拟 Go fmt.Sprintf("%v") 格式化行为
     *
     * <p>Go %v 格式化规则:
     * <ul>
     *   <li>nil: {@code <nil>}</li>
     *   <li>Map: {@code map[key1:val1 key2:val2]} (key 按字母序，空格分隔)</li>
     *   <li>Slice: {@code [val1 val2 val3]} (空格分隔)</li>
     *   <li>String: 直接输出，无引号</li>
     *   <li>Number/Boolean: 直接 toString()</li>
     * </ul>
     */
    private static String recursiveValueToString(Object value) {
        if (value == null) {
            return "<nil>";
        }
        if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) value;
            Map<String, Object> sortedMap = new TreeMap<>(map);
            List<String> pairs = new ArrayList<>();
            for (Map.Entry<String, Object> entry : sortedMap.entrySet()) {
                pairs.add(entry.getKey() + ":" + recursiveValueToString(entry.getValue()));
            }
            return "map[" + String.join(" ", pairs) + "]";
        }
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            List<String> items = new ArrayList<>();
            for (Object item : list) {
                items.add(recursiveValueToString(item));
            }
            return "[" + String.join(" ", items) + "]";
        }
        if (value instanceof Object[]) {
            Object[] array = (Object[]) value;
            List<String> items = new ArrayList<>();
            for (Object item : array) {
                items.add(recursiveValueToString(item));
            }
            return "[" + String.join(" ", items) + "]";
        }
        return String.valueOf(value);
    }

    /**
     * 发送 GET 请求
     *
     * <p>签名计算:
     * <pre>{@code
     * strToSign = "GET" + apiURL + "?" + mapToSortedQueryString(params) + timestamp
     * sign = signMessage(strToSign, apiSecret)
     * }</pre>
     *
     * @param apiURL     请求地址
     * @param params     查询参数
     * @param apiSecret  API 密钥
     * @param apiKey     API Key
     * @param timeoutSec 超时时间(秒)
     * @return 响应 Body
     */
    public static String spGet(String apiURL, Map<String, ?> params, String apiSecret, String apiKey, int timeoutSec) {
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);

        String queryForSign = mapToSortedQueryString(params);
        String strToSign = "GET" + apiURL + "?" + queryForSign + timestamp;
        String sign = signMessage(strToSign, apiSecret);

        String fullURL = apiURL;
        if (params != null && !params.isEmpty()) {
            try {
                Map<String, ?> sortedParams = new TreeMap<>(params);
                List<String> queryParts = new ArrayList<>();
                for (Map.Entry<String, ?> entry : sortedParams.entrySet()) {
                    queryParts.add(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8.name())
                            + "=" + URLEncoder.encode(entry.getValue().toString(), StandardCharsets.UTF_8.name()));
                }
                fullURL = apiURL + "?" + String.join("&", queryParts);
            } catch (Exception e) {
                throw new RuntimeException("URL encoding failed", e);
            }
        }

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(fullURL);
            httpGet.setHeader("SP-API-KEY", apiKey);
            httpGet.setHeader("SP-SIGN", sign);
            httpGet.setHeader("SP-TIMESTAMP", timestamp);

            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

                if (statusCode < 200 || statusCode >= 300) {
                    return "HTTP " + statusCode + ": " + body;
                }
                return body;
            }
        } catch (Exception e) {
            throw new RuntimeException("SpGet request failed: " + e.getMessage(), e);
        }
    }

    /**
     * 发送 POST JSON 请求
     *
     * <p>签名计算:
     * <pre>{@code
     * strToSign = "POST" + apiURL + "?" + mapToSortedQueryString(params) + timestamp
     * sign = signMessage(strToSign, apiSecret)
     * }</pre>
     *
     * @param apiURL     请求地址
     * @param params     请求参数(会作为 JSON Body 发送)
     * @param apiSecret  API 密钥
     * @param apiKey     API Key
     * @param timeoutSec 超时时间(秒)
     * @return 响应 Body
     */
    public static String spPost(String apiURL, Map<String, ?> params, String apiSecret, String apiKey, int timeoutSec) {
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);

        String queryForSign = mapToSortedQueryString(params);
        String strToSign = "POST" + apiURL + "?" + queryForSign + timestamp;
        String sign = signMessage(strToSign, apiSecret);

        String jsonBody = toJson(params);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(apiURL);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("SP-API-KEY", apiKey);
            httpPost.setHeader("SP-SIGN", sign);
            httpPost.setHeader("SP-TIMESTAMP", timestamp);
            httpPost.setEntity(new StringEntity(jsonBody, StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

                if (statusCode < 200 || statusCode >= 300) {
                    return "HTTP " + statusCode + ": " + body;
                }
                return body;
            }
        } catch (Exception e) {
            throw new RuntimeException("SpPost request failed: " + e.getMessage(), e);
        }
    }

    /**
     * 验证 StartPay 回调签名
     *
     * @param method     请求方法 "POST" / "GET"
     * @param apiURL     回调地址(需与发起请求时一致)
     * @param params     回调参数(body 或 query 解析后的 Map)
     * @param timestamp  SP-TIMESTAMP 请求头
     * @param sign       SP-SIGN 请求头
     * @param apiSecret  API 密钥
     * @return 验签是否通过
     */
    public static boolean verifySign(String method, String apiURL, Map<String, ?> params,
                                    String timestamp, String sign, String apiSecret) {
        String queryForSign = mapToSortedQueryString(params);
        String strToSign = method + apiURL + "?" + queryForSign + timestamp;
        String expectSign = signMessage(strToSign, apiSecret);
        return expectSign.equals(sign);
    }

    private static String toJson(Map<String, ?> params) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        Map<String, ?> sortedParams = new TreeMap<>(params);
        boolean first = true;
        for (Map.Entry<String, ?> entry : sortedParams.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(escapeJson(entry.getKey())).append("\":");
            appendJsonValue(sb, entry.getValue());
        }
        sb.append("}");
        return sb.toString();
    }

    private static void appendJsonValue(StringBuilder sb, Object value) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof String) {
            sb.append("\"").append(escapeJson((String) value)).append("\"");
        } else if (value instanceof Number) {
            sb.append(value);
        } else if (value instanceof Boolean) {
            sb.append(value);
        } else if (value instanceof List) {
            sb.append("[");
            boolean first = true;
            for (Object item : (List<?>) value) {
                if (!first) sb.append(",");
                first = false;
                appendJsonValue(sb, item);
            }
            sb.append("]");
        } else if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) value;
            sb.append("{");
            Map<String, Object> sortedMap = new TreeMap<>(map);
            boolean first = true;
            for (Map.Entry<String, Object> entry : sortedMap.entrySet()) {
                if (!first) sb.append(",");
                first = false;
                sb.append("\"").append(escapeJson(entry.getKey())).append("\":");
                appendJsonValue(sb, entry.getValue());
            }
            sb.append("}");
        } else {
            sb.append("\"").append(escapeJson(value.toString())).append("\"");
        }
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}