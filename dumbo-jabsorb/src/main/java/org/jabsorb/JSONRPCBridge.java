/*
 * jabsorb - a Java to JavaScript Advanced Object Request Broker
 * http://www.jabsorb.org
 *
 * Copyright 2007-2009 The jabsorb team
 *
 * based on original code from
 * JSON-RPC-Java - a JSON-RPC to Java Bridge with dynamic invocation
 *
 * Copyright Metaparadigm Pte. Ltd. 2004.
 * Michael Clark <michael@metaparadigm.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jabsorb;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.lang.reflect.AccessibleObject;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.jabsorb.callback.CallbackController;
import org.jabsorb.callback.InvocationCallback;
import org.jabsorb.localarg.LocalArgController;
import org.jabsorb.localarg.LocalArgResolver;
import org.jabsorb.reflect.AccessibleObjectKey;
import org.jabsorb.reflect.ClassAnalyzer;
import org.jabsorb.reflect.ClassData;
import org.jabsorb.serializer.AccessibleObjectResolver;
import org.jabsorb.serializer.Serializer;
import org.jabsorb.serializer.SerializerState;
import org.jabsorb.serializer.impl.ReferenceSerializer;
import org.jabsorb.serializer.request.RequestParser;
import org.jabsorb.serializer.request.fixups.FixupsCircularReferenceHandler;
import org.jabsorb.serializer.response.fixups.FixupCircRefAndNonPrimitiveDupes;
import org.jabsorb.serializer.response.results.FailedResult;
import org.jabsorb.serializer.response.results.JSONRPCResult;
import org.jabsorb.serializer.response.results.SuccessfulResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * This class implements a bridge that unmarshalls JSON objects in JSON-RPC request format, invokes
 * a method on the exported object, and then marshalls the resulting Java objects to JSON objects in
 * JSON-RPC result format.
 * </p>
 * <p>
 * There is a global bridge singleton object that allows exporting classes and objects to all HTTP
 * clients. In addition to this, an instance of the JSONRPCBridge can optionally be placed in a
 * users' HttpSession object registered under the attribute "JSONRPCBridge" to allow exporting of
 * classes and objects to specific users. A session specific bridge will delegate requests for
 * objects it does not know about to the global singleton JSONRPCBridge instance.
 * </p>
 * <p>
 * Using session specific bridge instances can improve the security of applications by allowing
 * exporting of certain objects only to specific HttpSessions as well as providing a convenient
 * mechanism for JavaScript clients to access stateful data associated with the current user.
 * </p>
 * <p>
 * You can create a HttpSession specific bridge in JSP with the usebean tag:
 * </p>
 * <code>&lt;jsp:useBean id="JSONRPCBridge" scope="session"
 * class="org.jabsorb.JSONRPCBridge" /&gt;</code>
 * <p>
 * Then export an object for your JSON-RPC client to call methods on:
 * </p>
 * <code>JSONRPCBridge.registerObject("test", testObject);</code>
 * <p>
 * This will make available all public methods of the object as
 * <code>test.&lt;methodnames&gt;</code> to JSON-RPC clients. This approach should generally be
 * performed after an authentication check to only export objects to clients that are authorised to
 * use them.
 * </p>
 * <p>
 * Alternatively, the global bridge singleton object allows exporting of classes and objects to all
 * HTTP clients. It can be fetched with <code>JSONRPCBridge.getGlobalBridge()</code>.
 * </p>
 * <p>
 * To export all public instance methods of an object to <b>all</b> clients:
 * </p>
 * <code>JSONRPCBridge.getGlobalBridge().registerObject("myObject",
 * myObject);</code>
 * <p>
 * To export all public static methods of a class to <b>all</b> clients:
 * </p>
 * <code>JSONRPCBridge.getGlobalBridge().registerClass("MyClass",
 * com.example.MyClass.class);</code>
 */
public class JSONRPCBridge implements Serializable {
  /**
   * Container for objects of which instances have been made
   */
  protected static class ObjectInstance implements Serializable {
    /**
     * Unique serialisation id.
     */
    private final static long serialVersionUID = 2;

    /**
     * The class the object is of
     */
    private final Class<?> clazz;

    /**
     * The object for the instance
     */
    private final Object object;

    /**
     * Creates a new ObjectInstance
     * 
     * @param object The object for the instance
     */
    public ObjectInstance(Object object) {
      this.object = object;
      this.clazz = object.getClass();
    }

    /**
     * Creates a new ObjectInstance
     * 
     * @param object The object for the instance
     * @param clazz The class the object is of
     */
    public ObjectInstance(Object object, Class<?> clazz) {
      if (!clazz.isInstance(object)) {
        throw new ClassCastException("Attempt to register jsonrpc object with invalid class.");
      }
      this.object = object;
      this.clazz = clazz;
    }

