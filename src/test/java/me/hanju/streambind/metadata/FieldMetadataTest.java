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
import me.hanju.streambind.metadata.TypeMetadataCache.TypeInfo;

class FieldMetadataTest {

  @BeforeEach
  void setUp() {
    TypeMetadataCache.clearCache();
  }

  @Nested
  class TypeDetection {

    @Test
    void shouldDetectStringType() {
      TypeInfo info = TypeMetadataCache.getTypeInfo(TypeDetectionDto.class);

      FieldMetadata field = info.findField("stringField");
      assertTrue(field.isString());
      assertFalse(field.isNumber());
      assertFalse(field.isList());
      assertFalse(field.isObject());
    }

    @Test
    void shouldDetectNumberTypes() {
      TypeInfo info = TypeMetadataCache.getTypeInfo(TypeDetectionDto.class);

      assertTrue(info.findField("integerField").isNumber());
      assertTrue(info.findField("longField").isNumber());
      assertTrue(info.findField("doubleField").isNumber());
      assertTrue(info.findField("primitiveInt").isNumber());
    }

    @Test
    void shouldDetectPrimitiveTypes() {
      TypeInfo info = TypeMetadataCache.getTypeInfo(TypeDetectionDto.class);

      assertTrue(info.findField("primitiveInt").isPrimitiveType());
      assertTrue(info.findField("primitiveDouble").isPrimitiveType());
      assertTrue(info.findField("primitiveBoolean").isPrimitiveType());
      assertFalse(info.findField("integerField").isPrimitiveType());
    }

    @Test
    void shouldDetectListTypes() {
      TypeInfo info = TypeMetadataCache.getTypeInfo(CollectionDto.class);

      FieldMetadata stringList = info.findField("stringList");
      assertTrue(stringList.isList());
      assertTrue(stringList.isPrimitiveList());
      assertFalse(stringList.isObjectList());

      FieldMetadata objectList = info.findField("objectList");
      assertTrue(objectList.isList());
      assertFalse(objectList.isPrimitiveList());
      assertTrue(objectList.isObjectList());
    }

    @Test
    void shouldDetectArrayTypes() {
      TypeInfo info = TypeMetadataCache.getTypeInfo(ArrayDto.class);

      FieldMetadata stringArray = info.findField("stringArray");
      assertTrue(stringArray.isArray());
      assertTrue(stringArray.isPrimitiveArray());
      assertFalse(stringArray.isObjectArray());

      FieldMetadata intArray = info.findField("intArray");
      assertTrue(intArray.isArray());
      assertTrue(intArray.isPrimitiveArray());

      FieldMetadata objectArray = info.findField("objectArray");
      assertTrue(objectArray.isArray());
      assertFalse(objectArray.isPrimitiveArray());
      assertTrue(objectArray.isObjectArray());
    }

    @Test
    void shouldDetectMapTypes() {
      TypeInfo info = TypeMetadataCache.getTypeInfo(MapDto.class);

      FieldMetadata primitiveMap = info.findField("primitiveMap");
      assertTrue(primitiveMap.isMap());
      assertTrue(primitiveMap.isPrimitiveValueMap());
      assertFalse(primitiveMap.isObjectValueMap());

      FieldMetadata objectMap = info.findField("objectMap");
      assertTrue(objectMap.isMap());
      assertFalse(objectMap.isPrimitiveValueMap());
      assertTrue(objectMap.isObjectValueMap());
    }

    @Test
    void shouldDetectObjectType() {
      TypeInfo info = TypeMetadataCache.getTypeInfo(NestedDto.class);

      FieldMetadata nestedField = info.findField("nested");
      assertTrue(nestedField.isObject());
      assertFalse(nestedField.isString());
      assertFalse(nestedField.isNumber());
      assertFalse(nestedField.isList());
    }
  }

  @Nested
  class ElementTypeExtraction {

    @Test
    void shouldExtractListElementType() {
      TypeInfo info = TypeMetadataCache.getTypeInfo(CollectionDto.class);

      FieldMetadata stringList = info.findField("stringList");
      assertEquals(String.class, stringList.elementType());

      FieldMetadata objectList = info.findField("objectList");
      assertEquals(SimpleItem.class, objectList.elementType());
    }

    @Test
    void shouldExtractArrayElementType() {
      TypeInfo info = TypeMetadataCache.getTypeInfo(ArrayDto.class);

      assertEquals(String.class, info.findField("stringArray").elementType());
      assertEquals(int.class, info.findField("intArray").elementType());
      assertEquals(SimpleItem.class, info.findField("objectArray").elementType());
    }

    @Test
    void shouldExtractMapValueType() {
      TypeInfo info = TypeMetadataCache.getTypeInfo(MapDto.class);

      assertEquals(String.class, info.findField("primitiveMap").elementType());
      assertEquals(SimpleItem.class, info.findField("objectMap").elementType());
    }
  }

  @Nested
  class ResolvedTypeHandling {

