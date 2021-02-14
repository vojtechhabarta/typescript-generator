
package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.util.Utils;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Test;


public class UtilsTest {

    @Test
    public void testReplaceExtension() {
        Assert.assertEquals(new File("test.dir/test.js"), Utils.replaceExtension(new File("test.dir/test"), ".js"));
        Assert.assertEquals(new File("test.dir/test.1.js"), Utils.replaceExtension(new File("test.dir/test.1.ts"), ".js"));
    }

    @Test
    public void testGlobToRegexp() {
        Assert.assertEquals("\\Q\\E.*\\QJson\\E", Utils.globsToRegexps(Arrays.asList("**Json")).get(0).toString());
        Assert.assertEquals("\\Qcz.habarta.test.\\E[^.\\$]*\\Q\\E", Utils.globsToRegexps(Arrays.asList("cz.habarta.test.*")).get(0).toString());
    }

    @Test
    public void testPathJoin() {
        Assert.assertEquals("controller", Utils.joinPath("/controller", null));
        Assert.assertEquals("controller/", Utils.joinPath("/controller/", null));
        Assert.assertEquals("path", Utils.joinPath(null, "/path"));
        Assert.assertEquals("path/", Utils.joinPath(null, "/path/"));
        Assert.assertEquals("", Utils.joinPath(null, "/"));
        Assert.assertEquals("", Utils.joinPath("/", null));
        Assert.assertEquals("path", Utils.joinPath("/", "path"));

        Assert.assertEquals("controller", Utils.joinPath("/controller", ""));
        Assert.assertEquals("controller/", Utils.joinPath("/controller", "/"));
        Assert.assertEquals("controller/path", Utils.joinPath("/controller", "/path"));
        Assert.assertEquals("controller/path", Utils.joinPath("/controller", "path"));
        Assert.assertEquals("controller/path/", Utils.joinPath("/controller", "/path/"));

        Assert.assertEquals("controller/", Utils.joinPath("/controller/", ""));
        Assert.assertEquals("controller/", Utils.joinPath("/controller/", "/"));
        Assert.assertEquals("controller/path", Utils.joinPath("/controller/", "/path"));
        Assert.assertEquals("controller/path", Utils.joinPath("/controller/", "path"));
        Assert.assertEquals("controller/path/", Utils.joinPath("/controller/", "/path/"));
    }

    @Test
    public void testIsPrimitiveType() {
        Assert.assertTrue(Utils.isPrimitiveType(char.class));
        Assert.assertTrue(Utils.isPrimitiveType(byte.class));
        Assert.assertTrue(Utils.isPrimitiveType(short.class));
        Assert.assertTrue(Utils.isPrimitiveType(int.class));
        Assert.assertTrue(Utils.isPrimitiveType(long.class));
        Assert.assertTrue(Utils.isPrimitiveType(float.class));
        Assert.assertTrue(Utils.isPrimitiveType(double.class));
        Assert.assertTrue(Utils.isPrimitiveType(boolean.class));
        Assert.assertFalse(Utils.isPrimitiveType(String.class));
        Assert.assertFalse(Utils.isPrimitiveType(Character.class));
        Assert.assertFalse(Utils.isPrimitiveType(Byte.class));
        Assert.assertFalse(Utils.isPrimitiveType(Short.class));
        Assert.assertFalse(Utils.isPrimitiveType(Integer.class));
        Assert.assertFalse(Utils.isPrimitiveType(Long.class));
        Assert.assertFalse(Utils.isPrimitiveType(Float.class));
        Assert.assertFalse(Utils.isPrimitiveType(Double.class));
        Assert.assertFalse(Utils.isPrimitiveType(Boolean.class));
        Assert.assertFalse(Utils.isPrimitiveType(UUID.class));
        Assert.assertFalse(Utils.isPrimitiveType(Date.class));
        Assert.assertFalse(Utils.isPrimitiveType(Collection.class));
        Assert.assertFalse(Utils.isPrimitiveType(Map.class));
        class NewClass{}
        Assert.assertFalse(Utils.isPrimitiveType(NewClass.class));
    }

}
