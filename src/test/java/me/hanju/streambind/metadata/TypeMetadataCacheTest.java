package me.hanju.streambind.metadata;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import lombok.Getter;
import lombok.Setter;
import me.hanju.streambind.annotation.StreamIndex;
import me.hanju.streambind.annotation.StreamOverwrite;
import me.hanju.streambind.metadata.TypeMetadataCache.TypeInfo;

class TypeMetadataCacheTest {

  @BeforeEach
  void setUp() {
    TypeMetadataCache.clearCache();
  }

  @Nested
  class IndexFieldDetection {

    @Test
    void shouldDetectStreamIndexAnnotation() {
      TypeInfo info = TypeMetadataCache.getTypeInfo(ItemWithStreamIndex.class);

      assertEquals("idx", info.indexFieldName());
      FieldMetadata idxField = info.findField("idx");
      assertNotNull(idxField);
      assertTrue(idxField.isSpecialKey());
    }

    @Test
    void shouldDetectConventionIndexField() {
      // "index"라는 이름의 필드는 자동 인식
      TypeInfo info = TypeMetadataCache.getTypeInfo(ItemWithConventionIndex.class);

      assertEquals("index", info.indexFieldName());
      FieldMetadata indexField = info.findField("index");
      assertNotNull(indexField);
      assertTrue(indexField.isSpecialKey());
    }

    @Test
    void shouldPreferStreamIndexOverConvention() {
      // @StreamIndex가 "index" 이름보다 우선
      TypeInfo info = TypeMetadataCache.getTypeInfo(ItemWithBothIndexFields.class);

      assertEquals("customIdx", info.indexFieldName());
    }

    @Test
    void shouldIgnoreStringIndexField() {
      // String 타입의 "index" 필드는 무시됨 (int/Integer만 지원)
      TypeInfo info = TypeMetadataCache.getTypeInfo(ItemWithStringIndex.class);

      assertNull(info.indexFieldName());
    }

    @Test
    void shouldIgnoreStreamIndexOnStringField() {
      // @StreamIndex가 String 필드에 있으면 무시됨
      TypeInfo info = TypeMetadataCache.getTypeInfo(ItemWithStreamIndexOnString.class);

      assertNull(info.indexFieldName());
    }

    @Test
    void shouldSupportPrimitiveIntIndex() {
      // int (primitive) 타입도 지원
      TypeInfo info = TypeMetadataCache.getTypeInfo(ItemWithPrimitiveIndex.class);

      assertEquals("idx", info.indexFieldName());
    }
  }

  @Nested
  class MergeMethodDetection {

    @Test
    void shouldDetectMergeMethod() {
      TypeInfo info = TypeMetadataCache.getTypeInfo(ItemWithMergeMethod.class);

      assertTrue(info.hasCustomMerge());
    }

    @Test
    void shouldNotDetectMergeMethodWithWrongSignature() {
      // merge(String) 같은 잘못된 시그니처는 무시
      TypeInfo info = TypeMetadataCache.getTypeInfo(ItemWithWrongMergeSignature.class);

      assertFalse(info.hasCustomMerge());
    }

    @Test
    void shouldExecuteMergeFunction() {
      TypeInfo info = TypeMetadataCache.getTypeInfo(ItemWithMergeMethod.class);

      ItemWithMergeMethod item1 = new ItemWithMergeMethod("Hello");
      ItemWithMergeMethod item2 = new ItemWithMergeMethod(" World");

      Object result = info.merge(item1, item2);

      assertInstanceOf(ItemWithMergeMethod.class, result);
      assertEquals("Hello World", ((ItemWithMergeMethod) result).getContent());
    }
  }

  @Nested
  class StreamOverwriteDetection {

    @Test
    void shouldDetectFieldLevelStreamOverwrite() {
      TypeInfo info = TypeMetadataCache.getTypeInfo(ItemWithOverwriteField.class);

      FieldMetadata typeField = info.findField("type");
      assertNotNull(typeField);
      assertTrue(typeField.isSpecialKey());

      FieldMetadata nameField = info.findField("name");
      assertNotNull(nameField);
      assertFalse(nameField.isSpecialKey());
    }

