package startpay

import (
	"bytes"
	"crypto/hmac"
	"crypto/sha1"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net/http"
	"net/url"
	"sort"
	"strings"
	"time"
)

// HMAC-SHA1 + Base64 签名
func signMessage(message, secret string) string {
	mac := hmac.New(sha1.New, []byte(secret))
	mac.Write([]byte(message))
	return base64.StdEncoding.EncodeToString(mac.Sum(nil))
}

// 将参数排序为 key=val&key2=val2
func mapToSortedQueryString(params map[string]interface{}) string {
	if len(params) == 0 {
		return ""
	}
	keys := make([]string, 0, len(params))
	for k := range params {
		keys = append(keys, k)
	}
	sort.Strings(keys)

	pairs := make([]string, 0, len(keys))
	for _, k := range keys {
		pairs = append(pairs, fmt.Sprintf("%s=%v", k, params[k]))
	}
	return strings.Join(pairs, "&")
}

// GET 请求
func SpGet(apiURL string, params map[string]interface{}, apiSecret, apiKey string, timeoutSec int) (string, error) {
	timestamp := fmt.Sprintf("%d", time.Now().Unix())

	queryForSign := mapToSortedQueryString(params)
	strToSign := "GET" + apiURL + "?" + queryForSign + timestamp
	sign := signMessage(strToSign, apiSecret)

	// 拼接 URL（带 urlencode）
	fullURL := apiURL
	if len(params) > 0 {
		q := url.Values{}
		for k, v := range params {
			q.Set(k, fmt.Sprintf("%v", v))
		}
		fullURL += "?" + q.Encode()
	}

	client := &http.Client{Timeout: time.Duration(timeoutSec) * time.Second}
	req, err := http.NewRequest("GET", fullURL, nil)
	if err != nil {
		return "", err
	}

	req.Header.Set("SP-API-KEY", apiKey)
	req.Header.Set("SP-SIGN", sign)
	req.Header.Set("SP-TIMESTAMP", timestamp)

	resp, err := client.Do(req)
	if err != nil {
		return "", err
	}
	defer resp.Body.Close()

	body, _ := ioutil.ReadAll(resp.Body)
	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		return "", fmt.Errorf("HTTP %d: %s", resp.StatusCode, string(body))
	}
	return string(body), nil
}

// POST JSON 请求
func SpPostJson(apiURL string, params map[string]interface{}, apiSecret, apiKey string, timeoutSec int) (string, error) {
	timestamp := fmt.Sprintf("%d", time.Now().Unix())

	queryForSign := mapToSortedQueryString(params)
	strToSign := "POST" + apiURL + "?" + queryForSign + timestamp
	sign := signMessage(strToSign, apiSecret)

	jsonData, _ := json.Marshal(params)

	client := &http.Client{Timeout: time.Duration(timeoutSec) * time.Second}
	req, err := http.NewRequest("POST", apiURL, bytes.NewBuffer(jsonData))
	if err != nil {
		return "", err
	}

	req.Header.Set("SP-API-KEY", apiKey)
	req.Header.Set("SP-SIGN", sign)
	req.Header.Set("SP-TIMESTAMP", timestamp)
	req.Header.Set("Content-Type", "application/json")

	resp, err := client.Do(req)
	if err != nil {
		return "", err
	}
	defer resp.Body.Close()

	body, _ := ioutil.ReadAll(resp.Body)
	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		return "", fmt.Errorf("HTTP %d: %s", resp.StatusCode, string(body))
	}
	return string(body), nil
}




// VerifySign 用于验证 StartPay 回调签名
// method: "POST" / "GET"
// apiURL: 你的回调URL，例如 "https://yourdomain.com/notify"
// params: 回调传过来的参数（body解析成 map）
// timestamp: 从请求头 SP-TIMESTAMP 里拿到
// sign: 从请求头 SP-SIGN 里拿到
// 返回 true 表示验签成功
func VerifySign(method, apiURL string, params map[string]interface{}, timestamp, sign, apiSecret string) bool {
	queryForSign := mapToSortedQueryString(params)
	strToSign := method + apiURL + "?" + queryForSign + timestamp
	expectSign := signMessage(strToSign, apiSecret)
	return expectSign == sign
}