    @Test
    void shouldResolveTypeVariableInField() {
      // GenericContainer<String>의 message 필드는 T -> String으로 해석
      TypeInfo info = TypeMetadataCache.getTypeInfo(
          GenericContainer.class, Map.of("T", (Type) String.class));

      FieldMetadata messageField = info.findField("message");
      assertEquals(String.class, messageField.getResolvedFieldClass());
    }

    @Test
    void shouldResolveTypeVariableInListElement() {
      // GenericListContainer<Item>의 items 필드는 List<T> -> List<Item>으로 해석
      TypeInfo info = TypeMetadataCache.getTypeInfo(
          GenericListContainer.class, Map.of("T", (Type) SimpleItem.class));

      FieldMetadata itemsField = info.findField("items");
      assertEquals(SimpleItem.class, itemsField.getResolvedElementClass());
    }

    @Test
    void shouldResolveNestedTypeVariable() {
      // CitedResponse extends BaseResponse<CitedMessage>
      // choices 필드는 List<GenericChoice<CitedMessage>>가 됨
      TypeInfo info = TypeMetadataCache.getTypeInfo(CitedResponse.class);

      FieldMetadata choicesField = info.findField("choices");
      assertEquals(GenericChoice.class, choicesField.getResolvedElementClass());

      // GenericChoice<T>의 T가 CitedMessage로 바인딩되어야 함
      Map<String, Type> bindings = choicesField.getElementTypeBindings();
      assertEquals(CitedMessage.class, bindings.get("T"));
    }

    @Test
    void shouldGetFieldTypeInfo() {
      TypeInfo containerInfo = TypeMetadataCache.getTypeInfo(NestedDto.class);
      FieldMetadata nestedField = containerInfo.findField("nested");

      TypeInfo nestedTypeInfo = nestedField.getFieldTypeInfo();
      assertNotNull(nestedTypeInfo);
      assertNotNull(nestedTypeInfo.findField("value"));
    }

    @Test
    void shouldGetElementTypeInfo() {
      TypeInfo containerInfo = TypeMetadataCache.getTypeInfo(CollectionDto.class);
      FieldMetadata objectListField = containerInfo.findField("objectList");

      TypeInfo elementTypeInfo = objectListField.getElementTypeInfo();
      assertNotNull(elementTypeInfo);
      assertNotNull(elementTypeInfo.findField("value"));
    }
  }

  @Nested
  class GetterSetterAccess {

    @Test
    void shouldGetValueUsingGetter() {
      TypeInfo info = TypeMetadataCache.getTypeInfo(SimpleItem.class);
      FieldMetadata valueField = info.findField("value");

      SimpleItem item = new SimpleItem();
      item.setValue("test");

      Object value = valueField.getValue(item);
      assertEquals("test", value);
    }

    @Test
    void shouldReturnNullForNullSource() {
      TypeInfo info = TypeMetadataCache.getTypeInfo(SimpleItem.class);
      FieldMetadata valueField = info.findField("value");

      Object value = valueField.getValue(null);
      assertNull(value);
    }

    @Test
    void shouldHaveSetterForMutableClass() {
      TypeInfo info = TypeMetadataCache.getTypeInfo(SimpleItem.class);
      FieldMetadata valueField = info.findField("value");

      assertNotNull(valueField.setter());
    }

    @Test
    void shouldNotHaveSetterForRecord() {
      TypeInfo info = TypeMetadataCache.getTypeInfo(SimpleRecord.class);
      FieldMetadata contentField = info.findField("content");

      assertNull(contentField.setter());
    }

    @Test
    void shouldHandlePrimitiveGetterWithBoxing() {
      TypeInfo info = TypeMetadataCache.getTypeInfo(TypeDetectionDto.class);
      FieldMetadata field = info.findField("primitiveInt");

      TypeDetectionDto dto = new TypeDetectionDto();
      dto.setPrimitiveInt(42);

      Object value = field.getValue(dto);
      assertEquals(42, value);
    }
  }

  // === Test DTOs ===

  @Getter
  @Setter
  static class TypeDetectionDto {
    private String stringField;
    private Integer integerField;
    private Long longField;
    private Double doubleField;
    private int primitiveInt;
    private double primitiveDouble;
    private boolean primitiveBoolean;
  }

  @Getter
  @Setter
  static class CollectionDto {
    private List<String> stringList;
    private List<SimpleItem> objectList;
  }

  @Getter
  @Setter
  static class ArrayDto {
    private String[] stringArray;
    private int[] intArray;
    private SimpleItem[] objectArray;
  }

  @Getter
  @Setter
  static class MapDto {
    private Map<String, String> primitiveMap;
    private Map<String, SimpleItem> objectMap;
  }

  @Getter
  @Setter
  static class NestedDto {
    private SimpleItem nested;
  }

  @Getter
  @Setter
  static class SimpleItem {
    @StreamIndex
    private Integer index;
    private String value;
  }

  record SimpleRecord(String content, Integer count) {
  }

  // === Generic Type Test DTOs ===

  @Getter
  @Setter
  static class GenericContainer<T> {
    private T message;
  }

  @Getter
  @Setter
  static class GenericListContainer<T> {
    private List<T> items;
  }

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
}
