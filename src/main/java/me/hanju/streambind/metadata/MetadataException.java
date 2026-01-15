package me.hanju.streambind.metadata;

/**
 * 메타데이터 처리 중 발생하는 예외.
 */
public final class MetadataException extends RuntimeException {

  /**
   * 메시지와 원인을 포함한 예외를 생성한다.
   *
   * @param message 예외 메시지
   * @param cause   원인 예외
   */
  public MetadataException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
