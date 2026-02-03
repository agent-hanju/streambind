package me.hanju.streambind.merge;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import lombok.Getter;
import lombok.Setter;
import me.hanju.streambind.annotation.StreamIndex;
import me.hanju.streambind.metadata.TypeMetadataCache;

class ObjectBuilderTest {

  @BeforeEach
  void setUp() {
    TypeMetadataCache.clearCache();
  }

  @Nested
  class BasicClassBuild {

    @Test
    void shouldBuildSimpleClass() {
      Map<String, Object> values = new HashMap<>();
      values.put("name", "John");
      values.put("count", 42);

      SimpleDto result = ObjectBuilder.build(SimpleDto.class, values);

      assertNotNull(result);
      assertEquals("John", result.getName());
      assertEquals(42, result.getCount());
    }

    @Test
    void shouldReturnNullForNullValues() {
      SimpleDto result = ObjectBuilder.build(SimpleDto.class, null);
      assertNull(result);
    }

    @Test
    void shouldHandleMissingFields() {
      Map<String, Object> values = new HashMap<>();
      values.put("name", "John");
      // count is missing

      SimpleDto result = ObjectBuilder.build(SimpleDto.class, values);

      assertEquals("John", result.getName());
      assertNull(result.getCount());
    }
  }

  @Nested
  class RecordBuild {

    @Test
    void shouldBuildRecord() {
      Map<String, Object> values = new HashMap<>();
      values.put("content", "Hello");
      values.put("count", 10);

      SimpleRecord result = ObjectBuilder.build(SimpleRecord.class, values);

      assertNotNull(result);
      assertEquals("Hello", result.content());
      assertEquals(10, result.count());
    }

    @Test
    void shouldBuildRecordWithNullValues() {
      Map<String, Object> values = new HashMap<>();
      values.put("content", null);
      values.put("count", 5);

      SimpleRecord result = ObjectBuilder.build(SimpleRecord.class, values);

      assertNull(result.content());
      assertEquals(5, result.count());
    }

    @Test
    void shouldBuildNestedRecord() {
      Map<String, Object> nestedValues = new HashMap<>();
      nestedValues.put("content", "Nested");
      nestedValues.put("count", 20);

      Map<String, Object> values = new HashMap<>();
      values.put("id", "parent");
      values.put("child", nestedValues);

      ParentRecord result = ObjectBuilder.build(ParentRecord.class, values);

      assertEquals("parent", result.id());
      assertNotNull(result.child());
      assertEquals("Nested", result.child().content());
      assertEquals(20, result.child().count());
    }
  }

  @Nested
  class NestedObjectBuild {

    @Test
    void shouldBuildNestedObject() {
      Map<String, Object> nestedValues = new HashMap<>();
      nestedValues.put("name", "Child");
      nestedValues.put("count", 5);

      Map<String, Object> values = new HashMap<>();
      values.put("id", "parent-1");
      values.put("nested", nestedValues);

      ParentDto result = ObjectBuilder.build(ParentDto.class, values);

      assertEquals("parent-1", result.getId());
      assertNotNull(result.getNested());
      assertEquals("Child", result.getNested().getName());
      assertEquals(5, result.getNested().getCount());
    }

    @Test
    void shouldBuildDeeplyNestedObject() {
      Map<String, Object> leafValues = new HashMap<>();
      leafValues.put("name", "Leaf");
      leafValues.put("count", 1);

      Map<String, Object> childValues = new HashMap<>();
      childValues.put("id", "child");
      childValues.put("nested", leafValues);

      Map<String, Object> values = new HashMap<>();
      values.put("name", "Root");
      values.put("child", childValues);

      GrandParentDto result = ObjectBuilder.build(GrandParentDto.class, values);

      assertEquals("Root", result.getName());
      assertEquals("child", result.getChild().getId());
      assertEquals("Leaf", result.getChild().getNested().getName());
    }
  }

  @Nested
  class ListBuild {

    @Test
    void shouldBuildObjectList() {
      List<Map<String, Object>> itemMaps = new ArrayList<>();

      Map<String, Object> item1 = new HashMap<>();
      item1.put("index", 0);
      item1.put("value", "first");
      itemMaps.add(item1);

      Map<String, Object> item2 = new HashMap<>();
      item2.put("index", 1);
      item2.put("value", "second");
      itemMaps.add(item2);

      Map<String, Object> values = new HashMap<>();
      values.put("items", itemMaps);

      ListContainerDto result = ObjectBuilder.build(ListContainerDto.class, values);

      assertEquals(2, result.getItems().size());
      assertEquals(0, result.getItems().get(0).getIndex());
      assertEquals("first", result.getItems().get(0).getValue());
      assertEquals(1, result.getItems().get(1).getIndex());
      assertEquals("second", result.getItems().get(1).getValue());
    }

