use hmac::{Hmac, Mac};
use sha1::Sha1;
use base64::{engine::general_purpose, Engine as _};
use reqwest::blocking::Client;
use serde_json::Value;
use std::collections::BTreeMap;
use std::time::{SystemTime, UNIX_EPOCH};

type HmacSha1 = Hmac<Sha1>;

/// 计算 HMAC-SHA1 + Base64
fn sign_message(message: &str, secret: &str) -> String {
    let mut mac = HmacSha1::new_from_slice(secret.as_bytes()).unwrap();
    mac.update(message.as_bytes());
    let result = mac.finalize().into_bytes();
    general_purpose::STANDARD.encode(result)
}

/// 将参数升序拼接成 k=v&k2=v2
fn map_to_sorted_query_string(params: &serde_json::Map<String, Value>) -> String {
    let mut bmap = BTreeMap::new();
    for (k, v) in params {
        bmap.insert(k.clone(), v.to_string().trim_matches('"').to_string());
    }
    bmap.into_iter()
        .map(|(k, v)| format!("{}={}", k, v))
        .collect::<Vec<_>>()
        .join("&")
}

/// GET 请求
pub fn sp_get(
    url: &str,
    params: serde_json::Map<String, Value>,
    api_secret: &str,
    api_key: &str,
    timeout: u64,
) -> Result<String, reqwest::Error> {
    let timestamp = SystemTime::now()
        .duration_since(UNIX_EPOCH).unwrap()
        .as_secs().to_string();

    let query_for_sign = map_to_sorted_query_string(&params);
    let str_to_sign = format!("GET{}?{}{}", url, query_for_sign, timestamp);
    let sign = sign_message(&str_to_sign, api_secret);

    let client = Client::builder()
        .timeout(std::time::Duration::from_secs(timeout))
        .build()?;

    let resp = client
        .get(url)
        .query(&params) // 自动 urlencode
        .header("SP-API-KEY", api_key)
        .header("SP-SIGN", sign)
        .header("SP-TIMESTAMP", timestamp)
        .send()?
        .text()?;

    Ok(resp)
}

/// POST JSON 请求
pub fn sp_post_json(
    url: &str,
    params: serde_json::Map<String, Value>,
    api_secret: &str,
    api_key: &str,
    timeout: u64,
) -> Result<String, reqwest::Error> {
    let timestamp = SystemTime::now()
        .duration_since(UNIX_EPOCH).unwrap()
        .as_secs().to_string();

    let query_for_sign = map_to_sorted_query_string(&params);
    let str_to_sign = format!("POST{}?{}{}", url, query_for_sign, timestamp);
    let sign = sign_message(&str_to_sign, api_secret);

    let client = Client::builder()
        .timeout(std::time::Duration::from_secs(timeout))
        .build()?;

    let resp = client
        .post(url)
        .json(&params)
        .header("SP-API-KEY", api_key)
        .header("SP-SIGN", sign)
        .header("SP-TIMESTAMP", timestamp)
        .header("Content-Type", "application/json")
        .send()?
        .text()?;

    Ok(resp)
}

/// 验签（回调用）
pub fn verify_sign(
    method: &str,
    url: &str,
    params: serde_json::Map<String, Value>,
    timestamp: &str,
    sign: &str,
    api_secret: &str,
) -> bool {
    let query_for_sign = map_to_sorted_query_string(&params);
    let str_to_sign = format!("{}{}?{}{}", method, url, query_for_sign, timestamp);
    let expect_sign = sign_message(&str_to_sign, api_secret);
    expect_sign == sign
}
