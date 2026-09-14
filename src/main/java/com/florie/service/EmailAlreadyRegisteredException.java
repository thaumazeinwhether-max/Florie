package com.florie.service;

public class EmailAlreadyRegisteredException extends RuntimeException {
    public EmailAlreadyRegisteredException() {
        super("このメールアドレスは既に登録されています。");
    }
}
