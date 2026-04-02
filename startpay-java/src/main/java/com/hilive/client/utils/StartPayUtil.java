package com.hilive.client.utils;

import startpay.StartPayClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StartPayUtil {

    private static final String apiKey = "key";
    private static final String apiSecret = "secret";
    private static final String URL_CREATE = "https://w3api-test.startpay.ai/api/public/create_dc_order";

    public static void createBatchTransfer(){
        String url = "https://w3api-test.startpay.ai/api/public/create_batch_transfer_api";

        List<Map<String, String>> list = new ArrayList<>();
        Map<String, String> item = new HashMap<>();
        item.put("merchantOrderId", "ORDER_" + System.currentTimeMillis());
        item.put("amount", "100.00");
        item.put("currency", "USDT");
        item.put("chain", "TRC20");
        item.put("toAddress", "TPaenT29Jssp9EmqAXada3tExzTXzNNqRT");
        item.put("source", "withdraw");
        list.add(item);

        Map<String, Object> body = new HashMap<>();
        body.put("merchantBatchId", "BATCH_" + System.currentTimeMillis());
        body.put("taskName", "批量提现吗");
        body.put("remarks", "测试批量");
        body.put("transferOrderList", list);
        String resp = StartPayClient.spPost(
                url,
                body,
                apiSecret,
                apiKey,
                30
        );
        System.out.println(resp);
    }

    public static void createDcOrder(){
        Map<String, Object> params = new HashMap<>();
        params.put("mchOrderNo", "ORDER123456");
        params.put("digitalCurrency", "USDT");
        params.put("chain", "TRX");
        params.put("orderAmount", 100);
        params.put("subject", "商品名称");
        params.put("notifyUrl", "https://www.baidu.com/notify");

        String resp = StartPayClient.spPost(
                URL_CREATE,
                params,
                apiSecret,
                apiKey,
                30
        );
        System.out.println(resp);
    }

    public static void main(String[] args) {
        createDcOrder();
        createBatchTransfer();
    }
}