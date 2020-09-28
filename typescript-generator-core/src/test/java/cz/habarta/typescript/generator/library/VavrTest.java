
package cz.habarta.typescript.generator.library;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.habarta.typescript.generator.Input;
import cz.habarta.typescript.generator.Jackson2ConfigurationResolved;
import cz.habarta.typescript.generator.Logger;
import cz.habarta.typescript.generator.Settings;
import cz.habarta.typescript.generator.TestUtils;
import cz.habarta.typescript.generator.TypeScriptGenerator;
import cz.habarta.typescript.generator.util.Utils;
import io.vavr.Lazy;
import io.vavr.collection.CharSeq;
import io.vavr.collection.LinkedHashMap;
import io.vavr.collection.LinkedHashMultimap;
import io.vavr.collection.LinkedHashSet;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.collection.Multimap;
import io.vavr.collection.PriorityQueue;
import io.vavr.collection.Set;
import io.vavr.control.Option;
import io.vavr.jackson.datatype.VavrModule;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;


@SuppressWarnings("unused")
public class VavrTest {

    @Test
    public void testVavrInJackson() throws JsonProcessingException {
        final ObjectMapper objectMapper = Utils.getObjectMapper();
        objectMapper.registerModule(new VavrModule());
        final String json = objectMapper.writeValueAsString(new VavrSerializedClasses());
//        System.out.println(json);
    }

    @Test
    public void testVavr() {
        TypeScriptGenerator.setLogger(new Logger(Logger.Level.Verbose));
        final Settings settings = TestUtils.settings();
        settings.jackson2Configuration = new Jackson2ConfigurationResolved();
        settings.additionalDataLibraries = Arrays.asList("vavr");
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(VavrSerializedClasses.class));
        Assert.assertTrue(output.contains("lazy: number"));
        Assert.assertTrue(output.contains("option?: number"));
        Assert.assertTrue(output.contains("charSeq: string"));
        Assert.assertTrue(output.contains("list: string[]"));
        Assert.assertTrue(output.contains("set: string[]"));
        Assert.assertTrue(output.contains("priorityQueue: number[]"));
        Assert.assertTrue(output.contains("map: { [index: string]: number }"));
        Assert.assertTrue(output.contains("multimap: VavrMultimap<number>"));
        Assert.assertTrue(output.contains("multimap2: VavrMultimap<Value>"));
        Assert.assertTrue(output.contains("type VavrMultimap<V>"));
    }

    private static final Key aKey = new Key("a");
    private static final Key bKey = new Key("b");

    private static class VavrSerializedClasses {
        public Lazy<BigDecimal> lazy = Lazy.of(() -> BigDecimal.ONE);
        public Option<BigDecimal> option = Option.some(BigDecimal.ONE);
        public CharSeq charSeq = CharSeq.of("abc");
        public List<String> list = List.of("a", "b");
        public Set<String> set = LinkedHashSet.of("a", "b");
        public PriorityQueue<Integer> priorityQueue = PriorityQueue.of(1, 2, 3);
        public Map<String, BigInteger> map = LinkedHashMap.of("a", BigInteger.ONE, "b", BigInteger.TEN);
        public Multimap<String, Integer> multimap = LinkedHashMultimap.withSeq().of("a", 1, "a", 1, "b", 2);
        public Multimap<Key, Value> multimap2 = LinkedHashMultimap.withSeq().of(aKey, new Value("1"), aKey, new Value("1"), bKey, new Value("2"), bKey, null);
    }

    private static class Key {
        public final String key;

        public Key(String key) {
            this.key = key;
        }

        @Override
        public String toString() {
            return key;
        }

    }

    private static class Value {
        public final String value;

        public Value(String value) {
            this.value = value;
        }
    }

}
