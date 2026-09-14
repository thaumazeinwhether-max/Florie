package com.florie.form;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class UserRegisterForm {

    @NotBlank(message = "ニックネームを入力してください。")
    @Size(max = 20, message = "ニックネームは20文字以内で入力してください。")
    private String nickname;

    @NotBlank(message = "メールアドレスを入力してください。")
    @Email(message = "正しい形式のメールアドレスを入力してください。")
    @Size(max = 255, message = "メールアドレスは255文字以内で入力してください。")
    private String email;

    @NotBlank(message = "パスワードを入力してください。")
    @Size(min = 8, max = 72, message = "パスワードは8文字以上、72文字以内で入力してください。")
    private String password;

    @NotBlank(message = "確認用のパスワードを入力してください。")
    private String passwordConfirmation;

    @AssertTrue(message = "パスワードはUTF-8で72バイト以内にしてください（日本語は通常1文字3バイトです）。")
    public boolean isPasswordWithinByteLimit() {
        // BCryptが扱える長さを超えた値を、切り詰めて保存しないための確認。
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    @AssertTrue(message = "パスワードが一致していません。")
    public boolean isPasswordsMatching() {
        if (password == null || passwordConfirmation == null) {
            return true; // 未入力は各項目の必須チェックで表示する。
        }
        return password.equals(passwordConfirmation);
    }

    public String getNickname() {
        return nickname;
    }
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        if (email == null) {
            this.email = null;
        } else {
            this.email = email.strip().toLowerCase(Locale.ROOT);
        }
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public String getPasswordConfirmation() {
        return passwordConfirmation;
    }
    public void setPasswordConfirmation(String passwordConfirmation) {
        this.passwordConfirmation = passwordConfirmation;
    }

    public void clearPasswords() {
        password = null;
        passwordConfirmation = null;
    }
}
