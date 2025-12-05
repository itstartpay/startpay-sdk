#!/usr/bin/env python3
"""
StartPay Python SDK 单元测试

测试核心功能：签名生成和验证
"""

import sys
import os
import time

# 添加当前目录到Python路径，以便导入startpay模块
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from startpay import sign_message, map_to_sorted_query_string, verify_sign


def test_sign_message():
    """测试签名生成"""
    print("测试签名生成...")
    
    message = "GEThttps://example.com/api/test?param1=value1&param2=value2" + str(int(time.time()))
    secret = "test_secret"
    
    signature = sign_message(message, secret)
    print(f"消息: {message}")
    print(f"签名: {signature}")
    
    # 验证签名一致性
    signature2 = sign_message(message, secret)
    assert signature == signature2, "相同输入应产生相同签名"
    print("✓ 签名生成测试通过")


def test_map_to_sorted_query_string():
    """测试参数排序和拼接"""
    print("\n测试参数排序和拼接...")
    
    params = {
        "z": "last",
        "a": "first",
        "m": "middle"
    }
    
    result = map_to_sorted_query_string(params)
    expected = "a=first&m=middle&z=last"
    
    assert result == expected, f"期望: {expected}, 实际: {result}"
    print(f"参数: {params}")
    print(f"结果: {result}")
    print("✓ 参数排序和拼接测试通过")


def test_verify_sign():
    """测试签名验证"""
    print("\n测试签名验证...")
    
    method = "POST"
    url = "https://example.com/callback"
    params = {
        "orderId": "12345",
        "amount": 100.50,
        "status": "SUCCESS"
    }
    timestamp = str(int(time.time()))
    secret = "test_secret"
    
    # 生成签名
    query_for_sign = map_to_sorted_query_string(params)
    str_to_sign = f"{method}{url}?{query_for_sign}{timestamp}"
    signature = sign_message(str_to_sign, secret)
    
    # 验证签名
    is_valid = verify_sign(method, url, params, timestamp, signature, secret)
    assert is_valid, "正确签名应验证通过"
    print(f"待签名字符串: {str_to_sign}")
    print(f"签名: {signature}")
    print("✓ 签名验证测试通过")
    
    # 测试错误签名
    is_invalid = verify_sign(method, url, params, timestamp, "invalid_signature", secret)
    assert not is_invalid, "错误签名应验证失败"
    print("✓ 错误签名验证测试通过")


def main():
    """运行所有测试"""
    print("StartPay Python SDK 单元测试")
    print("=" * 40)
    
    try:
        test_sign_message()
        test_map_to_sorted_query_string()
        test_verify_sign()
        
        print("\n" + "=" * 40)
        print("所有测试通过! ✓")
    except AssertionError as e:
        print(f"\n测试失败: {str(e)}")
        sys.exit(1)
    except Exception as e:
        print(f"\n测试出错: {str(e)}")
        sys.exit(1)


if __name__ == "__main__":
    main()