package startpay;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.relation.RoleUnresolved;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// 假设你的签名工具类和常量都在这里
// import com.yourpackage.SignatureUtils; 

@RestController
@RequestMapping("/iapStartpay/2025/")
public class OrderApi {

	// 替换为你的实际密钥
	private static final String API_SECRET = "/ranfszjFnvorq7mdnxV2PyInC5dkpnnbQnd6PSn84g=";

	private static final String CALLBACK_URL = "http://127.0.0.1:5757/iapStartpay/2025/callback.do";

	/**
	 * 处理 StartPay 的回调请求 * @param spSign Header: SP-SIGN
	 * 
	 * @param spTimestamp Header: SP-TIMESTAMP
	 * @param requestBody JSON Body 解析为 Map
	 * @return 签名验证结果
	 */
	@RequestMapping("/callback.do")
	public ResponseEntity<String> handleCallback(
			// 从 Request Header 中获取 SP-SIGN
			@RequestHeader("SP-SIGN") String spSign,

			// 从 Request Header 中获取 SP-TIMESTAM
			@RequestHeader("SP-TIMESTAM") String spTimestamp,

			// 使用 @RequestBody 将 JSON Body 自动解析为 Map<String, Object>
			@RequestBody Map<String, Object> requestBody) {

		String method = "POST";
		String url = CALLBACK_URL; // 使用配置的完整 URL

		// 2. 调用签名验证方法
		boolean isValid = SignatureUtils.verifySign(method, url, requestBody, spTimestamp, spSign, API_SECRET);

		// 3. 根据验证结果进行处理
		if (isValid) {
			// 签名验证成功：处理业务逻辑（如更新订单状态等）
			System.out.println("签名验证成功。处理订单: " + requestBody.get("orderId"));

			// 通常回调需要返回一个特定的成功状态码和/或消息
			return new ResponseEntity<>("SUCCESS", HttpStatus.OK);

		} else {
			// 签名验证失败：记录日志，返回错误状态码
			System.err.println("签名验证失败。收到的签名: " + spSign);
			System.err.println("待签名字符串参数: " + requestBody);

			// 返回 401 Unauthorized 或 400 Bad Request
			return new ResponseEntity<>("Signature verification failed", HttpStatus.UNAUTHORIZED);
		}
	}
}