    @Test
    void shouldDetectClassLevelStreamOverwrite() {
      TypeInfo info = TypeMetadataCache.getTypeInfo(ClassLevelOverwriteItem.class);

      // 모든 필드가 specialKey
      for (FieldMetadata field : info.fields()) {
        assertTrue(field.isSpecialKey(), "Field " + field.fieldName() + " should be specialKey");
      }
    }
  }

  @Nested
  class TypeVariableBinding {

    @Test
    void shouldBuildBindingsFromInheritance() {
      // CitedResponse extends BaseResponse<CitedMessage>
      Map<String, Type> bindings = TypeVariableResolver.buildBindings(CitedResponse.class);

      assertEquals(1, bindings.size());
      assertTrue(bindings.containsKey("T"));
      assertEquals(CitedMessage.class, bindings.get("T"));
    }

    @Test
    void shouldBuildBindingsFromMultipleLevelInheritance() {
      // ExtendedResponse extends CitedResponse extends BaseResponse<CitedMessage>
      Map<String, Type> bindings = TypeVariableResolver.buildBindings(ExtendedResponse.class);

      assertEquals(1, bindings.size());
      assertEquals(CitedMessage.class, bindings.get("T"));
    }

    @Test
    void shouldBuildBindingsForMultipleTypeParameters() {
      // MultiResponse extends BaseMultiResponse<KeyItem, ValueItem>
      Map<String, Type> bindings = TypeVariableResolver.buildBindings(MultiResponse.class);

      assertEquals(2, bindings.size());
      assertEquals(KeyItem.class, bindings.get("K"));
      assertEquals(ValueItem.class, bindings.get("V"));
    }

    @Test
    void shouldResolveTypeVariableInFieldType() {
      TypeInfo info = TypeMetadataCache.getTypeInfo(CitedResponse.class);

      FieldMetadata choicesField = info.findField("choices");
      assertNotNull(choicesField);
      assertTrue(choicesField.isList());

      // 요소 타입이 GenericChoice로 해석되어야 함
      Class<?> elementClass = choicesField.getResolvedElementClass();
      assertEquals(GenericChoice.class, elementClass);

      // 요소 타입의 바인딩에 T -> CitedMessage가 있어야 함
      Map<String, Type> elementBindings = choicesField.getElementTypeBindings();
      assertEquals(CitedMessage.class, elementBindings.get("T"));
    }
  }

  @Nested
  class FieldMetadataCollection {

    @Test
    void shouldCollectAllFieldsFromClass() {
      TypeInfo info = TypeMetadataCache.getTypeInfo(SimpleItem.class);

      assertEquals(3, info.fields().size());
      assertNotNull(info.findField("id"));
      assertNotNull(info.findField("name"));
      assertNotNull(info.findField("count"));
    }

    @Test
    void shouldCollectFieldsFromInheritanceHierarchy() {
      TypeInfo info = TypeMetadataCache.getTypeInfo(ChildItem.class);

      // 부모 필드 + 자식 필드
      assertNotNull(info.findField("parentField"));
      assertNotNull(info.findField("childField"));
    }

    @Test
    void shouldCollectRecordComponents() {
      TypeInfo info = TypeMetadataCache.getTypeInfo(SimpleRecord.class);

      assertEquals(2, info.fields().size());
      assertNotNull(info.findField("content"));
      assertNotNull(info.findField("count"));
    }

    @Test
    void shouldSkipStaticFields() {
      TypeInfo info = TypeMetadataCache.getTypeInfo(ItemWithStaticField.class);

      assertNull(info.findField("CONSTANT"));
      assertNotNull(info.findField("value"));
    }

    @Test
    void shouldSkipTransientFields() {
      TypeInfo info = TypeMetadataCache.getTypeInfo(ItemWithTransientField.class);

      assertNull(info.findField("cached"));
      assertNotNull(info.findField("value"));
    }
  }

  @Nested
  class Caching {

    @Test
    void shouldCacheTypeInfo() {
      TypeInfo info1 = TypeMetadataCache.getTypeInfo(SimpleItem.class);
      TypeInfo info2 = TypeMetadataCache.getTypeInfo(SimpleItem.class);

      assertSame(info1, info2);
    }

