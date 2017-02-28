
package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.util.Utils;
import java.io.File;
import org.junit.Assert;
import org.junit.Test;


public class UtilsTest {

    @Test
    public void testReplaceExtension() {
        Assert.assertEquals(new File("test.dir/test.js"), Utils.replaceExtension(new File("test.dir/test"), ".js"));
        Assert.assertEquals(new File("test.dir/test.1.js"), Utils.replaceExtension(new File("test.dir/test.1.ts"), ".js"));
    }

}