    @Test
    void shouldPreservePrimitiveList() {
      Map<String, Object> values = new HashMap<>();
      values.put("tags", List.of("a", "b", "c"));

      TagContainerDto result = ObjectBuilder.build(TagContainerDto.class, values);

      assertEquals(3, result.getTags().size());
      assertEquals("a", result.getTags().get(0));
    }
  }

  @Nested
  class ArrayBuild {

    @Test
    void shouldBuildStringArray() {
      Map<String, Object> values = new HashMap<>();
      values.put("tags", List.of("a", "b", "c"));

      StringArrayDto result = ObjectBuilder.build(StringArrayDto.class, values);

      assertEquals(3, result.getTags().length);
      assertEquals("a", result.getTags()[0]);
      assertEquals("b", result.getTags()[1]);
      assertEquals("c", result.getTags()[2]);
    }

    @Test
    void shouldBuildPrimitiveIntArray() {
      Map<String, Object> values = new HashMap<>();
      values.put("numbers", List.of(1, 2, 3, 4));

      IntArrayDto result = ObjectBuilder.build(IntArrayDto.class, values);

      assertEquals(4, result.getNumbers().length);
      assertEquals(1, result.getNumbers()[0]);
      assertEquals(4, result.getNumbers()[3]);
    }

    @Test
    void shouldBuildObjectArray() {
      List<Map<String, Object>> itemMaps = new ArrayList<>();

      Map<String, Object> item1 = new HashMap<>();
      item1.put("index", 0);
      item1.put("value", "first");
      itemMaps.add(item1);

      Map<String, Object> values = new HashMap<>();
      values.put("items", itemMaps);

      ObjectArrayDto result = ObjectBuilder.build(ObjectArrayDto.class, values);

      assertEquals(1, result.getItems().length);
      assertEquals(0, result.getItems()[0].getIndex());
      assertEquals("first", result.getItems()[0].getValue());
    }
  }

  @Nested
  class MapBuild {

    @Test
    void shouldBuildPrimitiveValueMap() {
      Map<String, Object> mapValues = new HashMap<>();
      mapValues.put("key1", "value1");
      mapValues.put("key2", "value2");

      Map<String, Object> values = new HashMap<>();
      values.put("metadata", mapValues);

      PrimitiveMapDto result = ObjectBuilder.build(PrimitiveMapDto.class, values);

      assertEquals(2, result.getMetadata().size());
      assertEquals("value1", result.getMetadata().get("key1"));
    }

    @Test
    void shouldBuildObjectValueMap() {
      Map<String, Object> item1 = new HashMap<>();
      item1.put("name", "Item1");
      item1.put("count", 10);

      Map<String, Object> mapValues = new HashMap<>();
      mapValues.put("first", item1);

      Map<String, Object> values = new HashMap<>();
      values.put("items", mapValues);

      ObjectMapDto result = ObjectBuilder.build(ObjectMapDto.class, values);

      assertEquals(1, result.getItems().size());
      assertNotNull(result.getItems().get("first"));
      assertEquals("Item1", result.getItems().get("first").getName());
      assertEquals(10, result.getItems().get("first").getCount());
    }
  }

  @Nested
  class RuntimeTypeResolution {

    @Test
    void shouldResolveRuntimeTypeForInterface() {
      // $$type$$에 실제 구현 클래스가 저장된 경우
      Map<String, Object> docValues = new HashMap<>();
      docValues.put("$$type$$", SimpleDocument.class);
      docValues.put("title", "Hello");
      docValues.put("content", "World");

      Map<String, Object> values = new HashMap<>();
      values.put("name", "Container");
      values.put("document", docValues);

      InterfaceContainerDto result = ObjectBuilder.build(InterfaceContainerDto.class, values);

      assertNotNull(result.getDocument());
      assertInstanceOf(SimpleDocument.class, result.getDocument());
      assertEquals("Hello", result.getDocument().getTitle());
      assertEquals("World", result.getDocument().getContent());
    }

    @Test
    void shouldResolveRuntimeTypeForConcreteSubclass() {
      // Animal 필드에 Dog 타입이 저장된 경우
      Map<String, Object> petValues = new HashMap<>();
      petValues.put("$$type$$", Dog.class);
      petValues.put("name", "Buddy");
      petValues.put("breed", "Golden");

      Map<String, Object> values = new HashMap<>();
      values.put("owner", "John");
      values.put("pet", petValues);

      AnimalContainerDto result = ObjectBuilder.build(AnimalContainerDto.class, values);

      assertNotNull(result.getPet());
      assertInstanceOf(Dog.class, result.getPet());
      assertEquals("Buddy", result.getPet().getName());
      assertEquals("Golden", ((Dog) result.getPet()).getBreed());
    }

