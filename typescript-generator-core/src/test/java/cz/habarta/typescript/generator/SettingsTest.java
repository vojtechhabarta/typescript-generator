
package cz.habarta.typescript.generator;

import java.lang.reflect.Modifier;
import org.junit.Assert;
import org.junit.Test;


public class SettingsTest {

    @Test
    public void testParseModifiers() {
        Assert.assertEquals(0, Settings.parseModifiers("", Modifier.fieldModifiers()));
        Assert.assertEquals(Modifier.STATIC, Settings.parseModifiers("static", Modifier.fieldModifiers()));
        Assert.assertEquals(Modifier.STATIC | Modifier.TRANSIENT, Settings.parseModifiers("static | transient", Modifier.fieldModifiers()));
    }

}
