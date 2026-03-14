 
package startpay;

import javax.net.ssl.HttpsURLConnection;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class CreateOrder {

    // 测试环境 key
    private static final String API_KEY = "509c1d67-6178-4579-93df-c9b90365df38";
    private static final String API_SECRET = "/ranfszjFnvorq7mdnxV2PyInC5dkpnnbQnd6PSn84g=";

    // 创建订单 API
    private static final String URL_CREATE =
            "https://w3api-test.startpay.ai/api/public/create_dc_order";


    public static void main(String[] args) throws Exception {

        Map<String, Object> params = new HashMap<>();

        params.put("mchOrderNo", "ORDER" + System.currentTimeMillis());
        params.put("digitalCurrency", "USDT");
        params.put("chain", "BSC");
        params.put("orderAmount", 100);
        params.put("subject", "游戏点卡");
        params.put("detail", "游戏点卡3个");
        params.put("notifyUrl", "https://go.com/xxx/callback");
        params.put("returnUrl", "https://java.com/");
        params.put("custId", "trump001");

        String resp = createOrder(params);

        System.out.println("Response:");
        System.out.println(resp);
    }


    public static String createOrder(Map<String, Object> params) throws Exception {

        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);

        // 1 生成签名字符串
        String queryForSign = SignatureUtils.mapToSortedQueryString(params);

        String strToSign =
                "POST" +
                URL_CREATE +
                "?" +
                queryForSign +
                timestamp;

        // 2 计算签名
        String sign = SignatureUtils.signMessage(strToSign, API_SECRET);

        // 3 JSON body
        String json = mapToJson(params);

        URL url = new URL(URL_CREATE);

        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        conn.setDoOutput(true);

        // headers
        conn.setRequestProperty("SP-API-KEY", API_KEY);
        conn.setRequestProperty("SP-SIGN", sign);
        conn.setRequestProperty("SP-TIMESTAMP", timestamp);
        conn.setRequestProperty("Content-Type", "application/json");

        // 发送 body
        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }

        // 读取返回
        InputStream is = conn.getInputStream();
        byte[] buf = is.readAllBytes();

        return new String(buf, StandardCharsets.UTF_8);
    }


    // 简单 map -> json
    private static String mapToJson(Map<String, Object> map) {

        StringBuilder sb = new StringBuilder();
        sb.append("{");

        boolean first = true;

        for (Map.Entry<String, Object> entry : map.entrySet()) {

            if (!first) {
                sb.append(",");
            }

            first = false;

            sb.append("\"")
                    .append(entry.getKey())
                    .append("\"")
                    .append(":");

            Object val = entry.getValue();

            if (val instanceof Number) {
                sb.append(val);
            } else {
                sb.append("\"")
                        .append(val.toString())
                        .append("\"");
            }
        }

        sb.append("}");

        return sb.toString();
    }
}