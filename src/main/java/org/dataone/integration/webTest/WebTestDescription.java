package org.dataone.integration.webTest;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * An annotation used to hold an end-user oriented description of the test
 * @author rnahf
 *
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface WebTestDescription {

    String value();
}