    @Test
    void shouldResolveRuntimeTypeInList() {
      List<Map<String, Object>> docList = new ArrayList<>();

      Map<String, Object> doc1 = new HashMap<>();
      doc1.put("$$type$$", SimpleDocument.class);
      doc1.put("title", "Simple");
      doc1.put("content", "Content1");
      docList.add(doc1);

      Map<String, Object> doc2 = new HashMap<>();
      doc2.put("$$type$$", RichDocument.class);
      doc2.put("title", "Rich");
      doc2.put("content", "Content2");
      doc2.put("author", "Alice");
      docList.add(doc2);

      Map<String, Object> values = new HashMap<>();
      values.put("documents", docList);

      InterfaceListContainerDto result = ObjectBuilder.build(InterfaceListContainerDto.class, values);

      assertEquals(2, result.getDocuments().size());
      assertInstanceOf(SimpleDocument.class, result.getDocuments().get(0));
      assertInstanceOf(RichDocument.class, result.getDocuments().get(1));
      assertEquals("Alice", ((RichDocument) result.getDocuments().get(1)).getAuthor());
    }

    @Test
    void shouldResolveRuntimeTypeInMap() {
      Map<String, Object> doc1 = new HashMap<>();
      doc1.put("$$type$$", SimpleDocument.class);
      doc1.put("title", "Simple");
      doc1.put("content", "Content1");

      Map<String, Object> doc2 = new HashMap<>();
      doc2.put("$$type$$", RichDocument.class);
      doc2.put("title", "Rich");
      doc2.put("content", "Content2");
      doc2.put("author", "Bob");

      Map<String, Object> mapValues = new HashMap<>();
      mapValues.put("simple", doc1);
      mapValues.put("rich", doc2);

      Map<String, Object> values = new HashMap<>();
      values.put("documents", mapValues);

      InterfaceMapContainerDto result = ObjectBuilder.build(InterfaceMapContainerDto.class, values);

      assertInstanceOf(SimpleDocument.class, result.getDocuments().get("simple"));
      assertInstanceOf(RichDocument.class, result.getDocuments().get("rich"));
      assertEquals("Bob", ((RichDocument) result.getDocuments().get("rich")).getAuthor());
    }
  }

  @Nested
  class TypeVariableResolution {

    @Test
    void shouldBuildWithTypeVariableBinding() {
      Map<String, Object> messageValues = new HashMap<>();
      messageValues.put("content", "Hello");
      messageValues.put("citation", "source1");

      Map<String, Object> choiceValues = new HashMap<>();
      choiceValues.put("index", 0);
      choiceValues.put("message", messageValues);

      List<Map<String, Object>> choicesList = new ArrayList<>();
      choicesList.add(choiceValues);

      Map<String, Object> values = new HashMap<>();
      values.put("id", "resp-1");
      values.put("choices", choicesList);

      // CitedResponse extends BaseResponse<CitedMessage>
      CitedResponse result = ObjectBuilder.build(CitedResponse.class, values);

      assertEquals("resp-1", result.getId());
      assertEquals(1, result.getChoices().size());
      assertEquals(0, result.getChoices().get(0).getIndex());
      assertNotNull(result.getChoices().get(0).getMessage());
      assertEquals("Hello", result.getChoices().get(0).getMessage().getContent());
      assertEquals("source1", result.getChoices().get(0).getMessage().getCitation());
    }

    @Test
    void shouldBuildWithExplicitBindings() {
      Map<String, Object> messageValues = new HashMap<>();
      messageValues.put("content", "Test");

      Map<String, Object> values = new HashMap<>();
      values.put("index", 0);
      values.put("message", messageValues);

      // GenericChoice<CitedMessage>로 명시적 바인딩
      GenericChoice<?> result = ObjectBuilder.build(
          GenericChoice.class,
          values,
          Map.of("T", (Type) CitedMessage.class));

      assertEquals(0, result.getIndex());
      assertInstanceOf(CitedMessage.class, result.getMessage());
      assertEquals("Test", ((CitedMessage) result.getMessage()).getContent());
    }
  }

  @Nested
  class DynamicMapBuild {

    @Test
    void shouldPreserveDynamicMapValues() {
      Map<String, Object> argsMap = new HashMap<>();
      argsMap.put("location", "Tokyo");
      argsMap.put("count", 42);
      argsMap.put("verbose", true);

      Map<String, Object> values = new HashMap<>();
      values.put("args", argsMap);

      DynamicMapDto result = ObjectBuilder.build(DynamicMapDto.class, values);

      assertNotNull(result.getArgs());
      assertEquals("Tokyo", result.getArgs().get("location"));
      assertEquals(42, result.getArgs().get("count"));
      assertEquals(true, result.getArgs().get("verbose"));
    }

