package com.bless.assess.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 创建题目请求DTO（GM后台用）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateQuestionRequest {
    
    @NotBlank(message = "题干不能为空")
    private String stem;
    
    @NotBlank(message = "题型不能为空")
    private String questionType;
    
    private List<OptionItem> options;
    
    private Object correctAnswer;
    
    @NotNull(message = "难度不能为空")
    private Integer difficulty;
    
    private List<String> tags;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptionItem {
        private Long optionId;
        private String text;
        private Boolean isCorrect;
    }
}
