package main

import "startpay-go/startpay" 
import "fmt"

func main() {
	// 测试环境 key/secret
	apiSecret := "DRhgZp59xWve7rrB67lD3j84MnyzcWoIzEXqI3Rolgw="
	apiKey := "6e867b71-37d4-41d1-bbce-a5972d9c718f"

	// GET 示例
	url1 := "https://test.bakbak.cn/api/public/query_dc_order"
	params1 := map[string]interface{}{
		"mchOrderNo": "ORDER1872983743393",
	}
	resp1, err := startpay.SpGet(url1, params1, apiSecret, apiKey, 10)
	if err != nil {
		fmt.Println("GET error:", err)
	} else {
		fmt.Println("GET 响应:", resp1)
	}

	// POST 示例
	url2 := "https://test.bakbak.cn/api/public/create_dc_order"
	params2 := map[string]interface{}{
		"mchOrderNo":     "ORDER1239234230",
		"digitalCurrency": "USDT",
		"chain":          "BSC",
		"orderAmount":    100,
		"subject":        "游戏点卡",
		"detail":         "游戏点卡3个",
		"notifyUrl":      "https://go.com/xxx/callback",
	}
	resp2, err := startpay.SpPostJson(url2, params2, apiSecret, apiKey, 10)
	if err != nil {
		fmt.Println("POST error:", err)
	} else {
		fmt.Println("POST 响应:", resp2)
	}
	
	//回调  
	// 请查看  VerifySign  方法
	
	
}
