package com.florie.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class FlowerRegisterForm {
    @NotNull(message = "お花を選択してください。")
    @Positive(message = "一覧からお花を選択してください。")
    private Long flowerTypeId;

    @NotBlank(message = "お花のニックネームを入力してください。")
    @Size(max = 20, message = "お花のニックネームは20文字以内で入力してください。")
    private String flowerNickname;

    public Long getFlowerTypeId() {
        return flowerTypeId;
    }

    public void setFlowerTypeId(Long flowerTypeId) {
        this.flowerTypeId = flowerTypeId;
    }

    public String getFlowerNickname() {
        return flowerNickname;
    }

    public void setFlowerNickname(String flowerNickname) {
        this.flowerNickname = flowerNickname;
    }
}