    @Test
    void shouldPreserveNestedMapInDynamicMap() {
      Map<String, Object> nested = new HashMap<>();
      nested.put("city", "Seoul");

      Map<String, Object> argsMap = new HashMap<>();
      argsMap.put("details", nested);
      argsMap.put("name", "test");

      Map<String, Object> values = new HashMap<>();
      values.put("args", argsMap);

      DynamicMapDto result = ObjectBuilder.build(DynamicMapDto.class, values);

      assertNotNull(result.getArgs());
      assertEquals("test", result.getArgs().get("name"));
      assertInstanceOf(Map.class, result.getArgs().get("details"));
    }
  }

  @Nested
  class DynamicListBuild {

    @Test
    void shouldPreserveDynamicListValues() {
      List<Object> itemsList = new ArrayList<>();
      itemsList.add("hello");
      itemsList.add(42);
      itemsList.add(true);

      Map<String, Object> values = new HashMap<>();
      values.put("items", itemsList);

      DynamicListDto result = ObjectBuilder.build(DynamicListDto.class, values);

      assertNotNull(result.getItems());
      assertEquals(3, result.getItems().size());
      assertEquals("hello", result.getItems().get(0));
      assertEquals(42, result.getItems().get(1));
      assertEquals(true, result.getItems().get(2));
    }
  }

  @Nested
  class NumberTypeConversion {

    @Test
    void shouldConvertLongToInteger() {
      // JSON 파서가 Long으로 반환할 경우
      Map<String, Object> values = new HashMap<>();
      values.put("name", "Test");
      values.put("count", 42L); // Long

      SimpleDto result = ObjectBuilder.build(SimpleDto.class, values);

      assertEquals(42, result.getCount()); // Integer로 변환됨
    }

    @Test
    void shouldConvertDoubleToInteger() {
      Map<String, Object> values = new HashMap<>();
      values.put("name", "Test");
      values.put("count", 42.0); // Double

      SimpleDto result = ObjectBuilder.build(SimpleDto.class, values);

      assertEquals(42, result.getCount());
    }

    @Test
    void shouldConvertIntegerToLong() {
      Map<String, Object> values = new HashMap<>();
      values.put("timestamp", 1000); // Integer

      LongFieldDto result = ObjectBuilder.build(LongFieldDto.class, values);

      assertEquals(1000L, result.getTimestamp());
    }
  }

  // === Test DTOs ===

  @Getter
  @Setter
  static class SimpleDto {
    private String name;
    private Integer count;
  }

  record SimpleRecord(String content, Integer count) {
  }

  record ParentRecord(String id, SimpleRecord child) {
  }

  @Getter
  @Setter
  static class ParentDto {
    private String id;
    private SimpleDto nested;
  }

  @Getter
  @Setter
  static class GrandParentDto {
    private String name;
    private ParentDto child;
  }

  @Getter
  @Setter
  static class IndexedItem {
    @StreamIndex
    private Integer index;
    private String value;
  }

  @Getter
  @Setter
  static class ListContainerDto {
    private List<IndexedItem> items;
  }

  @Getter
  @Setter
  static class TagContainerDto {
    private List<String> tags;
  }

  @Getter
  @Setter
  static class StringArrayDto {
    private String[] tags;
  }

  @Getter
  @Setter
  static class IntArrayDto {
    private int[] numbers;
  }

  @Getter
  @Setter
  static class ObjectArrayDto {
    private IndexedItem[] items;
  }

  @Getter
  @Setter
  static class PrimitiveMapDto {
    private Map<String, String> metadata;
  }

  @Getter
  @Setter
  static class ObjectMapDto {
    private Map<String, SimpleDto> items;
  }

  @Getter
  @Setter
  static class LongFieldDto {
    private Long timestamp;
  }

  // === Interface/Polymorphism Test DTOs ===

  interface IDocument {
    String getTitle();

    String getContent();
  }

  @Getter
  @Setter
  static class SimpleDocument implements IDocument {
    private String title;
    private String content;
  }

  @Getter
  @Setter
  static class RichDocument implements IDocument {
    private String title;
    private String content;
    private String author;
  }

  @Getter
  @Setter
  static class InterfaceContainerDto {
    private String name;
    private IDocument document;
  }

  @Getter
  @Setter
  static class InterfaceListContainerDto {
    private List<IDocument> documents;
  }

  @Getter
  @Setter
  static class InterfaceMapContainerDto {
    private Map<String, IDocument> documents;
  }

  @Getter
  @Setter
  static class Animal {
    private String name;
  }

  @Getter
  @Setter
  static class Dog extends Animal {
    private String breed;
  }

  @Getter
  @Setter
  static class AnimalContainerDto {
    private String owner;
    private Animal pet;
  }

  // === Dynamic Map/List Test DTOs ===

  @Getter
  @Setter
  static class DynamicMapDto {
    private Map<String, Object> args;
  }

  @Getter
  @Setter
  static class DynamicListDto {
    private List<Object> items;
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
}
