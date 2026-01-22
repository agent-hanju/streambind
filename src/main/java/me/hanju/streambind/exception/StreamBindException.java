package me.hanju.streambind.exception;

/**
 * streambind가 발생시키는 RuntimeException.
 */
public final class StreamBindException extends RuntimeException {

  /**
   * 메시지를 포함한 StreamBindException을 발생시킨다.
   *
   * @param message 예외 메시지
   */
  public StreamBindException(final String message) {
    super(message);
  }

  /**
   * 발생한 예외를 StreamBindException으로 감싼다.
   *
   * @param message 예외 메시지
   * @param cause   원인 예외
   */
  public StreamBindException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
