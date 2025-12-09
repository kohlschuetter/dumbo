package com.kohlschutter.dumbo.jacline;

import com.kohlschutter.dumbo.annotations.JavaScriptResource;
import com.kohlschutter.dumbo.api.DumboApplication;

/**
 * Any Jacline-based Dumbo application must implement this interface.
 * 
 * @author Christian Kohlschütter
 */
@JavaScriptResource({"js/jacline-generated.js"}) // see pom.xml
public interface JaclineDumboApplication extends DumboApplication {

}
