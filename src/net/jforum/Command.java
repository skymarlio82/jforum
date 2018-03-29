
package net.jforum;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import net.jforum.context.RequestContext;
import net.jforum.context.ResponseContext;
import net.jforum.exceptions.ForumException;
import net.jforum.exceptions.TemplateNotFoundException;
import net.jforum.repository.Tpl;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;
import net.jforum.util.preferences.TemplateKeys;
import freemarker.template.SimpleHash;
import freemarker.template.Template;

public abstract class Command {

	@SuppressWarnings("rawtypes")
	private static Class[] NO_ARGS_CLASS   = new Class[0];
	private static Object[] NO_ARGS_OBJECT = new Object[0];
	private boolean ignoreAction       = false;
	protected String templateName      = null;
	protected RequestContext request   = null;
	protected ResponseContext response = null;
	protected SimpleHash context       = null;

	protected void setTemplateName(String templateName) {
		System.out.println("--> [Command.setTemplateName] ......");
		this.templateName = Tpl.name(templateName);
		System.out.println("DEBUG: the 'templateName' in Command = " + templateName);
	}

	protected void ignoreAction() {
		this.ignoreAction = true;
	}

	public abstract void list();

	@SuppressWarnings("rawtypes")
	public Template process(RequestContext request, ResponseContext response, SimpleHash context) {
		System.out.println("--> [Command.process] ......");
		this.request = request;
		this.response = response;
		this.context = context;
		String action = request.getAction();
		if (!ignoreAction) {
			try {
				getClass().getMethod(action, NO_ARGS_CLASS).invoke(this, NO_ARGS_OBJECT);
				// ==================================
				// Only for test purpose 201714231526
				// ==================================
				System.out.println("DEBUG: the map in the freemarker - 'context' (SimpleHash)");
				Map map = context.toMap();
				Set set = map.keySet();
				Iterator ite = set.iterator();
				while (ite.hasNext()) {
					String key = (String)ite.next();
					Object obj = map.get(key);
					if (obj instanceof String) {
						System.out.println("DEBUG: key : " + key + " = (String) " + obj);
					} else if (obj instanceof Boolean) {
						System.out.println("DEBUG: key : " + key + " = (Boolean) " + ((Boolean)obj).toString());
					} else if (obj instanceof Integer) {
						System.out.println("DEBUG: key : " + key + " = (Integer) " + ((Integer)obj).toString());
					} else if (obj instanceof Double) {
						System.out.println("DEBUG: key : " + key + " = (Double) " + ((Double)obj).toString());
					} else if (obj instanceof Long) {
						System.out.println("DEBUG: key : " + key + " = (Double) " + ((Long)obj).toString());
					} else {
						System.out.println("DEBUG: key: " + key + " is a complex object type");
					}
				}
			} catch (NoSuchMethodException e) {
				list();
			} catch (Exception e) {
                throw new ForumException(e);
			}
		}
		if (JForumExecutionContext.getRedirectTo() != null) {
			setTemplateName(TemplateKeys.EMPTY);
		} else if (request.getAttribute("template") != null) {
			setTemplateName((String)request.getAttribute("template"));
		}
		if (JForumExecutionContext.isCustomContent()) {
			return null;
		}
		if (templateName == null) {
			throw new TemplateNotFoundException("Template for action " + action + " is not defined");
		}
        try {
        	String templatePathName = new StringBuffer(SystemGlobals.getValue(ConfigKeys.TEMPLATE_DIR)).append('/').append(templateName).toString();
        	System.out.println("The template path for the current 'module + action' = " + templatePathName);
            return JForumExecutionContext.templateConfig().getTemplate(templatePathName);
        } catch (IOException e) {
            throw new ForumException(e);
        }
    }
}
