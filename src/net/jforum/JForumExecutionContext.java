
package net.jforum;

import java.io.IOException;
import java.sql.Connection;

import javax.servlet.http.HttpServletResponse;

import net.jforum.context.RequestContext;
import net.jforum.context.ResponseContext;
import net.jforum.context.ForumContext;
import net.jforum.exceptions.ForumException;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;

import org.apache.log4j.Logger;

import freemarker.template.Configuration;
import freemarker.template.ObjectWrapper;
import freemarker.template.SimpleHash;

public class JForumExecutionContext {

	private static Logger logger = Logger.getLogger(JForumExecutionContext.class);

	// ==============================================================================
	// the static fields which be keep permanent status during Application LifeCycle
	// ==============================================================================
	@SuppressWarnings("rawtypes")
	private static ThreadLocal userData         = new ThreadLocal();
	private static Configuration templateConfig = null;
	// =============================================================================
	// the instance fields which will be reset to initial values after re-instanced
	// =============================================================================
	private Connection conn           = null;
	private ForumContext forumContext = null;
	private SimpleHash context        = new SimpleHash(ObjectWrapper.BEANS_WRAPPER);
	private String redirectTo  = null;
	private String contentType = null;
	private boolean isCustomContent = false;
	private boolean enableRollback  = false;

	@SuppressWarnings("unchecked")
	public static JForumExecutionContext get() {
		JForumExecutionContext ex = (JForumExecutionContext)userData.get();
		if (ex == null) {
			ex = new JForumExecutionContext();
			userData.set(ex);
			System.out.println("INFOR: the new instance of 'JForumExecutionContext' is built and assigned to 'userData' (ThreadLocal) ...");
		}
		return ex;
	}

	public static boolean exists() {
		return (userData.get() != null);
	}

	public static void setTemplateConfig(Configuration config) {
		templateConfig = config;
	}

	public static Configuration templateConfig() {
		return templateConfig;
	}

	@SuppressWarnings("unchecked")
	public static void set(JForumExecutionContext ex) {
		userData.set(ex);
	}

	public void setConnection(Connection conn) {
		this.conn = conn;
	}

	public static Connection getConnection() {
		return getConnection(true);
	}

	public static Connection getConnection(boolean validate) {
		JForumExecutionContext ex = get();
		Connection c = ex.conn;
		if (validate && c == null) {
			c = DBConnection.getImplementation().getConnection();
			try {
				c.setAutoCommit(!SystemGlobals.getBoolValue(ConfigKeys.DATABASE_USE_TRANSACTIONS));
			} catch (Exception e) {
				// catch error for AutoCommit
			}
			ex.setConnection(c);
			set(ex);
			System.out.println("INFOR: the new Connection object is created and assigned to 'ex' ...");
		}
		return c;
	}

	public static ForumContext getForumContext() {
		return ((JForumExecutionContext)userData.get()).forumContext;
	}

	public void setForumContext(ForumContext forumContext) {
		this.forumContext = forumContext;
	}

	public static RequestContext getRequest() {
		return getForumContext().getRequest();
	}

	public static ResponseContext getResponse() {
		return getForumContext().getResponse();
	}

	public static SimpleHash getTemplateContext() {
		return ((JForumExecutionContext)userData.get()).context;
	}

	public static void setRedirect(String redirect) {
		((JForumExecutionContext)userData.get()).redirectTo = redirect;
	}

	public static void setContentType(String contentType) {
		((JForumExecutionContext)userData.get()).contentType = contentType;
	}

	public static String getContentType() {
		return ((JForumExecutionContext)userData.get()).contentType;
	}

	public static String getRedirectTo() {
		JForumExecutionContext ex = (JForumExecutionContext)userData.get();
		return (ex != null ? ex.redirectTo : null);
	}

	public static void enableCustomContent(boolean enable) {
		((JForumExecutionContext)userData.get()).isCustomContent = enable;
	}

	public static boolean isCustomContent() {
		return ((JForumExecutionContext)userData.get()).isCustomContent;
	}

	public static void enableRollback() {
		((JForumExecutionContext)userData.get()).enableRollback = true;
	}

	public static boolean shouldRollback() {
		return ((JForumExecutionContext)userData.get()).enableRollback;
	}

	public static void requestBasicAuthentication() {
		getResponse().addHeader("WWW-Authenticate", "Basic realm=\"JForum\"");
		try {
			getResponse().sendError(HttpServletResponse.SC_UNAUTHORIZED);
		} catch (IOException e) {
			throw new ForumException(e);
		}
		enableCustomContent(true);
	}

	@SuppressWarnings("unchecked")
	public static void finish() {
		System.out.println("--> [JForumExecution.finish] ......");
		Connection conn = JForumExecutionContext.getConnection(false);
		if (conn != null) {
			if (SystemGlobals.getBoolValue(ConfigKeys.DATABASE_USE_TRANSACTIONS)) {
				if (JForumExecutionContext.shouldRollback()) {
					try {
						conn.rollback();
					} catch (Exception e) {
						logger.error("Error while rolling back a transaction", e);
					}
				} else {
					try {
						conn.commit();
					} catch (Exception e) {
						logger.error("Error while commiting a transaction", e);
					}
				}
			}
			try {
				DBConnection.getImplementation().releaseConnection(conn);
			} catch (Exception e) {
				logger.error("Error while releasing the connection : " + e, e);
			}
		}
		userData.set(null);
	}
}
