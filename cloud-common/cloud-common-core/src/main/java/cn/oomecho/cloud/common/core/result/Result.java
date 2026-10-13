package cn.oomecho.cloud.common.core.result;


/**
 * 统一响应格式；code 为业务码，timestamp 为毫秒时间戳
 *
 * @param <T>       响应数据类型
 * @param code      业务状态码
 * @param message   响应说明
 * @param data      响应数据；失败时为 null
 * @param timestamp 响应生成时的毫秒时间戳
 * @author xuesong.lei
 * @since 2026-10-12
 */
public record Result<T>(int code, String message, T data, long timestamp) {
    /**
     * 包装成功响应；时间戳便于关联请求发生的时间。
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return 成功响应
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data, System.currentTimeMillis());
    }

    /**
     * 包装失败响应；HTTP 状态由 Web 层设置，业务码用于区分具体原因。
     *
     * @param code    业务错误码
     * @param message 面向调用方的错误说明
     * @param <T>     数据类型
     * @return 不携带业务数据的失败响应
     */
    public static <T> Result<T> failure(int code, String message) {
        return new Result<>(code, message, null, System.currentTimeMillis());
    }
}
