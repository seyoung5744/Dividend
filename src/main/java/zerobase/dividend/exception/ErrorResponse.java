package zerobase.dividend.exception;

import lombok.Builder;
import lombok.Data;

// Error 발생 시 던져줄 모델 클래스
@Data
@Builder
public class ErrorResponse {
    private int code;
    private String message;
}
