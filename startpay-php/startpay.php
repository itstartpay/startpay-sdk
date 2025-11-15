<?php

/**
 * 计算签名（HMAC-SHA1 + Base64）
 */
function signMessage(string $message, string $secret): string {
    return base64_encode(hash_hmac('sha1', $message, $secret, true));
}

/**
 * 将参数按 key 升序拼接为 k=v&k2=v2（不做 urlencode；用于签名）
 */
function mapToSortedQueryString(array $data): string {
    if (empty($data)) return "";
    ksort($data);
    $pairs = [];
    foreach ($data as $k => $v) {
        $pairs[] = $k . "=" . (string)$v;  
    }
    return implode("&", $pairs);
}


/**
 * 验证签名
 *
 * @param string $method    请求方法 "POST" / "GET"
 * @param string $url       你的回调 URL（必须和配置在 StartPay 后台的一致）
 * @param array  $params    回调的业务参数（body 或 query 里的参数，排除 header）
 * @param string $timestamp Header: SP-TIMESTAMP
 * @param string $sign      Header: SP-SIGN
 * @param string $apiSecret 商户密钥
 * @return bool
 */
function verifySign(string $method, string $url, array $params, string $timestamp, string $sign, string $apiSecret): bool {
    $queryForSign = mapToSortedQueryString($params);
    $strToSign    = $method . $url . "?" . $queryForSign . $timestamp;
    $expectSign   = signMessage($strToSign, $apiSecret);
    return $expectSign === $sign;
}

/**
 * 封装的 GET 请求
 */
function spGet(string $url, array $params, string $apiSecret, string $apiKey, int $timeout = 10): string {
    $timestamp    = (string) time();

    // 1) 参与签名的 query（不做 urlencode）
    $queryForSign = mapToSortedQueryString($params);
    $strToSign    = "GET" . $url . "?" . $queryForSign . $timestamp;

    // 2) 计算签名
    $sign = signMessage($strToSign, $apiSecret);

    // 3) 请求头
    $headers = [
        "SP-API-KEY: $apiKey",
        "SP-SIGN: $sign",
        "SP-TIMESTAMP: $timestamp",
    ];

    // 4) 实际请求 URL（带 urlencode）
    $fullUrl = $url;
    if (!empty($params)) {
        $fullUrl .= "?" . http_build_query($params);
    }

    // 5) 发送请求
    $ch = curl_init($fullUrl);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
    curl_setopt($ch, CURLOPT_TIMEOUT, $timeout);

    $resp = curl_exec($ch);
    if ($resp === false) {
        $err = curl_error($ch);
        curl_close($ch);
        throw new Exception("cURL error: $err");
    }

    $status = (int) curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);

    if ($status < 200 || $status >= 300) {
        throw new Exception("HTTP $status: $resp");
    }

    return $resp;
}

/**
 * 封装的 POST JSON 请求
 */
function spPostJson(string $url, array $params, string $apiSecret, string $apiKey, int $timeout = 10): string {
    $timestamp    = (string) time();

    // 1) 参与签名的 query（就是请求体参数）
    $queryForSign = mapToSortedQueryString($params);
    $strToSign    = "POST" . $url . "?" . $queryForSign . $timestamp;
	

    // 2) 计算签名
    $sign = signMessage($strToSign, $apiSecret);

    // 3) 请求头
    $headers = [
        "SP-API-KEY: $apiKey",
        "SP-SIGN: $sign",
        "SP-TIMESTAMP: $timestamp",
        "Content-Type: application/json"
    ];

    // 4) JSON body
    $jsonData = json_encode($params, JSON_UNESCAPED_UNICODE);

    // 5) 发送请求
    $ch = curl_init($url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_POSTFIELDS, $jsonData);
    curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
    curl_setopt($ch, CURLOPT_TIMEOUT, $timeout);

    $resp = curl_exec($ch);
    if ($resp === false) {
        $err = curl_error($ch);
        curl_close($ch);
        throw new Exception("cURL error: $err");
    }

    $status = (int) curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);

    if ($status < 200 || $status >= 300) {
        throw new Exception("HTTP $status: $resp");
    }

    return $resp;
}





//测试环境  示例  key和sec  正式环境替换为 正式环境的key和sec  !!!
$apiSecret = "xH5sMh07w2pBJoXE3CSUp0QO63tIZaUID6z1EoTkP08=";
$apiKey    = "ead4b4a0-162a-4f4b-9846-da29f5696af1";



//w3api-test.startpay.ai 为测试域名, 正式环境替换为  w3api.startpay.ai !!!


 //GET 示例  API收单/查询订单
$url = "https://w3api-test.startpay.ai/api/public/query_dc_order";
$params = ["mchOrderNo" => "ORDER1872983743393"];
 $resp = spGet($url, $params, $apiSecret, $apiKey);
 echo "GET 响应: $resp\n";

 //POST 示例  API收单/创建订单
 $url = "https://w3api-test.startpay.ai/api/public/create_dc_order";
 $params = [
	"mchOrderNo" => "ORDER1239234230",
	"digitalCurrency" => "USDT" ,
	"chain" => "BSC" , 
	"orderAmount" => 100,  
	"subject" => "游戏点卡" ,
	"detail" => "游戏点卡3个" ,
	"notifyUrl" => "https://go.com/xxx/callback"
];
 $resp = spPostJson($url, $params, $apiSecret, $apiKey);
 echo "POST 响应: $resp\n";

 
 
//回调   响应http状态码为200代表成功,可能因为网络原因多次回调,请做好幂等. 
//请查看  VerifySign  方法



 