package org.dataone.integration.webTest;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * An annotation used to hold the human readable test name
 * @author rnahf
 *
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface WebTestName {

    String value();
}
