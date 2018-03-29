
package net.jforum;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.Connection;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.jforum.context.JForumContext;
import net.jforum.context.RequestContext;
import net.jforum.context.ResponseContext;
import net.jforum.context.web.WebRequestContext;
import net.jforum.context.web.WebResponseContext;
import net.jforum.dao.MySQLVersionWorkarounder;
import net.jforum.entities.Banlist;
import net.jforum.exceptions.ExceptionWriter;
import net.jforum.exceptions.ForumStartupException;
import net.jforum.repository.BanlistRepository;
import net.jforum.repository.ModulesRepository;
import net.jforum.repository.RankingRepository;
import net.jforum.repository.SecurityRepository;
import net.jforum.repository.SmiliesRepository;
import net.jforum.util.I18n;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;
import freemarker.template.SimpleHash;
import freemarker.template.Template;

@SuppressWarnings("serial")
public class JForum extends JForumBaseServlet {

	private static boolean isDatabaseUp = false;

	public void init(ServletConfig config) throws ServletException {
		System.out.println("--> [JForum.init] ......");
		super.init(config);
		super.startApplication();
		// Start database
		isDatabaseUp = ForumStartup.startDatabase();
		System.out.println("DEBUG: the flag of 'isDatabaseUp' = " + isDatabaseUp);
		try {
			Connection conn = DBConnection.getImplementation().getConnection();
			System.out.println("DEBUG: ConfigKeys.DATABASE_USE_TRANSACTIONS = " + SystemGlobals.getBoolValue(ConfigKeys.DATABASE_USE_TRANSACTIONS));
			conn.setAutoCommit(!SystemGlobals.getBoolValue(ConfigKeys.DATABASE_USE_TRANSACTIONS));
			// Try to fix some MySQL problems
			MySQLVersionWorkarounder dw = new MySQLVersionWorkarounder();
			dw.handleWorkarounds(conn);
			// Continues loading the forum
			JForumExecutionContext ex = JForumExecutionContext.get();
			ex.setConnection(conn);
			JForumExecutionContext.set(ex);
			// Initialize general forum stuff
			ForumStartup.startForumRepository();
			RankingRepository.loadRanks();
			SmiliesRepository.loadSmilies();
			BanlistRepository.loadBanlist();
		} catch (Throwable e) {
			e.printStackTrace();
			throw new ForumStartupException("Error while starting jforum", e);
		} finally {
			JForumExecutionContext.finish();
		}
	}

