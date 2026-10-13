package cn.oomecho.cloud.common.core.exception;


/**
 * 携带 HTTP 状态和业务错误码的业务异常
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
public class BusinessException extends RuntimeException {
    /**
     * HTTP 状态，例如 404、409、503。
     */
    private final int status;
    /**
     * 稳定的业务错误码。
     */
    private final int code;

    /**
     * 创建业务异常，不向调用方暴露内部异常细节。
     *
     * @param status  HTTP 状态
     * @param code    业务码
     * @param message 可公开的错误说明
     */
    public BusinessException(int status, int code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    /**
     * @return HTTP 状态
     */
    public int getStatus() {
        return status;
    }

    /**
     * @return 业务错误码
     */
    public int getCode() {
        return code;
    }
}
