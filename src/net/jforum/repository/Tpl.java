
package net.jforum.repository;

import java.io.FileInputStream;
import java.util.Iterator;
import java.util.Properties;

import net.jforum.cache.CacheEngine;
import net.jforum.cache.Cacheable;
import net.jforum.exceptions.ConfigLoadException;

public class Tpl implements Cacheable {

	private final static String FQN = "templates";

	private static CacheEngine cache = null;

	public void setCacheEngine(CacheEngine engine) {
		cache = engine;
	}

	@SuppressWarnings("rawtypes")
	public static void load(String filename) {
		System.out.println("--> [Tpl.load] ......");
		FileInputStream fis = null;
		try {
			Properties p = new Properties();
			fis = new FileInputStream(filename);
			p.load(fis);
			for (Iterator iter = p.keySet().iterator(); iter.hasNext(); ) {
				String key = (String)iter.next();
				cache.add(FQN, key, p.getProperty(key));
			}
			System.out.println("INFOR: already loaded all the template mapping into 'cache->FQN' from 'templatesMapping.properties' ...");
		} catch (Exception e) {
			e.printStackTrace();
			throw new ConfigLoadException("Error while trying to load " + filename + ": " + e);
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (Exception e) {

				}
			}
		}
	}

	public static String name(String key) {
		return (String)cache.get(FQN, key);
	}
}
