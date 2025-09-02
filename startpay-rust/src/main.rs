use serde_json::json;
use startpay_rust::{sp_get, sp_post_json, verify_sign};

fn main() {
    let api_secret = "xH5sMh07w2pBJoXE3CSUp0QO63tIZaUID6z1EoTkP08=";
    let api_key = "ead4b4a0-162a-4f4b-9846-da29f5696af1";

    // GET 示例
    let params1 = json!({
        "mchOrderNo": "ORDER1872983743393"
    }).as_object().unwrap().clone();

    match sp_get("https://test.bakbak.cn/api/public/query_dc_order", params1, api_secret, api_key, 10) {
        Ok(resp) => println!("GET 响应: {}", resp),
        Err(err) => eprintln!("GET 错误: {}", err),
    }

    // POST 示例
    let params2 = json!({
        "mchOrderNo": "ORDER1239234230",
        "digitalCurrency": "USDT",
        "chain": "BSC",
        "desc": "USDT",
        "orderAmount": 100,
        "subject": "游戏点卡",
        "detail": "游戏点卡3个",
        "notifyUrl": "https://go.com/xxx/callback"
    }).as_object().unwrap().clone();

    match sp_post_json("https://test.bakbak.cn/api/public/create_dc_order", params2, api_secret, api_key, 10) {
        Ok(resp) => println!("POST 响应: {}", resp),
        Err(err) => eprintln!("POST 错误: {}", err),
    }

    // 验签示例
    let params3 = json!({
        "mchOrderNo": "ORDER1239234230",
        "status": "SUCCESS"
    }).as_object().unwrap().clone();

    let ok = verify_sign(
        "POST",
        "https://yourdomain.com/callback",
        params3,
        "1725250000",     // 假设 header 里的 timestamp
        "签名字符串",     // 假设 header 里的 SP-SIGN
        api_secret,
    );

    println!("验签结果: {}", ok);
}
