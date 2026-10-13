package cn.oomecho.cloud.common.web.exception;


import cn.oomecho.cloud.common.core.exception.BusinessException;
import cn.oomecho.cloud.common.core.result.Result;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理；保留 HTTP 错误语义并屏蔽内部堆栈
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 将业务异常转换为对应 HTTP 状态，避免全部返回 HTTP 200。
     *
     * @param exception 业务异常
     * @return 统一错误响应
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> business(BusinessException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(Result.failure(exception.getCode(), exception.getMessage()));
    }

    /**
     * 返回第一个字段校验失败原因，供接口文档中的调用方修正参数。
     *
     * @param exception 请求体字段校验异常
     * @return HTTP 400 响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> validation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + "：" + error.getDefaultMessage())
                .findFirst().orElse("请求参数不合法");
        return ResponseEntity.badRequest().body(Result.failure(40000, message));
    }

    /**
     * 统一处理参数格式、方法校验和 JSON 解析错误，不回传框架堆栈。
     *
     * @return HTTP 400 响应
     */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            ConstraintViolationException.class, HandlerMethodValidationException.class})
    public ResponseEntity<Result<Void>> invalidRequest() {
        return ResponseEntity.badRequest().body(Result.failure(40000, "请求参数格式或取值不合法"));
    }

    /**
     * @return 请求方法不支持时的 HTTP 405 响应
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> methodNotAllowed() {
        return ResponseEntity.status(405).body(Result.failure(40500, "请求方法不支持"));
    }

    /**
     * @return 请求媒体类型不支持时的 HTTP 415 响应
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Result<Void>> unsupportedMediaType() {
        return ResponseEntity.status(415).body(Result.failure(41500, "请求媒体类型不支持"));
    }

    /**
     * @return 未找到接口时的 HTTP 404 响应
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> notFound() {
        return ResponseEntity.status(404).body(Result.failure(40400, "接口不存在"));
    }

    /**
     * 记录未知异常供排查，调用方仅收到稳定的错误说明。
     *
     * @param exception 未预期异常
     * @return HTTP 500 响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> unexpected(Exception exception) {
        log.error("请求处理失败", exception);
        return ResponseEntity.internalServerError().body(Result.failure(50000, "服务内部错误"));
    }
}
