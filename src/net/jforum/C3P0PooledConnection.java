
package net.jforum;

import java.lang.reflect.Method;
import java.sql.Connection;

import net.jforum.exceptions.DatabaseException;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.mchange.v2.c3p0.DataSources;

public class C3P0PooledConnection extends DBConnection {

	private ComboPooledDataSource ds = null;

	public void init() throws Exception {
		System.out.println("--> [C3P0PooledConnection.init] ......");
		ds = new ComboPooledDataSource();
		System.out.println("DEBUG: ConfigKeys.DATABASE_CONNECTION_DRIVER = " + SystemGlobals.getValue(ConfigKeys.DATABASE_CONNECTION_DRIVER));
		ds.setDriverClass(SystemGlobals.getValue(ConfigKeys.DATABASE_CONNECTION_DRIVER));
		System.out.println("DEBUG: ConfigKeys.DATABASE_CONNECTION_STRING = " + SystemGlobals.getValue(ConfigKeys.DATABASE_CONNECTION_STRING));
		ds.setJdbcUrl(SystemGlobals.getValue(ConfigKeys.DATABASE_CONNECTION_STRING));
		System.out.println("DEBUG: ConfigKeys.DATABASE_POOL_MIN = " + SystemGlobals.getValue(ConfigKeys.DATABASE_POOL_MIN));
		ds.setMinPoolSize(SystemGlobals.getIntValue(ConfigKeys.DATABASE_POOL_MIN));
		System.out.println("DEBUG: ConfigKeys.DATABASE_POOL_MAX = " + SystemGlobals.getValue(ConfigKeys.DATABASE_POOL_MAX));
		ds.setMaxPoolSize(SystemGlobals.getIntValue(ConfigKeys.DATABASE_POOL_MAX));
		System.out.println("DEBUG: ConfigKeys.DATABASE_PING_DELAY = " + SystemGlobals.getValue(ConfigKeys.DATABASE_PING_DELAY));
		ds.setIdleConnectionTestPeriod(SystemGlobals.getIntValue(ConfigKeys.DATABASE_PING_DELAY));
		extraParams();
	}

	private void extraParams() {
		System.out.println("--> [C3P0PooledConnection.extraParams] ......");
		String extra = SystemGlobals.getValue(ConfigKeys.C3P0_EXTRA_PARAMS);
		System.out.println("DEBUG: ConfigKeys.C3P0_EXTRA_PARAMS = " + extra);
		if (extra != null && extra.trim().length() > 0) {
			String[] p = extra.split(";");
			for (int i = 0; i < p.length; i++) {
				String[] kv = p[i].trim().split("=");
				if (kv.length == 2) {
					invokeSetter(kv[0], kv[1]);
				}
			}
		}
	}

	@SuppressWarnings("rawtypes")
	private void invokeSetter(String propertyName, String value) {
		try {
			String setter = "set" + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
			Method[] methods = ds.getClass().getMethods();
			for (int i = 0; i < methods.length; i++) {
				Method method = methods[i];
				if (method.getName().equals(setter)) {
					Class[] paramTypes = method.getParameterTypes();
					if (paramTypes[0] == String.class) {
						method.invoke(ds, new Object[] { value });
					} else if (paramTypes[0] == int.class) {
						method.invoke(ds, new Object[] { new Integer(value) });
					} else if (paramTypes[0] == boolean.class) {
						method.invoke(ds, new Object[] { Boolean.valueOf(value) });
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Connection getConnection() {
		try {
			return ds.getConnection();
		} catch (Exception e) {
			throw new DatabaseException(e);
		}
	}

	public void releaseConnection(Connection conn) {
        if (conn == null) {
            return;
        }
        try {
			conn.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void realReleaseAllConnections() throws Exception {
		DataSources.destroy(ds);
	}
}
