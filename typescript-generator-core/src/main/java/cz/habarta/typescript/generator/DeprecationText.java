
package cz.habarta.typescript.generator;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


@Retention(RetentionPolicy.RUNTIME)
public @interface DeprecationText {
    String value();
}
