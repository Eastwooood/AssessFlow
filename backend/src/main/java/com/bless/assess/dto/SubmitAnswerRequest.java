package com.bless.assess.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 提交答案请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitAnswerRequest {
    
    @NotNull(message = "题型不能为空")
    private String type;
    
    @NotEmpty(message = "所选答案不能为空")
    private List<Long> chosen;
}
