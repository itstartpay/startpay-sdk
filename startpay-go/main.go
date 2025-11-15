package main

import (
	"encoding/json"
	"fmt"
	"startpay-go/startpay"
)

func main() {
	// 测试环境 key/secret
	apiSecret := "DRhgZp59xWve7rrB67lD3j84MnyzcWoIzEXqI3Rolgw="
	apiKey := "6e867b71-37d4-41d1-bbce-a5972d9c718f"

	// GET 示例
	url1 := "https://w3api-test.startpay.ai/api/public/query_dc_order"
	params1 := map[string]interface{}{
		"mchOrderNo": "ORDER18729837433193",
	}
	resp1, err := startpay.SpGet(url1, params1, apiSecret, apiKey, 10)
	if err != nil {
		fmt.Println("GET error:", err)
	} else {
		fmt.Println("GET 响应:", resp1)
	}

	// POST 示例
	url2 := "https://w3api-test.startpay.ai/api/public/create_dc_order"
	params2 := map[string]interface{}{
		"mchOrderNo":      "ORDE13314230",
		"digitalCurrency": "USDT",
		"chain":           "TRX",
		"orderAmount":     100,
		"subject":         "游戏点卡",
		"detail":          "游戏点卡3个",
		"notifyUrl":       "https://go.com/xxx/callback",
	}
	resp2, err := startpay.SpPostJson(url2, params2, apiSecret, apiKey, 10)
	if err != nil {
		fmt.Println("POST error:", err)
	} else {
		fmt.Println("POST 响应:", resp2)
	}

	// 尝试解析 resp2（JSON 字符串）并打印 payUrl 字段
	var parsed map[string]interface{}
	if err := json.Unmarshal([]byte(resp2), &parsed); err != nil {
		fmt.Println("无法解析 POST 响应为 JSON:", err)
	} else {
		// 先查找顶层 payUrl
		if v, ok := parsed["payUrl"]; ok {
			fmt.Println("payUrl:", v)
		} else if data, ok := parsed["data"].(map[string]interface{}); ok {
			if pu, ok := data["payUrl"]; ok {
				fmt.Println("payUrl:", pu)
			} else {
				fmt.Println("响应中未找到 payUrl 字段")
			}
		} else {
			fmt.Println("响应中未找到 payUrl 字段")
		}
	}

	//回调   响应http状态码为200代表成功,可能因为网络原因多次回调,请做好幂等.
	//请查看  VerifySign  方法

}
