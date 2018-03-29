
package net.jforum.util.preferences;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import net.jforum.ConfigLoader;
import net.jforum.exceptions.ForumException;

import org.apache.log4j.Logger;

public class SystemGlobals implements VariableStore {

	private static final Logger logger = Logger.getLogger(SystemGlobals.class);

	private static SystemGlobals globals       = new SystemGlobals();
	@SuppressWarnings("rawtypes")
	private static List additionalDefaultsList = new ArrayList();
	private static Properties queries          = new Properties();
	private static Properties transientValues  = new Properties();

	private String defaultConfig      = null;
	private String installationConfig = null;

	private Properties defaults     = new Properties();
	private Properties installation = new Properties();

	@SuppressWarnings("rawtypes")
	private Map objectProperties = new HashMap();

	private VariableExpander expander = new VariableExpander(this, "${", "}");

	private SystemGlobals() {
		
	}

	public static void initGlobals(String appPath, String mainConfigurationFile) {
		System.out.println("--> [SystemGlobals.initGlobals] ......");
		// *->Design Pattern : singleton
		globals = new SystemGlobals();
		globals.buildSystem(appPath, mainConfigurationFile);
	}

	public static void reset() {
		globals.defaults.clear();
		globals.installation.clear();
		additionalDefaultsList.clear();
		queries.clear();
		transientValues.clear();
	}

	@SuppressWarnings("rawtypes")
	private void buildSystem(String appPath, String mainConfigurationFile) {
		System.out.println("--> [SystemGlobals.buildSystem] ......");
		if (mainConfigurationFile == null) {
			throw new InvalidParameterException("defaultConfig could not be null");
		}
		defaultConfig = mainConfigurationFile;
		defaults = new Properties();
		System.out.println("INFOR: try to Assign the value of 'ConfigKeys.APPLICATION_PATH' in 'defaults' with 'appPath'");
		defaults.put(ConfigKeys.APPLICATION_PATH, appPath);
		System.out.println("INFOR: try to Assign the value of 'ConfigKeys.DEFAULT_CONFIG' in 'defaults' with 'mainConfigurationFile'");
		defaults.put(ConfigKeys.DEFAULT_CONFIG, mainConfigurationFile);
		SystemGlobals.loadDefaults();
		installation = new Properties();
		installationConfig = getVariableValue(ConfigKeys.INSTALLATION_CONFIG);
		System.out.println("DEBUG: installationConfig = " + installationConfig + " from 'getVariableValue'");
		for (Iterator iter = additionalDefaultsList.iterator(); iter.hasNext(); ) {
			loadAdditionalDefaults((String)iter.next());
		}
		if (new File(installationConfig).exists()) {
			loadAdditionalDefaults(installationConfig);
		}
	}

	public static void setValue(String field, String value) {
		globals.installation.put(field, value);
		globals.expander.clearCache();
	}

	@SuppressWarnings("unchecked")
	public static void setObjectValue(String field, Object value) {
		globals.objectProperties.put(field, value);
	}

	public static Object getObjectValue(String field) {
		return globals.objectProperties.get(field);
	}

	public static void setTransientValue(String field, String value) {
		transientValues.put(field, value);
	}

	public static void loadDefaults() {
		System.out.println("--> [SystemGlobals.loadDefaults] ......");
		try {
			FileInputStream input = new FileInputStream(globals.defaultConfig);
			globals.defaults.load(input);
			input.close();
			System.out.println("INFOR: loading the content of 'mainConfigurationFile' into 'globals.defaults' ...");
			globals.expander.clearCache();
			System.out.println("INFOR: cleaning the cache of 'globals.expander' ...");
		} catch (IOException e) {
			throw new ForumException(e);
		}
	}

	@SuppressWarnings("unchecked")
	public static void loadAdditionalDefaults(String file) {
		System.out.println("--> [SystemGlobals.loadAdditionalDefaults] ......");
		if (!new File(file).exists()) {
			logger.info("Cannot find file " + file + ". Will ignore it");
			return;
		}
		try {
			FileInputStream input = new FileInputStream(file);
			globals.installation.load(input);
			input.close();
			System.out.println("DEBUG: loading the additional config default file into 'globals.installation' <== " + file);
		} catch (IOException e) {
			throw new ForumException(e);
		}
		// only for the 'installationConfig', 'ConfigKeys.DATABASE_DRIVER_CONFIG' and 'QUARTZ_CONFIG'
		if (!additionalDefaultsList.contains(file)) {
			additionalDefaultsList.add(file);
		}
	}

	public static void saveInstallation() {
		// We need this temporary "p" because, when new FileOutputStream() is called, it will raise an event to the TimerTask who is listen for file modifications, which then reloads the configurations from the file system, overwriting our new keys. 
		@SuppressWarnings("serial")
		class SortedProperties extends Properties {
			@SuppressWarnings({ "unchecked", "rawtypes" })
			public synchronized Enumeration keys() {
				Enumeration keysEnum = super.keys();
				Vector keyList = new Vector();
				while (keysEnum.hasMoreElements()) {
					keyList.add(keysEnum.nextElement());
				}
				Collections.sort(keyList);
				return keyList.elements();
			}
		}
		Properties p = new SortedProperties();
		p.putAll(globals.installation);
		try {
			FileOutputStream out = new FileOutputStream(globals.installationConfig);
			p.store(out, "Installation specific configuration options");
			out.close();
		} catch (IOException e) {
			throw new ForumException(e);
		}
		ConfigLoader.listenInstallationConfig();
	}

	public static String getValue(String field) {
		return globals.getVariableValue(field);
	}

	public static String getTransientValue(String field) {
		return transientValues.getProperty(field);
	}

	public static int getIntValue(String field) {
		return Integer.parseInt(getValue(field));
	}

	public static boolean getBoolValue(String field) {
		return "true".equals(getValue(field));
	}

	public String getVariableValue(String field) {
		String preExpansion = globals.installation.getProperty(field);
		if (preExpansion == null) {
			preExpansion = defaults.getProperty(field);
			if (preExpansion == null) {
				return null;
			}
		}
		return expander.expandVariables(preExpansion);
	}

	public static void setApplicationPath(String ap) {
		setValue(ConfigKeys.APPLICATION_PATH, ap);
	}

	public static String getApplicationPath() {
		return getValue(ConfigKeys.APPLICATION_PATH);
	}

	public static String getApplicationResourceDir() {
		return getValue(ConfigKeys.RESOURCE_DIR);
	}

	public static void loadQueries(String queryFile) {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(queryFile);
			queries.load(fis);
		} catch (IOException e) {
			throw new ForumException(e);
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (Exception e) {
					
				}
			}
		}
	}

	public static String getSql(String sql) {
		return queries.getProperty(sql);
	}

	@SuppressWarnings("rawtypes")
	public static Iterator fetchConfigKeyIterator() {
		return globals.defaults.keySet().iterator();
	}

	public static Properties getConfigData() {
		return new Properties(globals.defaults);
	}
}