
package net.jforum.repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import net.jforum.ConfigLoader;
import net.jforum.JForumExecutionContext;

import org.apache.log4j.Logger;

public class ModulesRepository {

	private final static Logger logger = Logger.getLogger(ModulesRepository.class);

	private final static String ENTRIES = "entries";

	@SuppressWarnings("rawtypes")
	private static Map cache = new HashMap();

	@SuppressWarnings("unchecked")
	public static void init(String baseDir) {
		System.out.println("--> [ModulesRepository.init] ......");
		cache.put(ENTRIES, ConfigLoader.loadModulesMapping(baseDir));
		System.out.println("INFOR: already load the file of '/modulesMapping.properties' to 'cache->ENTRIES' ...");
	}

	public static int size() {
		return cache.size();
	}

	public static String getModuleClass(String moduleName) {
		System.out.println("--> [ModulesRepository.getModuleClass] ......");
		System.out.println("DEBUG: moduleName = " + moduleName);
		Properties p = (Properties)cache.get(ENTRIES);
		if (p == null) {
			logger.error("Null modules. Askes moduleName: " + moduleName + ", url=" + JForumExecutionContext.getRequest().getQueryString());
			return null;
		}
		return p.getProperty(moduleName);
	}
}
