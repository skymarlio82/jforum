
package net.jforum.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import net.jforum.SessionFacade;
import net.jforum.entities.UserSession;
import net.jforum.exceptions.ForumException;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;

import org.apache.log4j.Logger;

import freemarker.template.SimpleSequence;

public class I18n {

	private final static Logger logger = Logger.getLogger(I18n.class);

	public final static String CANNOT_DELETE_GROUP    = "CannotDeleteGroup";
	public final static String CANNOT_DELETE_CATEGORY = "CannotDeleteCategory";
	public final static String CANNOT_DELETE_BANNER   = "CannotDeleteBanner";

	private static I18n classInstance     = new I18n();
	@SuppressWarnings("rawtypes")
	private static Map messagesMap        = new HashMap();
	private static Properties localeNames = new Properties();
	private static String defaultName     = null;
	private static String baseDir         = null;
	@SuppressWarnings("rawtypes")
	private static List watching          = new ArrayList();

	private I18n() {
		
	}

	public static I18n getInstance() {
		return classInstance;
	}

	public static synchronized void load() {
		System.out.println("--> [I18n.load] ......");
		baseDir = SystemGlobals.getApplicationResourceDir() + "/" + SystemGlobals.getValue(ConfigKeys.LOCALES_DIR);
		System.out.println("DEBUG: ConfigKeys.LOCALES_DIR or baseDir = " + baseDir);
		loadLocales();
		defaultName = SystemGlobals.getValue(ConfigKeys.I18N_DEFAULT_ADMIN);
		System.out.println("DEBUG: ConfigKeys.I18N_DEFAULT_ADMIN or defaultName = " + defaultName);
		load(defaultName, null);
		String custom = SystemGlobals.getValue(ConfigKeys.I18N_DEFAULT);
		System.out.println("DEBUG: ConfigKeys.I18N_DEFAULT or custom = " + custom);
		if (!custom.equals(defaultName)) {
			load(custom, defaultName);
			defaultName = custom;
		}
	}

	public static void changeBoardDefault(String newDefaultLanguage) {
		load(newDefaultLanguage, SystemGlobals.getValue(ConfigKeys.I18N_DEFAULT_ADMIN));
		defaultName = newDefaultLanguage;
	}

	private static void loadLocales() {
		System.out.println("--> [I18n.loadLocales] ......");
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(baseDir + SystemGlobals.getValue(ConfigKeys.LOCALES_NAMES));
			localeNames.load(fis);
			System.out.println("DEBUG: the config filePath of ConfigKeys.LOCALES_NAMES = " + SystemGlobals.getValue(ConfigKeys.LOCALES_NAMES));
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

	protected static void load(String localeName, String mergeWith) {
		load(localeName, mergeWith, false);
	}

	@SuppressWarnings("unchecked")
	protected static void load(String localeName, String mergeWith, boolean force) {
		System.out.println("--> [I18n.load] ......");
		System.out.println("DEBUG: localeName = " + localeName);
		System.out.println("DEBUG: mergeWith = " + mergeWith);
		System.out.println("DEBUG: force = " + force);
		if (!force && (localeName == null || localeName.trim().equals("") || I18n.contains(localeName))) {
			return;
		}
		if (localeNames.size() == 0) {
			loadLocales();
		}
		Properties p = new Properties();
		if (mergeWith != null) {
			if (!I18n.contains(mergeWith)) {
				load(mergeWith, null);
			}
			p.putAll((Properties)messagesMap.get(mergeWith));
		}
		FileInputStream fis = null;
		try {
			String filename = baseDir + localeNames.getProperty(localeName);
			// If the requested locale does not exist, use the default
			if (!new File(filename).exists()) {
				filename = baseDir + localeNames.getProperty(SystemGlobals.getValue(ConfigKeys.I18N_DEFAULT_ADMIN));
			}
			System.out.println("DEBUG: ConfigKeys.I18N_DEFAULT_ADMIN filename = " + filename);
			fis = new FileInputStream(filename);
			p.load(fis);
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
		messagesMap.put(localeName, p);
		System.out.println("INFOR: I18N Default Admin already loaded into messagesMap ...");
		watchForChanges(localeName);
	}

	public static void load(String localeName) {
		load(localeName, SystemGlobals.getValue(ConfigKeys.I18N_DEFAULT));
	}

	@SuppressWarnings("rawtypes")
	public static void reset() {
		messagesMap = new HashMap();
		localeNames = new Properties();
		defaultName = null;
	}

	@SuppressWarnings("unchecked")
	private static void watchForChanges(final String localeName) {
		System.out.println("--> [I18n.watchForChanges] ......");
		if (!watching.contains(localeName)) {
			watching.add(localeName);
			int fileChangesDelay = SystemGlobals.getIntValue(ConfigKeys.FILECHANGES_DELAY);
			System.out.println("DEBUG: ConfigKeys.FILECHANGES_DELAY = " + fileChangesDelay);
			if (fileChangesDelay > 0) {
				FileMonitor.getInstance().addFileChangeListener(new FileChangeListener() {
					public void fileChanged(String filename) {
						if (logger.isDebugEnabled()) {
							logger.info("Reloading i18n for " + localeName);
						}
						I18n.load(localeName, SystemGlobals.getValue(ConfigKeys.I18N_DEFAULT), true);
					}
				}, baseDir + localeNames.getProperty(localeName), fileChangesDelay);
			}
		}
	}

	public static String getMessage(String localeName, String messageName, Object params[]) {
		return MessageFormat.format(((Properties)messagesMap.get(localeName)).getProperty(messageName), params);
	}

	public static String getMessage(String messageName, Object params[]) {
		String lang = "";
		UserSession us = SessionFacade.getUserSession();
		if (us != null && us.getLang() != null) {
			lang = us.getLang();
		}
		if ("".equals(lang)) {
			return getMessage(defaultName, messageName, params);
		}
		return getMessage(lang, messageName, params);
	}

	public static String getMessage(String messageName, SimpleSequence params) {
		try {
			return getMessage(messageName, params.toList().toArray());
		} catch (Exception e) {
			throw new ForumException(e);
		}
	}

	public static String getMessage(String localeName, String m) {
		if (!messagesMap.containsKey(localeName)) {
			load(localeName);
		}
		return ((Properties)messagesMap.get(localeName)).getProperty(m);
	}

	public static String getMessage(String m) {
		return getMessage(getUserLanguage(), m);
	}

	public static String getMessage(String m, UserSession us) {
		if (us == null || us.getLang() == null || us.getLang().equals("")) {
			return getMessage(defaultName, m);
		}
		return getMessage(us.getLang(), m);
	}

	public static String getUserLanguage() {
		UserSession us = SessionFacade.getUserSession();
		if (us == null || us.getLang() == null || us.getLang().trim().equals("")) {
			return defaultName;
		}
		return us.getLang();
	}

	public static boolean contains(String language) {
		return messagesMap.containsKey(language);
	}

	public static boolean languageExists(String language) {
		return localeNames.getProperty(language) != null;
	}
}