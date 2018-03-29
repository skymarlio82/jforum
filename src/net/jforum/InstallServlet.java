
package net.jforum;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.jforum.context.JForumContext;
import net.jforum.context.RequestContext;
import net.jforum.context.ResponseContext;
import net.jforum.context.ForumContext;
import net.jforum.context.web.WebRequestContext;
import net.jforum.context.web.WebResponseContext;
import net.jforum.exceptions.ExceptionWriter;
import net.jforum.repository.ModulesRepository;
import net.jforum.util.I18n;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;
import freemarker.template.SimpleHash;
import freemarker.template.Template;

@SuppressWarnings("serial")
public class InstallServlet extends JForumBaseServlet {

	public void init(ServletConfig config) throws ServletException {
		super.init(config);
	}

	@SuppressWarnings("unused")
	public void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		try {
			String encoding = SystemGlobals.getValue(ConfigKeys.ENCODING);
			req.setCharacterEncoding(encoding);
			// Request
			RequestContext request = new WebRequestContext(req);
			ResponseContext response = new WebResponseContext(res);
			request.setCharacterEncoding(encoding);
			JForumExecutionContext ex = JForumExecutionContext.get();
			ForumContext forumContext = new JForumContext(request.getContextPath(), SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION), request, response, false);
			ex.setForumContext(forumContext);
			// Assigns the information to user's thread
			JForumExecutionContext.set(ex);
			// Context
			SimpleHash context = JForumExecutionContext.getTemplateContext();
			context.put("contextPath", req.getContextPath());
			context.put("serverName", req.getServerName());
			context.put("templateName", "default");
			context.put("serverPort", Integer.toString(req.getServerPort()));
			context.put("I18n", I18n.getInstance());
			context.put("encoding", encoding);
			context.put("extension", SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION));
			context.put("JForumContext", forumContext);
			context.put("version", SystemGlobals.getValue(ConfigKeys.VERSION));
			if (SystemGlobals.getBoolValue(ConfigKeys.INSTALLED)) {
				JForumExecutionContext.setRedirect(request.getContextPath() + "/forums/list" + SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION));
			} else {
				// Module and Action
				String moduleClass = ModulesRepository.getModuleClass(request.getModule());
				context.put("moduleName", request.getModule());
				context.put("action", request.getAction());
				BufferedWriter out = new BufferedWriter(new OutputStreamWriter(response.getOutputStream(), encoding));
				try {
					if (moduleClass != null) {
						// Here we go, baby
						Command c = (Command) Class.forName(moduleClass).newInstance();
						Template template = c.process(request, response, context);
						if (JForumExecutionContext.getRedirectTo() == null) {
							response.setContentType("text/html; charset=" + encoding);
							template.process(context, out);
							out.flush();
						}
					}
				} catch (Exception e) {
					response.setContentType("text/html; charset=" + encoding);
					if (out != null) {
						new ExceptionWriter().handleExceptionData(e, out, request);
					} else {
						new ExceptionWriter().handleExceptionData(e, new BufferedWriter(new OutputStreamWriter(response.getOutputStream())), request);
					}
				}
			}
			String redirectTo = JForumExecutionContext.getRedirectTo();
			if (redirectTo != null) {
				response.sendRedirect(response.encodeRedirectURL(redirectTo));
			}
		} finally {
			JForumExecutionContext.finish();
		}
	}
}