    /**
     * Gets the class the object is of
     * 
     * @return The class the object is of
     */
    public Class<?> getClazz() {
      return clazz;
    }

    /**
     * Gets the object for the instance
     * 
     * @return the object for the instance
     */
    public Object getObject() {
      return object;
    }
  }

  /**
   * The prefix for callable references, as sent in messages
   */
  public static final String CALLABLE_REFERENCE_METHOD_PREFIX = ".ref";

  /**
   * The string identifying constuctor calls
   */
  public static final String CONSTRUCTOR_FLAG = "$constructor";

  /**
   * The default location of the properties file which is looked for in the constructor
   */
  public static final String DEFAULT_PROPERTIES_LOCATION = "jabsorb.properties";

  /**
   * The logger for this class
   */
  public final static Logger log;

  /**
   * The prefix for objects, as sent in messages
   */
  public static final String OBJECT_METHOD_PREFIX = ".obj";

  /**
   * The key in the properties file in which the request parser class should be put
   */
  public static final String REQUEST_PARSER_KEY = "RequestParser";

  /**
   * The key in the properties file in which the serializer state class should be put
   */
  public static final String SERIALIZER_STATE_CLASS_KEY = "SerializerStateClass";

  /**
   * The default file which should contain a list of serializers to load
   */
  public static final String SERIALIZERS_FILE = "serializers.txt";

  /**
   * Global bridge (for exporting to all users)
   */
  private final static JSONRPCBridge globalBridge;

  /**
   * A simple transformer that makes no change
   */
  private static final ExceptionTransformer IDENTITY_EXCEPTION_TRANSFORMER =
      new ExceptionTransformer() {
        /**
         * Unique serialisation id.
         */
        private final static long serialVersionUID = 2;

        @Override
        public Object transform(Throwable t) {
          return t;
        }
      };

  /**
   * Unique serialisation id.
   */
  private final static long serialVersionUID = 2;

  static {
    log = LoggerFactory.getLogger(JSONRPCBridge.class);
    globalBridge = new JSONRPCBridge();
  }

  /**
   * This method retrieves the global bridge singleton.
   * <p/>
   * It should be used with care as objects should generally be registered within session specific
   * bridges for security reasons.
   * 
   * @return returns the global bridge object.
   */
  public static JSONRPCBridge getGlobalBridge() {
    return globalBridge;
  }

  /**
   * Registers a Class to be removed from the exported method signatures and instead be resolved
   * locally using context information from the transport.
   * 
   * @param argClazz The class to be resolved locally
   * @param argResolver The user defined class that resolves the and returns the method argument
   *          using transport context information
   * @param contextInterface The type of transport Context object the callback is interested in eg.
   *          HttpServletRequest.class for the servlet transport
   */
  public static void registerLocalArgResolver(Class<?> argClazz, Class<?> contextInterface,
      LocalArgResolver argResolver) {
    LocalArgController.registerLocalArgResolver(argClazz, contextInterface, argResolver);
  }

  /**
   * Unregisters a LocalArgResolver</b>.
   * 
   * @param argClazz The previously registered local class
   * @param argResolver The previously registered LocalArgResolver object
   * @param contextInterface The previously registered transport Context interface.
   */
  public static void unregisterLocalArgResolver(Class<?> argClazz, Class<?> contextInterface,
      LocalArgResolver argResolver) {
    LocalArgController.unregisterLocalArgResolver(argClazz, contextInterface, argResolver);
  }

  /**
   * Create unique method names by appending the given prefix to the keys from the given HashMap and
   * adding them all to the given HashSet.
   * 
   * @param m HashSet to add unique methods to.
   * @param prefix prefix to append to each method name found in the methodMap.
   * @param methodMap a HashMap containing MethodKey keys specifying methods.
   */
  protected static void uniqueMethods(Set<String> m, String prefix,
      Map<AccessibleObjectKey, Set<AccessibleObject>> methodMap) {
    for (Map.Entry<AccessibleObjectKey, Set<AccessibleObject>> mentry : methodMap.entrySet()) {
      AccessibleObjectKey mk = mentry.getKey();
      m.add(prefix + mk.getMethodName());
    }
  }

  /**
   * Gets the request parser for the constructor
   * 
   * @param properties The properties file which should have a key <REQUEST_PARSER_KEY>
   * @return A request parser as described by the properties file or the
   *         FixupsCircularReferenceHandler if nothing is found
   */
  private static RequestParser getInitRequestParser(Properties properties) {
    final String className = properties.getProperty(REQUEST_PARSER_KEY);
    if (className != null) {
      try {
        final Class<?> clazz = Class.forName(className);
        Class<?> superClass = clazz.getSuperclass();
        while (superClass != null) {
          if (superClass.equals(RequestParser.class)) {
            return (RequestParser) clazz.newInstance();
          }
          superClass = superClass.getSuperclass();
        }
      } catch (Exception e) {
        e.printStackTrace();
        // let it fall through to the return
      }
    }
    return new FixupsCircularReferenceHandler();
  }

