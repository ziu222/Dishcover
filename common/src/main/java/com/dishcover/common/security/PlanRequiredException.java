package com.dishcover.common.security;

/** Ném khi user không đủ plan cho endpoint yêu cầu @RequiresPlan — mỗi service tự map sang HTTP 402. */
public class PlanRequiredException extends RuntimeException {
    public PlanRequiredException(String requiredPlan) {
        super("Yêu cầu gói " + requiredPlan + " để dùng tính năng này");
    }
}
