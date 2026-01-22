package me.hanju.streambind.map;

import java.util.List;

import reactor.core.publisher.Flux;

/**
 * 델타 변환 인터페이스.
 *
 * <p>
 * {@code T} 타입의 델타 스트림을 0개 이상의 {@code R} 타입 델타로 변환한다.
 * 기본적으로는 상태 없는 변환기로 사용할 수 있으며, {@link #flush()}를 오버라이드하면
 * 내부 상태를 유지하여 여러 델타에 걸쳐 정보를 누적하는 변환기도 구현할 수 있다.
 * </p>
 *
 * <p>반환 타입이 {@code List<R>}인 이유:</p>
 * <ul>
 *   <li>0개 출력 - 필터링 또는 버퍼링 (빈 리스트 반환)</li>
 *   <li>1개 출력 - 단순 1:1 변환</li>
 *   <li>N개 출력 - 델타 분할 또는 확장</li>
 * </ul>
 *
 * <p>사용 예시:</p>
 * <pre>{@code
 * // SDK 청크를 커스텀 델타로 변환 (1:1, 상태 없음)
 * StreamMapper<SdkChunk, MyDelta> mapper = chunk ->
 *     List.of(new MyDelta(chunk.getContent(), chunk.getIndex()));
 *
 * // 버퍼링 예시 (0:N, 상태 있음 - flush 오버라이드)
 * StreamMapper<String, String> lineMapper = new StreamMapper<>() {
 *   private final StringBuilder buffer = new StringBuilder();
 *
 *   @Override
 *   public List<String> map(String chunk) {
 *     buffer.append(chunk);
 *     List<String> lines = new ArrayList<>();
 *     int idx;
 *     while ((idx = buffer.indexOf("\n")) >= 0) {
 *       lines.add(buffer.substring(0, idx));
 *       buffer.delete(0, idx + 1);
 *     }
 *     return lines; // 0개 또는 N개 반환
 *   }
 *
 *   @Override
 *   public List<String> flush() {
 *     if (buffer.isEmpty()) return List.of();
 *     String remaining = buffer.toString();
 *     buffer.setLength(0);
 *     return List.of(remaining);
 *   }
 * };
 *
 * // 사용 (apply 메서드 활용)
 * Flux<String> lines = lineMapper.apply(flux);
 *
 * // 또는 직접 체이닝
 * Flux<String> lines = flux
 *     .flatMapIterable(lineMapper::map)
 *     .concatWith(Flux.defer(() -> Flux.fromIterable(lineMapper.flush())));
 * }</pre>
 *
 * @param <T> 입력 델타 타입
 * @param <R> 출력 델타 타입
 */
@FunctionalInterface
public interface StreamMapper<T, R> {

  /**
   * 입력 델타를 0개 이상의 출력 델타로 변환한다.
   *
   * @param delta 변환할 입력 델타
   * @return 변환된 출력 델타 리스트 (0개 이상)
   */
  List<R> map(T delta);

  /**
   * 버퍼링된 상태를 플러시하고 남은 델타를 반환한다.
   * 기본 구현은 빈 리스트를 반환한다.
   *
   * @return 플러시된 출력 델타 리스트
   */
  default List<R> flush() {
    return List.of();
  }

  /**
   * Flux에 이 매퍼를 적용하고 완료 시 flush를 호출한다.
   *
   * @param source 변환할 소스 Flux
   * @return 변환된 Flux
   */
  default Flux<R> apply(Flux<T> source) {
    return source
        .flatMapIterable(this::map)
        .concatWith(Flux.defer(() -> Flux.fromIterable(this.flush())));
  }
}
