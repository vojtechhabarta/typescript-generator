
package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.util.Utils;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class UtilsTest {

    @Test
    public void testReplaceExtension() {
        Assertions.assertEquals(new File("test.dir/test.js"), Utils.replaceExtension(new File("test.dir/test"), ".js"));
        Assertions.assertEquals(new File("test.dir/test.1.js"), Utils.replaceExtension(new File("test.dir/test.1.ts"), ".js"));
    }

    @Test
    public void testGlobToRegexp() {
        Assertions.assertEquals("\\Q\\E.*\\QJson\\E", Utils.globsToRegexps(Arrays.asList("**Json")).get(0).toString());
        Assertions.assertEquals("\\Qcz.habarta.test.\\E[^.\\$]*\\Q\\E", Utils.globsToRegexps(Arrays.asList("cz.habarta.test.*")).get(0).toString());
    }

    @Test
    public void testPathJoin() {
        Assertions.assertEquals("controller", Utils.joinPath("/controller", null));
        Assertions.assertEquals("controller/", Utils.joinPath("/controller/", null));
        Assertions.assertEquals("path", Utils.joinPath(null, "/path"));
        Assertions.assertEquals("path/", Utils.joinPath(null, "/path/"));
        Assertions.assertEquals("", Utils.joinPath(null, "/"));
        Assertions.assertEquals("", Utils.joinPath("/", null));
        Assertions.assertEquals("path", Utils.joinPath("/", "path"));

        Assertions.assertEquals("controller", Utils.joinPath("/controller", ""));
        Assertions.assertEquals("controller/", Utils.joinPath("/controller", "/"));
        Assertions.assertEquals("controller/path", Utils.joinPath("/controller", "/path"));
        Assertions.assertEquals("controller/path", Utils.joinPath("/controller", "path"));
        Assertions.assertEquals("controller/path/", Utils.joinPath("/controller", "/path/"));

        Assertions.assertEquals("controller/", Utils.joinPath("/controller/", ""));
        Assertions.assertEquals("controller/", Utils.joinPath("/controller/", "/"));
        Assertions.assertEquals("controller/path", Utils.joinPath("/controller/", "/path"));
        Assertions.assertEquals("controller/path", Utils.joinPath("/controller/", "path"));
        Assertions.assertEquals("controller/path/", Utils.joinPath("/controller/", "/path/"));
    }

    @Test
    public void testIsPrimitiveType() {
        Assertions.assertTrue(Utils.isPrimitiveType(char.class));
        Assertions.assertTrue(Utils.isPrimitiveType(byte.class));
        Assertions.assertTrue(Utils.isPrimitiveType(short.class));
        Assertions.assertTrue(Utils.isPrimitiveType(int.class));
        Assertions.assertTrue(Utils.isPrimitiveType(long.class));
        Assertions.assertTrue(Utils.isPrimitiveType(float.class));
        Assertions.assertTrue(Utils.isPrimitiveType(double.class));
        Assertions.assertTrue(Utils.isPrimitiveType(boolean.class));
        Assertions.assertFalse(Utils.isPrimitiveType(String.class));
        Assertions.assertFalse(Utils.isPrimitiveType(Character.class));
        Assertions.assertFalse(Utils.isPrimitiveType(Byte.class));
        Assertions.assertFalse(Utils.isPrimitiveType(Short.class));
        Assertions.assertFalse(Utils.isPrimitiveType(Integer.class));
        Assertions.assertFalse(Utils.isPrimitiveType(Long.class));
        Assertions.assertFalse(Utils.isPrimitiveType(Float.class));
        Assertions.assertFalse(Utils.isPrimitiveType(Double.class));
        Assertions.assertFalse(Utils.isPrimitiveType(Boolean.class));
        Assertions.assertFalse(Utils.isPrimitiveType(UUID.class));
        Assertions.assertFalse(Utils.isPrimitiveType(Date.class));
        Assertions.assertFalse(Utils.isPrimitiveType(Collection.class));
        Assertions.assertFalse(Utils.isPrimitiveType(Map.class));
        class NewClass{}
        Assertions.assertFalse(Utils.isPrimitiveType(NewClass.class));
    }

}