  /**
   * Loads serializer objects from a file.
   * 
   * @param filename The name of a file which has a list of serializer classes to load, through the
   *          Class.forName() mechanism. One class name should occur per line
   * @return A list of serializers to be loaded at the construction of the bridge.
   */
  private static List<Serializer> getInitSerializers(final String filename) {
    if (filename != null) {
      final File serializersFile = new File(filename);
      try {
        if (serializersFile.exists()) {
          try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(
              serializersFile), StandardCharsets.UTF_8))) {
            String line;
            final List<Serializer> serializers = new ArrayList<Serializer>();
            while ((line = reader.readLine()) != null) {
              log.info("Creating Serializer: " + line);
              serializers.add((Serializer) Class.forName(line).newInstance());
            }
            return serializers;
          }
        }
      } catch (Exception e) {
        // fall through and return default serializers
      }
    }
    return JSONSerializer.getDefaultSerializers();
  }

  /**
   * Gets the serializer state for the constructor
   * 
   * @param properties The properties file which should have a key <SERIALIZER_STATE_CLASS_KEY>
   * @return A serializer state class as described by the properties file or the
   *         FixupCircRefAndNonPrimitiveDupes.class if nothing is found
   */
  @SuppressWarnings("unchecked")
  private static Class<? extends SerializerState> getInitSerializerStateClass(
      Properties properties) {
    final String className = properties.getProperty(SERIALIZER_STATE_CLASS_KEY);
    if (className != null) {
      try {
        final Class<?> clazz = Class.forName(className);
        Class<?> interfaces[] = clazz.getInterfaces();
        for (Class<?> c : interfaces) {
          if (c.equals(SerializerState.class)) {
            return (Class<? extends SerializerState>) clazz;
          }
        }
      } catch (Exception e) {
        // let it fall through to the return
      }
    }
    return FixupCircRefAndNonPrimitiveDupes.class;
  }

  /**
   * Creates and loads a java.util.Properties from a file.
   * 
   * @param propertiesFilename The name of the file to load the properties from.
   * @return An initialised properties.
   */
  private static Properties loadProperties(final String propertiesFilename) {
    final Properties p = new Properties();
    try {
      p.load(new BufferedInputStream(new FileInputStream(propertiesFilename)));
    } catch (IOException e) {
      // If we have an exception here we don't want to throw it up to the
      // constructors, so just swallow it.
    }
    return p;
  }

  /**
   * Whether references will be used on the bridge
   */
  protected boolean referencesEnabled;

  /**
   * key clazz, classes that should be returned as CallableReferences
   */
  private final Set<Class<?>> callableReferenceSet;

  /**
   * The callback controller
   */
  private CallbackController cbc = null;

  /**
   * key "exported class name", val Class
   */
  private final Map<String, Class<?>> classMap;

  /**
   * The functor used to convert exceptions
   */
  private ExceptionTransformer exceptionTransformer = IDENTITY_EXCEPTION_TRANSFORMER;

  /**
   * key "exported instance name", val ObjectInstance
   */
  private final Map<Object, ObjectInstance> objectMap;

  /**
   * key Integer hashcode, object held as reference
   */
  private final Map<Integer, Object> referenceMap;

  /**
   * ReferenceSerializer if enabled
   */
  private final Serializer referenceSerializer;

  /**
   * key clazz, classes that should be returned as References
   */
  private final Set<Class<?>> referenceSet;

  /**
   * Local JSONSerializer instance
   */
  private JSONSerializer ser;

  /**
   * Creates a new bridge.
   */
  public JSONRPCBridge() {
    this(DEFAULT_PROPERTIES_LOCATION);
  }

  /**
   * Creates a new bridge.
   * 
   * @param serializers The serializers to load on this bridge.
   * @param requestParser The request parser to use
   * @param serializerStateClass The serializer state to use
   */
  public JSONRPCBridge(final List<Serializer> serializers, final RequestParser requestParser,
      final Class<? extends SerializerState> serializerStateClass) {
    {
      if (serializerStateClass == null) {
        JSONRPCBridge.log.info("Using default serializer state");
      } else {
        JSONRPCBridge.log.info("Using serializer state: " + serializerStateClass
            .getCanonicalName());
      }
      if (requestParser == null) {
        JSONRPCBridge.log.info("Using default request parser");
      } else {
        JSONRPCBridge.log.info("Using request parser: " + requestParser.getClass()
            .getCanonicalName());
      }
    }
    ser = new JSONSerializer(serializerStateClass, requestParser);

    referenceSerializer = new ReferenceSerializer(this);
    try {
      for (Serializer s : serializers) {
        if (s.getClass().equals(ReferenceSerializer.class)) {
          ser.registerSerializer(this.referenceSerializer);
        } else {
          ser.registerSerializer(s);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    classMap = new HashMap<String, Class<?>>();
    objectMap = new HashMap<Object, ObjectInstance>();
    referenceMap = new HashMap<Integer, Object>();
    referenceSet = new HashSet<Class<?>>();
    callableReferenceSet = new HashSet<Class<?>>();
    referencesEnabled = false;
  }

  /**
   * Creates a new bridge.
   * 
   * @param properties Contains properties to initialize the bridge with.
   */
  public JSONRPCBridge(final Properties properties) {
    this(getInitSerializers(properties.getProperty(SERIALIZERS_FILE)), getInitRequestParser(
        properties), getInitSerializerStateClass(properties));
  }

  /**
   * Creates a new bridge.
   * 
   * @param propertiesFilename The name of a file which has a properties to load, through the
   *          Properties.load() mechanism.
   */
  public JSONRPCBridge(final String propertiesFilename) {
    this(loadProperties(propertiesFilename));
  }

  /**
   * Adds a reference to the map of known references
   * 
   * @param o The object to be added
   */
  public void addReference(Object o) {
    synchronized (referenceMap) {
      referenceMap.put(new Integer(System.identityHashCode(o)), o);
    }
  }

  /**
   * Call a method using a JSON-RPC request object.
   * 
   * @param context The transport context (the HttpServletRequest and HttpServletResponse objects in
   *          the case of the HTTP transport).
   * @param jsonReq The JSON-RPC request structured as a JSON object tree.
   * @return a JSONRPCResult object with the result of the invocation or an error.
   */
  public JSONRPCResult call(Object context[], JSONObject jsonReq) {
    // #1: Parse the request
    final String encodedMethod;
    final Object requestId;
    final JSONArray arguments;
    JSONRPCResult r;
    try {
      encodedMethod = jsonReq.getString(JSONSerializer.METHOD_FIELD);
      requestId = jsonReq.opt(JSONSerializer.ID_FIELD);
      arguments = this.ser.getRequestParser().unmarshallArray(jsonReq,
          JSONSerializer.PARAMETER_FIELD);
      if (log.isDebugEnabled()) {
        log.debug("call " + encodedMethod + "(" + arguments + ")" + ", requestId=" + requestId);
      }
      // #2: Get the name of the class and method from the encodedMethod
      final String className;
      final String methodName;
      {
        StringTokenizer t = new StringTokenizer(encodedMethod, ".");
        if (t.hasMoreElements()) {
          className = t.nextToken();
        } else {
          className = null;
        }
        if (t.hasMoreElements()) {
          methodName = t.nextToken();
        } else {
          methodName = null;
        }
      }
      // #3: Get the id of the object (if it exists) from the className
      // (in the format: ".obj#<objectID>")
      final int objectID;
      {
        final int objectStartIndex = encodedMethod.indexOf('[');
        final int objectEndIndex = encodedMethod.indexOf(']');
        if (encodedMethod.startsWith(OBJECT_METHOD_PREFIX) && (objectStartIndex != -1)
            && (objectEndIndex != -1) && (objectStartIndex < objectEndIndex)) {
          objectID = Integer.parseInt(encodedMethod.substring(objectStartIndex + 1,
              objectEndIndex));
        } else {
          objectID = 0;
        }
      }
      // #4: Handle list method calls
      if ((objectID == 0) && (encodedMethod.equals("system.listMethods"))) {
        r = new SuccessfulResult(requestId, getSystemMethods());
      } else {
        // #5: Get the object to act upon and the possible method that could be
        // called on it
        final Map<AccessibleObjectKey, Set<AccessibleObject>> methodMap;
        final Object javascriptObject;
        final AccessibleObject ao;
        try {
          javascriptObject = getObjectContext(objectID, className);
          methodMap = getAccessibleObjectMap(objectID, className, methodName);
          // #6: Resolve the method
          ao = AccessibleObjectResolver.resolveMethod(methodMap, methodName, arguments, ser);
          if (ao == null) {
            throw new NoSuchMethodException(FailedResult.MSG_ERR_NOMETHOD);
          }
          // #7: Call the method
          r = AccessibleObjectResolver.invokeAccessibleObject(ao, context, arguments,
              javascriptObject, requestId, ser, cbc, exceptionTransformer);
        } catch (NoSuchMethodException e) {
          if (e.getMessage().equals(FailedResult.MSG_ERR_NOCONSTRUCTOR)) {
            r = new FailedResult(FailedResult.CODE_ERR_NOCONSTRUCTOR, requestId,
                FailedResult.MSG_ERR_NOCONSTRUCTOR);
          } else {
            r = new FailedResult(FailedResult.CODE_ERR_NOMETHOD, requestId,
                FailedResult.MSG_ERR_NOMETHOD);
          }
        }
      }
    } catch (JSONException e) {
      log.error("no method or parameters in request");
      r = new FailedResult(FailedResult.CODE_ERR_NOMETHOD, null, FailedResult.MSG_ERR_NOMETHOD);
    }

    return r;
  }

  /**
   * Allows references to be used on the bridge
   * 
   * @throws Exception If a serialiser has already been registered for CallableReferences
   */
  public synchronized void enableReferences() throws Exception {
    if (!referencesEnabled) {
      registerSerializer(referenceSerializer);
      referencesEnabled = true;
      log.info("enabled references on this bridge");
    }
  }

  /**
   * Get the CallbackController object associated with this bridge.
   * 
   * @return the CallbackController object associated with this bridge.
   */
  public CallbackController getCallbackController() {
    return cbc;
  }

  /**
   * Gets a known reference
   * 
   * @param objectId The id of the object to get
   * @return The requested reference
   */
  public Object getReference(int objectId) {
    synchronized (referenceMap) {
      return referenceMap.get(new Integer(objectId));
    }
  }

  /**
   * Get the global JSONSerializer object.
   * 
   * @return the global JSONSerializer object.
   */
  public JSONSerializer getSerializer() {
    return ser;
  }

  /**
   * Check whether a class is registered as a callable reference type.
   * 
   * @param clazz The class object to check is a callable reference.
   * @return true if it is, false otherwise
   */
  public boolean isCallableReference(Class<?> clazz) {
    if (this == globalBridge) {
      return false;
    }
    if (!referencesEnabled) {
      return false;
    }
    if (callableReferenceSet.contains(clazz)) {
      return true;
    }

    // check if the class implements any interface that is
    // registered as a callable reference...
    Class<?>[] interfaces = clazz.getInterfaces();
    for (int i = 0; i < interfaces.length; i++) {
      if (callableReferenceSet.contains(interfaces[i])) {
        return true;
      }
    }

    // check super classes as well...
    Class<?> superClass = clazz.getSuperclass();
    while (superClass != null) {
      if (callableReferenceSet.contains(superClass)) {
        return true;
      }
      superClass = superClass.getSuperclass();
    }

    // should interfaces of each superclass be checked too???
    // not sure...

    return globalBridge.isCallableReference(clazz);
  }

  /**
   * Check whether a class is registered as a reference type.
   * 
   * @param clazz The class object to check is a reference.
   * @return true if it is, false otherwise.
   */
  public boolean isReference(Class<?> clazz) {
    if (this == globalBridge) {
      return false;
    }
    if (!referencesEnabled) {
      return false;
    }
    if (referenceSet.contains(clazz)) {
      return true;
    }
    return globalBridge.isReference(clazz);
  }

  /**
   * Lookup a class that is registered with this bridge.
   * 
   * @param name The registered name of the class to lookup.
   * @return the class for the name
   */
  public Class<?> lookupClass(String name) {
    synchronized (classMap) {
      return classMap.get(name);
    }
  }

  /**
   * Lookup an object that is registered with this bridge.
   * 
   * @param key The registered name of the object to lookup.
   * @return The object desired if it exists, else null.
   */
  public Object lookupObject(Object key) {
    synchronized (objectMap) {
      ObjectInstance oi = objectMap.get(key);
      if (oi != null) {
        return oi.getObject();
      }
    }
    return null;
  }

  /**
   * <p>
   * Registers a class to be returned as a callable reference.
   * </p>
   * <p>
   * The JSONBridge will return a callable reference to the JSON-RPC client for registered classes
   * instead of passing them by value. The JSONBridge will take a references to these objects and
   * the JSON-RPC client will create an invocation proxy for objects of this class for which methods
   * will be called on the instance on the server.
   * </p>
   * <p>
   * <p>
   * Note that the global bridge does not support registering of callable references and attempting
   * to do so will throw an Exception. These operations are inherently session based and are
   * disabled on the global bridge because there is currently no safe simple way to garbage collect
   * such references across the JavaScript/Java barrier.
   * </p>
   * <p>
   * A Callable Reference in JSON format looks like this:
   * </p>
   * <code>{ "javaClass":"org.jabsorb.test.Bar",<br />
   * "objectID":4827452,<br /> "JSONRPCType":"CallableReference" }</code>
   * 
   * @param clazz The class object that should be marshalled as a callable reference.
   * @throws Exception if this method is called on the global bridge.
   */
  public void registerCallableReference(Class<?> clazz) throws Exception {
    if (this == globalBridge) {
      throw new Exception("Can't register callable reference on global bridge");
    }
    if (!referencesEnabled) {
      enableReferences();
    }
    synchronized (callableReferenceSet) {
      callableReferenceSet.add(clazz);
    }
    ser.registerCallableReference(clazz);
    if (log.isDebugEnabled()) {
      log.debug("registered callable reference " + clazz.getName());
    }
  }

  /**
   * Registers a callback to be called before and after method invocation
   * 
   * @param callback The object implementing the InvocationCallback Interface
   * @param contextInterface The type of transport Context interface the callback is interested in
   *          eg. HttpServletRequest.class for the servlet transport.
   */
  public void registerCallback(InvocationCallback callback, Class<?> contextInterface) {
    if (cbc == null) {
      cbc = new CallbackController();
    }
    cbc.registerCallback(callback, contextInterface);
  }

  /**
   * Registers a class to export static methods.
   * <p/>
   * The JSONBridge will export all static methods of the class. This is useful for exporting
   * factory classes that may then return CallableReferences to the JSON-RPC client.
   * <p/>
   * Calling registerClass for a clazz again under the same name will have no effect.
   * <p/>
   * To export instance methods you need to use registerObject.
   * 
   * @param name The name to register the class with.
   * @param clazz The class to export static methods from.
   * @throws Exception If a class is already registed with this name
   */
  public void registerClass(String name, Class<?> clazz) throws Exception {
    synchronized (classMap) {
      Class<?> exists = classMap.get(name);
      if (exists != null && exists != clazz) {
        throw new Exception("different class registered as " + name);
      }
      if (exists == null) {
        classMap.put(name, clazz);
      }
    }
    if (log.isDebugEnabled()) {
      log.debug("registered class " + clazz.getName() + " as " + name);
    }
  }

  /**
   * Registers an object to export all instance methods and static methods.
   * <p/>
   * The JSONBridge will export all instance methods and static methods of the particular object
   * under the name passed in as a key.
   * <p/>
   * This will make available all methods of the object as
   * <code>&lt;key&gt;.&lt;methodnames&gt;</code> to JSON-RPC clients.
   * <p/>
   * Calling registerObject for a name that already exists will replace the existing entry.
   * 
   * @param key The named prefix to export the object as
   * @param o The object instance to be called upon
   */
  public void registerObject(Object key, Object o) {
    ObjectInstance oi = new ObjectInstance(o);
    synchronized (objectMap) {
      objectMap.put(key, oi);
    }
    if (log.isDebugEnabled()) {
      log.debug("registered object " + o.hashCode() + " of class " + o.getClass().getName() + " as "
          + key);
    }
  }

  /**
   * Registers an object to export all instance methods defined by interfaceClass.
   * <p/>
   * The JSONBridge will export all instance methods defined by interfaceClass of the particular
   * object under the name passed in as a key.
   * <p/>
   * This will make available these methods of the object as
   * <code>&lt;key&gt;.&lt;methodnames&gt;</code> to JSON-RPC clients.
   * 
   * @param key The named prefix to export the object as
   * @param o The object instance to be called upon
   * @param interfaceClass The type that this object should be registered as.
   *          <p/>
   *          This can be used to restrict the exported methods to the methods defined in a specific
   *          superclass or interface.
   */
  public void registerObject(String key, Object o, Class<?> interfaceClass) {
    ObjectInstance oi = new ObjectInstance(o, interfaceClass);
    synchronized (objectMap) {
      objectMap.put(key, oi);
    }
    if (log.isDebugEnabled()) {
      log.debug("registered object " + o.hashCode() + " of class " + interfaceClass.getName()
          + " as " + key);
    }
  }

  /**
   * Registers a class to be returned by reference and not by value as is done by default.
   * <p/>
   * The JSONBridge will take a references to these objects and return an opaque object to the
   * JSON-RPC client. When the opaque object is passed back through the bridge in subsequent calls,
   * the original object is substitued in calls to Java methods. This should be used for any objects
   * that contain security information or complex types that are not required in the Javascript
   * client but need to be passed as a reference in methods of exported objects.
   * <p/>
   * A Reference in JSON format looks like this:
   * <p/>
   * <code>{ "javaClass":"org.jabsorb.test.Foo",<br />
   * "objectID":5535614,<br /> "JSONRPCType":"Reference" }</code>
   * <p>
   * Note that the global bridge does not support registering of references and attempting to do so
   * will throw an Exception. These operations are inherently session based and are disabled on the
   * global bridge because there is currently no safe simple way to garbage collect such references
   * across the JavaScript/Java barrier.
   * </p>
   * 
   * @param clazz The class object that should be marshalled as a reference.
   * @throws Exception if this method is called on the global bridge.
   */
  public void registerReference(Class<?> clazz) throws Exception {
    if (this == globalBridge) {
      throw new Exception("Can't register reference on global bridge");
    }
    if (!referencesEnabled) {
      enableReferences();
    }
    synchronized (referenceSet) {
      referenceSet.add(clazz);
    }
    if (log.isDebugEnabled()) {
      log.debug("registered reference " + clazz.getName());
    }
  }

  /**
   * Register a new serializer on this bridge.
   * 
   * @param serializer A class implementing the Serializer interface (usually derived from
   *          AbstractSerializer).
   * @throws Exception If a serialiser has already been registered that serialises the same class
   */
  public void registerSerializer(Serializer serializer) throws Exception {
    ser.registerSerializer(serializer);
  }

  /**
   * Set the CallbackController object for this bridge.
   * 
   * @param cbc the CallbackController object to be set for this bridge.
   */
  public void setCallbackController(CallbackController cbc) {
    this.cbc = cbc;
  }

  /**
   * Sets the exception transformer for the bridge.
   * 
   * @param exceptionTransformer The new exception transformer to use.
   */
  public void setExceptionTransformer(ExceptionTransformer exceptionTransformer) {
    this.exceptionTransformer = exceptionTransformer;
  }

  /**
   * Allow serializer state class to be set after construction. This is necessary for beans.
   * 
   * @param serializerStateClass The serializer state class to use.
   */
  public void setSerializerStateClass(Class<? extends SerializerState> serializerStateClass) {
    this.ser.setSerializerStateClass(serializerStateClass);
  }

  /**
   * Unregisters a callback
   * 
   * @param callback The previously registered InvocationCallback object
   * @param contextInterface The previously registered transport Context interface.
   */
  public void unregisterCallback(InvocationCallback callback, Class<?> contextInterface) {
    if (cbc == null) {
      return;
    }
    cbc.unregisterCallback(callback, contextInterface);
  }

  /**
   * Unregisters a class exported with registerClass.
   * <p/>
   * The JSONBridge will unexport all static methods of the class.
   * 
   * @param name The registered name of the class to unexport static methods from.
   */
  public void unregisterClass(String name) {
    synchronized (classMap) {
      Class<?> clazz = classMap.get(name);
      if (clazz != null) {
        classMap.remove(name);
        if (log.isDebugEnabled()) {
          log.debug("unregistered class " + clazz.getName() + " from " + name);
        }
      }
    }
  }

  /**
   * Unregisters an object exported with registerObject.
   * <p/>
   * The JSONBridge will unexport all instance methods and static methods of the particular object
   * under the name passed in as a key.
   * 
   * @param key The named prefix of the object to unexport
   */
  public void unregisterObject(Object key) {
    synchronized (objectMap) {
      ObjectInstance oi = objectMap.get(key);
      if (oi.getObject() != null) {
        objectMap.remove(key);
        if (log.isDebugEnabled()) {
          log.debug("unregistered object " + oi.getObject().hashCode() + " of class " + oi
              .getClazz().getName() + " from " + key);
        }
      }
    }
  }

  /**
   * Get list of system methods that can be invoked on this JSONRPCBridge.
   *
   * These are the methods that are retrieved via a system.listMethods call from the client (like
   * when a new JSONRpcClient object is initialized by the browser side javascript.)
   *
   * @return A JSONArray of method names (in the format of Class.Method)
   */
  public JSONArray getSystemMethods() {
    Set<String> m = new TreeSet<String>();
    globalBridge.allInstanceMethods(m);
    if (globalBridge != this) {
      globalBridge.allStaticMethods(m);
      globalBridge.allInstanceMethods(m);
    }
    allStaticMethods(m);
    allInstanceMethods(m);
    allCallableReferences(m);
    JSONArray methodNames = new JSONArray();
    for (String methodName : m) {
      methodNames.put(methodName);
    }
    return methodNames;
  }

  /**
   * Add all methods on registered callable references to a HashSet.
   * 
   * @param m Set to add all methods to.
   */
  private void allCallableReferences(Set<String> m) {
    synchronized (callableReferenceSet) {
      for (Class<?> clazz : callableReferenceSet) {
        ClassData cd = ClassAnalyzer.getClassData(clazz);

        uniqueMethods(m, CALLABLE_REFERENCE_METHOD_PREFIX + "[" + clazz.getName() + "].", cd
            .getStaticMethodMap());
        uniqueMethods(m, CALLABLE_REFERENCE_METHOD_PREFIX + "[" + clazz.getName() + "].", cd
            .getMethodMap());
      }
    }
  }

  /**
   * Add all instance methods that can be invoked on this bridge to a HashSet.
   * 
   * @param m HashSet to add all static methods to.
   */
  private void allInstanceMethods(Set<String> m) {
    synchronized (objectMap) {
      for (Map.Entry<Object, ObjectInstance> oientry : objectMap.entrySet()) {
        Object key = oientry.getKey();
        if (!(key instanceof String)) {
          continue;
        }
        String name = (String) key;
        ObjectInstance oi = oientry.getValue();
        ClassData cd = ClassAnalyzer.getClassData(oi.getClazz());
        uniqueMethods(m, name + ".", cd.getMethodMap());
        uniqueMethods(m, name + ".", cd.getStaticMethodMap());
      }
    }
  }

  /**
   * Add all static methods that can be invoked on this bridge to the given HashSet.
   * 
   * @param m HashSet to add all static methods to.
   */
  private void allStaticMethods(Set<String> m) {
    synchronized (classMap) {
      for (Map.Entry<String, Class<?>> cdentry : classMap.entrySet()) {
        String name = cdentry.getKey();
        Class<?> clazz = cdentry.getValue();
        ClassData cd = ClassAnalyzer.getClassData(clazz);
        uniqueMethods(m, name + ".", cd.getStaticMethodMap());
      }
    }
  }

  /**
   * Gets the methods that can be called on the given object
   * 
   * @param objectID The id of the object or 0 if it is a class
   * @param className The name of the class of the object - only required if objectID==0
   * @param methodName The name of method in the request
   * @return A map of AccessibleObjectKeys to a Collection of AccessibleObjects
   * @throws NoSuchMethodException If methods cannot be found for the class
   */
  private Map<AccessibleObjectKey, Set<AccessibleObject>> getAccessibleObjectMap(final int objectID,
      final String className, final String methodName) throws NoSuchMethodException

  {
    final Map<AccessibleObjectKey, Set<AccessibleObject>> methodMap =
        new HashMap<AccessibleObjectKey, Set<AccessibleObject>>();
    // if it is not an object
    if (objectID == 0) {
      final ObjectInstance oi = resolveObject(className);
      final ClassData classData = resolveClass(className);

      // Look up the class, object instance and method objects
      if (oi != null) {
        methodMap.putAll(ClassAnalyzer.getClassData(oi.getClazz()).getMethodMap());
      }
      // try to get the constructor data
      else if (methodName.equals(CONSTRUCTOR_FLAG)) {
        try {
          methodMap.putAll(ClassAnalyzer.getClassData(lookupClass(className)).getConstructorMap());
        } catch (Exception e) {
          throw new NoSuchMethodException(FailedResult.MSG_ERR_NOCONSTRUCTOR);
        }
      }
      // else it must be static
      else if (classData != null) {
        methodMap.putAll(classData.getStaticMethodMap());
      } else {
        throw new NoSuchMethodException(FailedResult.MSG_ERR_NOMETHOD);
      }
    }
    // else it is an object, so we can get the member methods
    else {
      final ObjectInstance oi = resolveObject(new Integer(objectID));
      if (oi == null) {
        throw new NoSuchMethodException("Object not found");
      }
      ClassData cd = ClassAnalyzer.getClassData(oi.getClazz());
      methodMap.putAll(cd.getMethodMap());
    }
    return methodMap;
  }

  /**
   * Resolves an objectId to an actual object
   * 
   * @param objectID The id of the object to resolve
   * @param className The name of the class of the object
   * @return The object requested
   */
  private Object getObjectContext(final int objectID, final String className) {
    final Object objectContext;
    if (objectID == 0) {
      final ObjectInstance oi = resolveObject(className);
      if (oi != null) {
        objectContext = oi.getObject();
      } else {
        objectContext = null;
      }
    } else {
      final ObjectInstance oi = resolveObject(new Integer(objectID));
      if (oi != null) {
        objectContext = oi.getObject();
      } else {
        objectContext = null;
      }
    }
    return objectContext;
  }

  /**
   * Resolves a string to a class
   * 
   * @param className The name of the class to resolve
   * @return The data associated with the className
   */
  private ClassData resolveClass(String className) {
    Class<?> clazz;
    ClassData cd = null;

    synchronized (classMap) {
      clazz = classMap.get(className);
    }

    if (clazz != null) {
      cd = ClassAnalyzer.getClassData(clazz);
    }

    if (cd != null) {
      if (log.isDebugEnabled()) {
        log.debug("found class " + cd.getClazz().getName() + " named " + className);
      }
      return cd;
    }

    if (this != globalBridge) {
      return globalBridge.resolveClass(className);
    }

    return null;
  }

  /**
   * Resolve the key to a specified instance object. If an instance object of the requested key is
   * not found, and this is not the global bridge, then look in the global bridge too.
   * <p/>
   * If the key is not found in this bridge or the global bridge, the requested key may be a class
   * method (static method) or may not exist (not registered under the requested key.)
   * 
   * @param key registered object key being requested by caller.
   * @return ObjectInstance that has been registered under this key, in this bridge or the global
   *         bridge.
   */
  private ObjectInstance resolveObject(Object key) {
    ObjectInstance oi;
    synchronized (objectMap) {
      oi = objectMap.get(key);
    }
    if (log.isDebugEnabled() && oi != null) {
      log.debug("found object " + oi.getObject().hashCode() + " of class " + oi.getClazz().getName()
          + " with key " + key);
    }
    if (oi == null && this != globalBridge) {
      return globalBridge.resolveObject(key);
    }
    return oi;
  }

}