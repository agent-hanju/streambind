package me.hanju.streambind.metadata;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import lombok.Getter;
import lombok.Setter;

class TypeVariableResolverTest {

  @Nested
  class BuildBindings {

    @Test
    void shouldBuildBindingsFromDirectInheritance() {
      // StringResponse extends GenericResponse<String>
      Map<String, Type> bindings = TypeVariableResolver.buildBindings(StringResponse.class);

      assertEquals(1, bindings.size());
      assertEquals(String.class, bindings.get("T"));
    }

    @Test
    void shouldBuildBindingsFromMultipleLevelInheritance() {
      // ExtendedStringResponse extends StringResponse extends GenericResponse<String>
      Map<String, Type> bindings = TypeVariableResolver.buildBindings(ExtendedStringResponse.class);

      assertEquals(1, bindings.size());
      assertEquals(String.class, bindings.get("T"));
    }

    @Test
    void shouldBuildBindingsForMultipleTypeParameters() {
      // PairResponse extends GenericPair<String, Integer>
      Map<String, Type> bindings = TypeVariableResolver.buildBindings(PairResponse.class);

      assertEquals(2, bindings.size());
      assertEquals(String.class, bindings.get("K"));
      assertEquals(Integer.class, bindings.get("V"));
    }

    @Test
    void shouldReturnEmptyMapForNonGenericClass() {
      Map<String, Type> bindings = TypeVariableResolver.buildBindings(SimpleClass.class);

      assertTrue(bindings.isEmpty());
    }

    @Test
    void shouldReturnEmptyMapForNull() {
      Map<String, Type> bindings = TypeVariableResolver.buildBindings(null);

      assertTrue(bindings.isEmpty());
    }

    @Test
    void shouldHandleChainedTypeVariables() {
      // DoubleExtended extends SingleExtended<String> extends GenericResponse<T>
      // T가 다른 T를 참조하는 경우
      Map<String, Type> bindings = TypeVariableResolver.buildBindings(DoubleExtended.class);

      assertEquals(String.class, bindings.get("T"));
    }
  }

  @Nested
  class ResolveType {

    @Test
    void shouldResolveTypeVariable() throws NoSuchFieldException {
      // GenericResponse<T>의 data 필드는 T 타입
      Field dataField = GenericResponse.class.getDeclaredField("data");
      Type genericType = dataField.getGenericType();

      Map<String, Type> bindings = Map.of("T", String.class);
      Type resolved = TypeVariableResolver.resolveType(genericType, bindings);

      assertEquals(String.class, resolved);
    }

    @Test
    void shouldResolveParameterizedTypeWithTypeVariable() throws NoSuchFieldException {
      // GenericResponse<T>의 items 필드는 List<T> 타입
      Field itemsField = GenericResponse.class.getDeclaredField("items");
      Type genericType = itemsField.getGenericType();

      Map<String, Type> bindings = Map.of("T", String.class);
      Type resolved = TypeVariableResolver.resolveType(genericType, bindings);

      assertInstanceOf(ParameterizedType.class, resolved);
      ParameterizedType pt = (ParameterizedType) resolved;
      assertEquals(List.class, pt.getRawType());
      assertEquals(String.class, pt.getActualTypeArguments()[0]);
    }

    @Test
    void shouldResolveNestedParameterizedType() throws NoSuchFieldException {
      // GenericResponse<T>의 nested 필드는 GenericWrapper<T> 타입
      Field nestedField = GenericResponse.class.getDeclaredField("nested");
      Type genericType = nestedField.getGenericType();

      Map<String, Type> bindings = Map.of("T", String.class);
      Type resolved = TypeVariableResolver.resolveType(genericType, bindings);

      assertInstanceOf(ParameterizedType.class, resolved);
      ParameterizedType pt = (ParameterizedType) resolved;
      assertEquals(GenericWrapper.class, pt.getRawType());
      assertEquals(String.class, pt.getActualTypeArguments()[0]);
    }

    @Test
    void shouldReturnOriginalTypeWhenNoBindingExists() throws NoSuchFieldException {
      Field dataField = GenericResponse.class.getDeclaredField("data");
      Type genericType = dataField.getGenericType();

      // 빈 바인딩
      Type resolved = TypeVariableResolver.resolveType(genericType, Map.of());

      // 원본 TypeVariable 그대로 반환
      assertSame(genericType, resolved);
    }

    @Test
    void shouldReturnOriginalTypeForNullBindings() throws NoSuchFieldException {
      Field dataField = GenericResponse.class.getDeclaredField("data");
      Type genericType = dataField.getGenericType();

      Type resolved = TypeVariableResolver.resolveType(genericType, null);

      assertSame(genericType, resolved);
    }

    @Test
    void shouldReturnNullForNullType() {
      Type resolved = TypeVariableResolver.resolveType(null, Map.of("T", String.class));

      assertNull(resolved);
    }

    @Test
    void shouldNotModifyNonGenericType() {
      Type resolved = TypeVariableResolver.resolveType(String.class, Map.of("T", Integer.class));

      assertEquals(String.class, resolved);
    }
  }

