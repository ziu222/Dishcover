package com.dishcover.user.admin;

/** Admin tự nhắm vào tài khoản của chính mình ở thao tác nguy hiểm — map ra HTTP 409. */
public class SelfTargetException extends RuntimeException {
    public SelfTargetException(String message) {
        super(message);
    }
}
