package me.hanju.streambind.exception;

/**
 * 스트림 바인딩 처리 중 발생하는 예외.
 */
public final class StreamBindException extends RuntimeException {

  /**
   * 메시지를 포함한 예외를 생성한다.
   *
   * @param message 예외 메시지
   */
  public StreamBindException(final String message) {
    super(message);
  }

  /**
   * 메시지와 원인을 포함한 예외를 생성한다.
   *
   * @param message 예외 메시지
   * @param cause   원인 예외
   */
  public StreamBindException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
