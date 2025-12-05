#!/usr/bin/env python3
"""
StartPay Python SDK 使用示例

此示例展示了如何使用startpay.py模块进行API调用和签名验证。
"""

import sys
import os
import json
import time

# 添加当前目录到Python路径，以便导入startpay模块
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from startpay import sp_get, sp_post_json, verify_sign


def main():
    # 测试环境配置
    # 注意：这里使用的是测试环境的密钥，正式环境请替换为正式环境的密钥
    api_secret = "xH5sMh07w2pBJoXE3CSUp0QO63tIZaUID6z1EoTkP08="
    api_key = "ead4b4a0-162a-4f4b-9846-da29f5696af1"
    
    # 注意：test.bakbak.cn 为测试域名，正式环境请替换为 w3api.startpay.ai
    base_url = "https://test.bakbak.cn"
    
    print("StartPay Python SDK 示例")
    print("=" * 40)
    
    # GET 示例：查询订单
    print("\n1. GET 示例 - 查询订单")
    query_url = f"{base_url}/api/public/query_dc_order"
    query_params = {
        "mchOrderNo": "ORDER1872983743393"
    }
    
    try:
        response = sp_get(query_url, query_params, api_secret, api_key)
        print("GET 响应:", response)
    except Exception as e:
        print("GET 请求失败:", str(e))
    
    # POST 示例：创建订单
    print("\n2. POST 示例 - 创建订单")
    create_url = f"{base_url}/api/public/create_dc_order"
    create_params = {
        "mchOrderNo": "ORDER1239234230",
        "digitalCurrency": "USDT",
        "chain": "BSC",
        "orderAmount": 100,
        "subject": "游戏点卡",
        "detail": "游戏点卡3个",
        "notifyUrl": "https://example.com/callback"
    }
    
    try:
        response = sp_post_json(create_url, create_params, api_secret, api_key)
        print("POST 响应:", response)
    except Exception as e:
        print("POST 请求失败:", str(e))
    
    # 签名验证示例
    print("\n3. 签名验证示例")
    callback_url = "https://example.com/callback/startpay"
    callback_method = "POST"
    callback_params = {
        "orderId": "2021102200001",
        "amount": 100.50,
        "status": "SUCCESS"
    }
    callback_timestamp = str(int(time.time()))
    
    # 模拟生成签名（实际使用中，这个签名会从请求头中获取）
    from startpay import sign_message, map_to_sorted_query_string
    query_for_sign = map_to_sorted_query_string(callback_params)
    str_to_sign = f"{callback_method}{callback_url}?{query_for_sign}{callback_timestamp}"
    callback_sign = sign_message(str_to_sign, api_secret)
    
    # 验证签名
    is_valid = verify_sign(
        callback_method,
        callback_url,
        callback_params,
        callback_timestamp,
        callback_sign,
        api_secret
    )
    
    print(f"签名验证结果: {'成功' if is_valid else '失败'}")
    print(f"待签名字符串: {str_to_sign}")
    print(f"签名: {callback_sign}")
    
    print("\n" + "=" * 40)
    print("示例完成")


if __name__ == "__main__":
    main()