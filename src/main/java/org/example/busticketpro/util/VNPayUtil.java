package org.example.busticketpro.util;

import org.example.busticketpro.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
@Slf4j
public class VNPayUtil {

    @Value("${app.vnpay.url}")
    private String vnpayUrl;

    @Value("${app.vnpay.tmn-code}")
    private String tmnCode;

    @Value("${app.vnpay.hash-secret}")
    private String hashSecret;

    @Value("${app.vnpay.return-url}")
    private String returnUrl;

    public String createPaymentUrl(String ticketCode, long amount, String orderInfo, String ipAddr) {
        validateConfig();

        TimeZone vietnamTimeZone = TimeZone.getTimeZone("Asia/Ho_Chi_Minh");
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        formatter.setTimeZone(vietnamTimeZone);
        Calendar createDate = Calendar.getInstance(vietnamTimeZone);

        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", tmnCode);
        params.put("vnp_Amount", String.valueOf(amount * 100));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", ticketCode);
        params.put("vnp_OrderInfo", orderInfo);
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", returnUrl);
        params.put("vnp_IpAddr", normalizeIp(ipAddr));
        params.put("vnp_CreateDate", formatter.format(createDate.getTime()));

        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);
        StringBuilder query = new StringBuilder();
        StringBuilder hashData = new StringBuilder();
        for (String fieldName : fieldNames) {
            String fieldValue = params.get(fieldName);
            if (fieldValue != null && !fieldValue.isBlank()) {
                String encodedValue = encode(fieldValue);
                if (!query.isEmpty()) {
                    query.append("&");
                    hashData.append("&");
                }
                // For hashData: fieldName is NOT encoded, only value
                hashData.append(fieldName).append("=").append(encodedValue);
                // For query string: both fieldName and value are encoded
                query.append(fieldName).append("=").append(encodedValue);
            }
        }

        String hash = hmacSHA512(hashSecret.trim(), hashData.toString());
        log.debug("VNPay hashData: {}", hashData);
        log.debug("VNPay computed hash: {}", hash);
        String paymentUrl = vnpayUrl + "?" + query + "&vnp_SecureHash=" + hash;
        log.debug("VNPay payment URL: {}", paymentUrl);
        return paymentUrl;
    }

    private String normalizeIp(String ipAddr) {
        if (ipAddr == null || ipAddr.isBlank() || "0:0:0:0:0:0:0:1".equals(ipAddr) || "::1".equals(ipAddr)) {
            return "127.0.0.1";
        }
        return ipAddr.split(",")[0].trim();
    }

    private String encode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            throw new BusinessException("VNPAY_ENCODING_ERROR", "Không thể mã hóa tham số VNPay");
        }
    }

    private void validateConfig() {
        if (tmnCode == null || tmnCode.isBlank() || hashSecret == null || hashSecret.isBlank()) {
            throw new BusinessException(
                "VNPAY_NOT_CONFIGURED",
                "Chưa cấu hình VNPay. Vui lòng khai báo VNPAY_TMN_CODE và VNPAY_HASH_SECRET từ tài khoản sandbox VNPay.");
        }
        if (!tmnCode.matches("[A-Za-z0-9]{8}")) {
            throw new BusinessException(
                "VNPAY_INVALID_TMN_CODE",
                "Mã website VNPay không hợp lệ. vnp_TmnCode phải có 8 ký tự chữ/số.");
        }
        if ("DEMO1234".equalsIgnoreCase(tmnCode) || "DEMOHASHHASHHASHHASH".equals(hashSecret)) {
            throw new BusinessException(
                "VNPAY_PLACEHOLDER_CONFIG",
                "Cấu hình VNPay đang dùng dữ liệu mẫu nên VNPay báo không tìm thấy website. Hãy dùng TmnCode và HashSecret thật từ sandbox VNPay.");
        }
    }

    public boolean validateCallback(Map<String, String> params) {
        String receivedHash = params.get("vnp_SecureHash");
        Map<String, String> filtered = new TreeMap<>(params);
        filtered.remove("vnp_SecureHash");
        filtered.remove("vnp_SecureHashType");

        StringBuilder hashData = new StringBuilder();
        filtered.forEach((k, v) -> {
            if (v != null && !v.isBlank()) {
                if (!hashData.isEmpty()) {
                    hashData.append("&");
                }
                hashData.append(k).append("=").append(encode(v));
            }
        });

        String computedHash = hmacSHA512(hashSecret.trim(), hashData.toString());
        return computedHash.equalsIgnoreCase(receivedHash);
    }

    private String hmacSHA512(String key, String data) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac.init(secretKey);
            byte[] bytes = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte b : bytes) result.append(String.format("%02x", b));
            return result.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute HMAC", e);
        }
    }

    public boolean isSuccess(Map<String, String> params) {
        return "00".equals(params.get("vnp_ResponseCode")) &&
               "00".equals(params.get("vnp_TransactionStatus"));
    }

    public String extractTicketCode(Map<String, String> params) {
        String txnRef = params.get("vnp_TxnRef");
        if (txnRef != null && txnRef.contains("_")) {
            return txnRef.substring(0, txnRef.lastIndexOf("_"));
        }
        return txnRef;
    }
}