    @Test
    void shouldCacheSeparatelyForDifferentBindings() {
      // 같은 클래스라도 다른 바인딩이면 다른 캐시 엔트리
      TypeInfo info1 = TypeMetadataCache.getTypeInfo(
          GenericChoice.class, Map.of("T", CitedMessage.class));
      TypeInfo info2 = TypeMetadataCache.getTypeInfo(
          GenericChoice.class, Map.of("T", String.class));

      assertNotSame(info1, info2);
    }

    @Test
    void shouldClearCache() {
      TypeInfo info1 = TypeMetadataCache.getTypeInfo(SimpleItem.class);
      TypeMetadataCache.clearCache();
      TypeInfo info2 = TypeMetadataCache.getTypeInfo(SimpleItem.class);

      assertNotSame(info1, info2);
    }
  }

  // === Test DTOs ===

  @Getter
  @Setter
  static class ItemWithStreamIndex {
    @StreamIndex
    private Integer idx;
    private String value;
  }

  @Getter
  @Setter
  static class ItemWithConventionIndex {
    private Integer index;
    private String value;
  }

  @Getter
  @Setter
  static class ItemWithBothIndexFields {
    private Integer index; // 컨벤션
    @StreamIndex
    private Integer customIdx; // 어노테이션 (우선)
    private String value;
  }

  @Getter
  @Setter
  static class ItemWithStringIndex {
    private String index; // String 타입은 무시됨
    private String value;
  }

  @Getter
  @Setter
  static class ItemWithStreamIndexOnString {
    @StreamIndex
    private String idx; // String에 @StreamIndex는 무시됨
    private String value;
  }

  @Getter
  @Setter
  static class ItemWithPrimitiveIndex {
    @StreamIndex
    private int idx; // primitive int도 지원
    private String value;
  }

  @Getter
  @Setter
  static class ItemWithMergeMethod {
    private String content;

    public ItemWithMergeMethod() {
    }

    public ItemWithMergeMethod(String content) {
      this.content = content;
    }

    public ItemWithMergeMethod merge(ItemWithMergeMethod delta) {
      String newContent = (this.content == null ? "" : this.content)
          + (delta.content == null ? "" : delta.content);
      return new ItemWithMergeMethod(newContent);
    }
  }

  @Getter
  @Setter
  static class ItemWithWrongMergeSignature {
    private String content;

    // 잘못된 시그니처: String 파라미터
    public ItemWithWrongMergeSignature merge(String delta) {
      return this;
    }
  }

  @Getter
  @Setter
  static class ItemWithOverwriteField {
    @StreamOverwrite
    private String type;
    private String name;
  }

  @StreamOverwrite
  @Getter
  @Setter
  static class ClassLevelOverwriteItem {
    private String field1;
    private String field2;
  }

  @Getter
  @Setter
  static class SimpleItem {
    private String id;
    private String name;
    private Integer count;
  }

  @Getter
  @Setter
  static class ParentItem {
    private String parentField;
  }

  @Getter
  @Setter
  static class ChildItem extends ParentItem {
    private String childField;
  }

  record SimpleRecord(String content, Integer count) {
  }

  @Getter
  @Setter
  static class ItemWithStaticField {
    public static final String CONSTANT = "constant";
    private String value;
  }

  @Getter
  @Setter
  static class ItemWithTransientField {
    private transient String cached;
    private String value;
  }

  // === TypeVariable Test DTOs ===

  @Getter
  @Setter
  static class BaseResponse<T> {
    private String id;
    private List<GenericChoice<T>> choices;
  }

  @Getter
  @Setter
  static class GenericChoice<T> {
    @StreamIndex
    private Integer index;
    private T message;
  }

  @Getter
  @Setter
  static class CitedMessage {
    private String content;
    private String citation;
  }

  @Getter
  @Setter
  static class CitedResponse extends BaseResponse<CitedMessage> {
  }

  @Getter
  @Setter
  static class ExtendedResponse extends CitedResponse {
    private String metadata;
  }

  @Getter
  @Setter
  static class BaseMultiResponse<K, V> {
    private List<K> keys;
    private List<V> values;
  }

  @Getter
  @Setter
  static class KeyItem {
    @StreamIndex
    private Integer index;
    private String keyName;
  }

  @Getter
  @Setter
  static class ValueItem {
    @StreamIndex
    private Integer index;
    private String data;
  }

  @Getter
  @Setter
  static class MultiResponse extends BaseMultiResponse<KeyItem, ValueItem> {
  }
}
