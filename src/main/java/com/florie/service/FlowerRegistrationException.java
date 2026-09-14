package com.florie.service;

// 登録できない業務上の理由を、内部例外とは区別して画面へ伝える。
public class FlowerRegistrationException extends RuntimeException {
    public FlowerRegistrationException(String message) {
        super(message);
    }
}
