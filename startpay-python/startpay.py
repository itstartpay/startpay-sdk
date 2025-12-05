"""
StartPay Python SDK

提供StartPay API的Python接口，包括：
- HMAC-SHA1签名生成和验证
- GET/POST请求封装
- 回调签名验证
"""

import base64
import hashlib
import hmac
import json
import time
from typing import Dict, Any
import urllib.parse
import urllib.request


def sign_message(message: str, secret: str) -> str:
    """
    计算HMAC-SHA1签名并进行Base64编码
    
    Args:
        message: 待签名的消息
        secret: 密钥
        
    Returns:
        Base64编码的签名
    """
    signature = hmac.new(
        secret.encode('utf-8'),
        message.encode('utf-8'),
        hashlib.sha1
    ).digest()
    return base64.b64encode(signature).decode('utf-8')


def map_to_sorted_query_string(params: Dict[str, Any]) -> str:
    """
    将参数按key升序拼接为k=v&k2=v2格式（用于签名）
    
    Args:
        params: 参数字典
        
    Returns:
        排序后的查询字符串
    """
    if not params:
        return ""
    
    # 按key排序
    sorted_keys = sorted(params.keys())
    
    # 拼接键值对
    pairs = []
    for key in sorted_keys:
        value = str(params[key])
        pairs.append(f"{key}={value}")
    
    return "&".join(pairs)


def sp_get(
    api_url: str, 
    params: Dict[str, Any], 
    api_secret: str, 
    api_key: str, 
    timeout_sec: int = 10
) -> str:
    """
    发送GET请求
    
    Args:
        api_url: API地址
        params: 请求参数
        api_secret: API密钥
        api_key: API Key
        timeout_sec: 超时时间（秒）
        
    Returns:
        响应内容
        
    Raises:
        Exception: 请求失败时抛出异常
    """
    timestamp = str(int(time.time()))
    
    # 生成签名
    query_for_sign = map_to_sorted_query_string(params)
    str_to_sign = f"GET{api_url}?{query_for_sign}{timestamp}"
    sign = sign_message(str_to_sign, api_secret)
    
    # 构建完整URL（带URL编码）
    full_url = api_url
    if params:
        query_string = urllib.parse.urlencode(params)
        full_url += f"?{query_string}"
    
    # 创建请求
    req = urllib.request.Request(full_url)
    req.add_header("SP-API-KEY", api_key)
    req.add_header("SP-SIGN", sign)
    req.add_header("SP-TIMESTAMP", timestamp)
    
    try:
        with urllib.request.urlopen(req, timeout=timeout_sec) as response:
            if response.status < 200 or response.status >= 300:
                error_msg = response.read().decode('utf-8')
                raise Exception(f"HTTP {response.status}: {error_msg}")
            return response.read().decode('utf-8')
    except urllib.error.URLError as e:
        raise Exception(f"Request failed: {str(e)}")


def sp_post_json(
    api_url: str, 
    params: Dict[str, Any], 
    api_secret: str, 
    api_key: str, 
    timeout_sec: int = 10
) -> str:
    """
    发送POST JSON请求
    
    Args:
        api_url: API地址
        params: 请求参数
        api_secret: API密钥
        api_key: API Key
        timeout_sec: 超时时间（秒）
        
    Returns:
        响应内容
        
    Raises:
        Exception: 请求失败时抛出异常
    """
    timestamp = str(int(time.time()))
    
    # 生成签名
    query_for_sign = map_to_sorted_query_string(params)
    str_to_sign = f"POST{api_url}?{query_for_sign}{timestamp}"
    sign = sign_message(str_to_sign, api_secret)
    
    # 构建请求数据
    json_data = json.dumps(params, ensure_ascii=False).encode('utf-8')
    
    # 创建请求
    req = urllib.request.Request(api_url, data=json_data, method='POST')
    req.add_header("SP-API-KEY", api_key)
    req.add_header("SP-SIGN", sign)
    req.add_header("SP-TIMESTAMP", timestamp)
    req.add_header("Content-Type", "application/json")
    
    try:
        with urllib.request.urlopen(req, timeout=timeout_sec) as response:
            if response.status < 200 or response.status >= 300:
                error_msg = response.read().decode('utf-8')
                raise Exception(f"HTTP {response.status}: {error_msg}")
            return response.read().decode('utf-8')
    except urllib.error.URLError as e:
        raise Exception(f"Request failed: {str(e)}")


def verify_sign(
    method: str,
    api_url: str,
    params: Dict[str, Any],
    timestamp: str,
    sign: str,
    api_secret: str
) -> bool:
    """
    验证StartPay回调签名
    
    Args:
        method: 请求方法 "POST" 或 "GET"
        api_url: 回调URL
        params: 回调参数
        timestamp: 请求头中的SP-TIMESTAMP
        sign: 请求头中的SP-SIGN
        api_secret: API密钥
        
    Returns:
        True表示验签成功，False表示验签失败
    """
    query_for_sign = map_to_sorted_query_string(params)
    str_to_sign = f"{method}{api_url}?{query_for_sign}{timestamp}"
    expect_sign = sign_message(str_to_sign, api_secret)
    return expect_sign == sign