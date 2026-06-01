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
import time
from typing import Dict, Any

import requests


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
    将Map按key排序生成 query string，支持嵌套结构
    完全对应 Java/Go 版本逻辑
    """
    if not params:
        return ""

    sorted_params = dict(sorted(params.items()))
    pairs = []

    for key, value in sorted_params.items():
        pairs.append(f"{key}={recursive_value_to_string(value)}")

    return "&".join(pairs)


def recursive_value_to_string(value: Any) -> str:
    """
    递归转字符串，完全模拟 Go fmt.Sprintf("%v") 格式
    支持：null、map、list、array、普通类型
    """
    if value is None:
        return "<nil>"

    # 字典 = Go map
    if isinstance(value, dict):
        sorted_map = dict(sorted(value.items()))
        items = []
        for k, v in sorted_map.items():
            items.append(f"{k}:{recursive_value_to_string(v)}")
        return f"map[{' '.join(items)}]"

    # 列表 = Go slice
    if isinstance(value, list):
        items = [recursive_value_to_string(item) for item in value]
        return f"[{' '.join(items)}]"

    # 数组 = Go array
    if isinstance(value, (tuple, set)):
        items = [recursive_value_to_string(item) for item in value]
        return f"[{' '.join(items)}]"

    # 普通类型
    return str(value)


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

    # 请求头
    headers = {
        "SP-API-KEY": api_key,
        "SP-SIGN": sign,
        "SP-TIMESTAMP": timestamp
    }

    try:
        response = requests.get(
            url=api_url,
            params=params,
            headers=headers,
            timeout=timeout_sec
        )

        if response.status_code < 200 or response.status_code >= 300:
            error_msg = response.text
            raise Exception(f"HTTP {response.status_code}: {error_msg}")

        return response.text

    except requests.exceptions.RequestException as e:
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

    headers = {
        "SP-API-KEY": api_key,
        "SP-SIGN": sign,
        "SP-TIMESTAMP": timestamp
    }

    try:
        response = requests.post(
            url=api_url,
            json=params,
            headers=headers,
            timeout=timeout_sec
        )

        if not response.ok:
            raise Exception(f"HTTP {response.status_code}: {response.text}")

        return response.text

    except requests.exceptions.RequestException as e:
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
