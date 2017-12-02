
package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.util.Utils;
import java.io.File;
import java.util.*;
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

}
