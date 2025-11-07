package io.github.rosestack.spring.boot.xxljob.exception;

/**
 * XXL-Job 相关异常
 */
public class XxlJobException extends RuntimeException {

    public XxlJobException(String message) {
        super(message);
    }

    public XxlJobException(String message, Throwable cause) {
        super(message, cause);
    }
}
