package com.dishcover.payment.exception;

/** Gom nhóm exception nghiệp vụ riêng của Payment Service (namespace, không khởi tạo được). */
public final class ApiExceptions {

    private ApiExceptions() {
    }

    /**
     * 400 — dữ liệu từ cổng thanh toán không hợp lệ: sai chữ ký, thiếu tham số, hoặc số tiền
     * không khớp đơn đã lưu. Cố ý KHÔNG nói rõ sai ở đâu trong thông báo trả ra ngoài để không
     * gợi ý cho người đang thử giả mạo request.
     */
    public static class InvalidPaymentCallbackException extends RuntimeException {
        public InvalidPaymentCallbackException(String message) {
            super(message);
        }
    }

    /** 503 — không dựng được yêu cầu thanh toán (cấu hình thiếu hoặc cổng không phản hồi). */
    public static class PaymentGatewayUnavailableException extends RuntimeException {
        public PaymentGatewayUnavailableException(String message) {
            super(message);
        }
    }
}