  @Nested
  class GetRawClass {

    @Test
    void shouldGetRawClassFromClass() {
      Class<?> raw = TypeVariableResolver.getRawClass(String.class);

      assertEquals(String.class, raw);
    }

    @Test
    void shouldGetRawClassFromParameterizedType() throws NoSuchFieldException {
      Field itemsField = GenericResponse.class.getDeclaredField("items");
      Type genericType = itemsField.getGenericType();

      Class<?> raw = TypeVariableResolver.getRawClass(genericType);

      assertEquals(List.class, raw);
    }

    @Test
    void shouldReturnNullForTypeVariable() throws NoSuchFieldException {
      Field dataField = GenericResponse.class.getDeclaredField("data");
      Type genericType = dataField.getGenericType();

      Class<?> raw = TypeVariableResolver.getRawClass(genericType);

      assertNull(raw);
    }

    @Test
    void shouldReturnNullForNull() {
      Class<?> raw = TypeVariableResolver.getRawClass(null);

      assertNull(raw);
    }
  }

  @Nested
  class ExtractBindingsFromType {

    @Test
    void shouldExtractBindingsFromParameterizedType() throws NoSuchFieldException {
      // List<String> 타입에서 바인딩 추출
      Field field = ContainerWithConcreteList.class.getDeclaredField("strings");
      Type genericType = field.getGenericType();

      Map<String, Type> bindings = TypeVariableResolver.extractBindingsFromType(genericType);

      assertEquals(1, bindings.size());
      assertEquals(String.class, bindings.get("E")); // List<E>의 E
    }

    @Test
    void shouldExtractMultipleBindings() throws NoSuchFieldException {
      // Map<String, Integer> 타입에서 바인딩 추출
      Field field = ContainerWithConcreteMap.class.getDeclaredField("data");
      Type genericType = field.getGenericType();

      Map<String, Type> bindings = TypeVariableResolver.extractBindingsFromType(genericType);

      assertEquals(2, bindings.size());
      assertEquals(String.class, bindings.get("K"));
      assertEquals(Integer.class, bindings.get("V"));
    }

    @Test
    void shouldReturnEmptyMapForNonParameterizedType() {
      Map<String, Type> bindings = TypeVariableResolver.extractBindingsFromType(String.class);

      assertTrue(bindings.isEmpty());
    }

    @Test
    void shouldReturnEmptyMapForNull() {
      Map<String, Type> bindings = TypeVariableResolver.extractBindingsFromType(null);

      assertTrue(bindings.isEmpty());
    }
  }

  @Nested
  class ResolvedParameterizedType {

    @Test
    void shouldCreateValidResolvedType() throws NoSuchFieldException {
      Field itemsField = GenericResponse.class.getDeclaredField("items");
      Type genericType = itemsField.getGenericType();

      Map<String, Type> bindings = Map.of("T", String.class);
      Type resolved = TypeVariableResolver.resolveType(genericType, bindings);

      assertInstanceOf(ParameterizedType.class, resolved);
      ParameterizedType pt = (ParameterizedType) resolved;

      // toString이 올바르게 동작하는지
      assertTrue(pt.toString().contains("List"));
      assertTrue(pt.toString().contains("String"));
    }

    @Test
    void shouldSupportEquality() throws NoSuchFieldException {
      Field itemsField = GenericResponse.class.getDeclaredField("items");
      Type genericType = itemsField.getGenericType();

      Map<String, Type> bindings = Map.of("T", String.class);
      Type resolved1 = TypeVariableResolver.resolveType(genericType, bindings);
      Type resolved2 = TypeVariableResolver.resolveType(genericType, bindings);

      assertEquals(resolved1, resolved2);
      assertEquals(resolved1.hashCode(), resolved2.hashCode());
    }
  }

  // === Test Classes ===

  @Getter
  @Setter
  static class GenericResponse<T> {
    private String id;
    private T data;
    private List<T> items;
    private GenericWrapper<T> nested;
  }

  @Getter
  @Setter
  static class GenericWrapper<T> {
    private T value;
  }

  @Getter
  @Setter
  static class StringResponse extends GenericResponse<String> {
  }

  @Getter
  @Setter
  static class ExtendedStringResponse extends StringResponse {
    private String extra;
  }

  @Getter
  @Setter
  static class GenericPair<K, V> {
    private K key;
    private V value;
  }

  @Getter
  @Setter
  static class PairResponse extends GenericPair<String, Integer> {
  }

  @Getter
  @Setter
  static class SimpleClass {
    private String value;
  }

  // T를 다시 T로 전달하는 중간 클래스
  @Getter
  @Setter
  static class SingleExtended<T> extends GenericResponse<T> {
  }

  @Getter
  @Setter
  static class DoubleExtended extends SingleExtended<String> {
  }

  @Getter
  @Setter
  static class ContainerWithConcreteList {
    private List<String> strings;
  }

  @Getter
  @Setter
  static class ContainerWithConcreteMap {
    private Map<String, Integer> data;
  }
}
