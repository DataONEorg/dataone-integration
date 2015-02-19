package org.dataone.integration.webTest;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Used to annotate the field that holds the test implementations 
 * from where to get the WebTestName and WebTestDescription annotations
 * @author rnahf
 *
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface WebTestImplementation {

}