	public void service(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
		System.out.println("--> [JForum.service] ......");
		Writer out = null;
		JForumContext forumContext = null;
		RequestContext request = null;
		ResponseContext response = null;
		String encoding = SystemGlobals.getValue(ConfigKeys.ENCODING);
		try {
			// Initializes the execution context
			JForumExecutionContext ex = JForumExecutionContext.get();
			request = new WebRequestContext(req);
			System.out.println("INFOR: Build the instance of WebRequestContext based on HttpServletRequest ...");
            response = new WebResponseContext(res);
            System.out.println("INFOR: Build the instance of WebResponseContext based on HttpServletResponse ...");
			checkDatabaseStatus();
            forumContext = new JForumContext(request.getContextPath(), SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION), request, response);
            System.out.println("INFOR: Build the instance of JForumContext ...");
            ex.setForumContext(forumContext);
            JForumExecutionContext.set(ex);
			// Setup stuff
			SimpleHash context = JForumExecutionContext.getTemplateContext();
			ControllerUtils utils = new ControllerUtils();
			utils.refreshSession();
			context.put("logged", SessionFacade.isLogged());
			// Process security data
			SecurityRepository.load(SessionFacade.getUserSession().getUserId());
			utils.prepareTemplateContext(context, forumContext);
			String module = request.getModule();
			// Gets the module class name
			String moduleClass = (module != null) ? ModulesRepository.getModuleClass(module) : null;
			System.out.println("DEBUG: moduleClass = " + moduleClass);
			if (moduleClass == null) {
				// Module not found, send 404 not found response
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
			} else {
				boolean shouldBan = shouldBan(request.getRemoteAddr());
				System.out.println("DEBUG: shouldBan = " + shouldBan);
				if (!shouldBan) {
					context.put("moduleName", module);
					context.put("action", request.getAction());
				} else {
					moduleClass = ModulesRepository.getModuleClass("forums");
					context.put("moduleName", "forums");
					((WebRequestContext)request).changeAction("banned");
				}
				if (shouldBan && SystemGlobals.getBoolValue(ConfigKeys.BANLIST_SEND_403FORBIDDEN)) {
					response.sendError(HttpServletResponse.SC_FORBIDDEN);
				} else {
					context.put("language", I18n.getUserLanguage());
					context.put("session", SessionFacade.getUserSession());
					context.put("request", req);
					context.put("response", response);
					out = processCommand(out, request, response, encoding, context, moduleClass);
				}
			}
		} catch (Exception e) {
			handleException(out, response, encoding, e, request);
		} finally {
			handleFinally(out, forumContext, response);
			System.out.println("\r\n==================== One HTTP Request Action Process Done ====================\r\n");
		}
	}

	private Writer processCommand(Writer out, RequestContext request, ResponseContext response, String encoding, SimpleHash context, String moduleClass) throws Exception {
		System.out.println("--> [JForum.processCommand] ......");
		Command c = retrieveCommand(moduleClass);
		Template template = c.process(request, response, context);
		System.out.println("DEBUG: JForumExecutionContext.getRedirectTo() = " + JForumExecutionContext.getRedirectTo());
		if (JForumExecutionContext.getRedirectTo() == null) {
			String contentType = JForumExecutionContext.getContentType();
			if (contentType == null) {
				contentType = "text/html; charset=" + encoding;
			}
			response.setContentType(contentType);
			// Binary content are expected to be fully handled in the action, including output-stream manipulation
			System.out.println("DEBUG: JForumExecutionContext.isCustomContent() = " + JForumExecutionContext.isCustomContent());
			if (!JForumExecutionContext.isCustomContent()) {
				out = new BufferedWriter(new OutputStreamWriter(response.getOutputStream(), encoding));
				template.process(JForumExecutionContext.getTemplateContext(), out);
				out.flush();
			}
		}
		return out;
	}

	private void checkDatabaseStatus() {
		System.out.println("--> [JForum.checkDatabaseStatus] ......");
		if (!isDatabaseUp) {
			synchronized (this) {
				if (!isDatabaseUp) {
					isDatabaseUp = ForumStartup.startDatabase();
				}
			}
		}
	}

	private void handleFinally(Writer out, JForumContext forumContext, ResponseContext response) throws IOException {
		System.out.println("--> [JForum.handleFinally] ......");
		try {
			if (out != null) {
				out.close(); 
			}
		} catch (Exception e) {
		    // catch close error 
		}
		String redirectTo = JForumExecutionContext.getRedirectTo();
		System.out.println("DEBUG: redirectTo = " + redirectTo);
		JForumExecutionContext.finish();
		if (redirectTo != null) {
			if (forumContext != null && forumContext.isEncodingDisabled()) {
				response.sendRedirect(redirectTo);
			} else {
				response.sendRedirect(response.encodeRedirectURL(redirectTo));
			}
		}
	}

	private void handleException(Writer out, ResponseContext response, String encoding, Exception e, RequestContext request) throws IOException {
		JForumExecutionContext.enableRollback();
		if (e.toString().indexOf("ClientAbortException") == -1) {
			response.setContentType("text/html; charset=" + encoding);
			if (out != null) {
				new ExceptionWriter().handleExceptionData(e, out, request);
			} else {
				new ExceptionWriter().handleExceptionData(e, new BufferedWriter(new OutputStreamWriter(response.getOutputStream())), request);
			}
		}
	}

	private boolean shouldBan(String ip) {
		System.out.println("--> [JForum.shouldBan] ......");
		System.out.println("DEBUG: ip of Banlist = " + ip);
		Banlist b = new Banlist();
		b.setUserId(SessionFacade.getUserSession().getUserId());
		System.out.println("DEBUG: userId of Banlist = " + b.getUserId());
		b.setIp(ip);
		return BanlistRepository.shouldBan(b);
	}

	private Command retrieveCommand(String moduleClass) throws Exception {
		System.out.println("--> [JForum.retrieveCommand] ......");
		System.out.println("DEBUG: building the instance of '" + moduleClass + "' ...");
		return (Command)Class.forName(moduleClass).newInstance();
	}

	public void destroy() {
		super.destroy();
		System.out.println("Destroying JForum...");
		try {
			DBConnection.getImplementation().realReleaseAllConnections();
			ConfigLoader.stopCacheEngine();
		} catch (Exception e) {
			
		}
	}
}
