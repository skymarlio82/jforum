
package net.jforum;

import java.io.File;
import java.util.Date;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import net.jforum.exceptions.ForumStartupException;
import net.jforum.repository.BBCodeRepository;
import net.jforum.repository.ModulesRepository;
import net.jforum.repository.Tpl;
import net.jforum.util.I18n;
import net.jforum.util.bbcode.BBCodeHandler;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;

@SuppressWarnings("serial")
public class JForumBaseServlet extends HttpServlet {

	private static Logger logger = Logger.getLogger(JForumBaseServlet.class);

	protected boolean debug = false;

	protected void startApplication() {
		System.out.println("--> [JForumBaseServlet.startApplication] ......");
		try {
			System.out.println("DEBUG: ConfigKeys.SQL_QUERIES_GENERIC = " + SystemGlobals.getValue(ConfigKeys.SQL_QUERIES_GENERIC));
			SystemGlobals.loadQueries(SystemGlobals.getValue(ConfigKeys.SQL_QUERIES_GENERIC));
			System.out.println("DEBUG: ConfigKeys.SQL_QUERIES_DRIVER = " + SystemGlobals.getValue(ConfigKeys.SQL_QUERIES_DRIVER));
			SystemGlobals.loadQueries(SystemGlobals.getValue(ConfigKeys.SQL_QUERIES_DRIVER));
			String filename = SystemGlobals.getValue(ConfigKeys.QUARTZ_CONFIG);
			System.out.println("DEBUG: ConfigKeys.QUARTZ_CONFIG = " + filename);
			SystemGlobals.loadAdditionalDefaults(filename);
			ConfigLoader.createLoginAuthenticator();
			ConfigLoader.loadDaoImplementation();
			ConfigLoader.listenForChanges();
			ConfigLoader.startSearchIndexer();
			ConfigLoader.startSummaryJob();
		} catch (Exception e) {
			throw new ForumStartupException("Error while starting JForum", e);
		}
	}

	public void init(ServletConfig config) throws ServletException {
		System.out.println("--> [JForumBaseServlet.init] ......");
		super.init(config);
		try {
			String appPath = config.getServletContext().getRealPath("");
			System.out.println("DEBUG: appPath = " + appPath);
			debug = "true".equals(config.getInitParameter("development"));
			// need to identify and locate the absolute path for log4j configure
			DOMConfigurator.configure(appPath + "/WEB-INF/log4j.xml");
			logger.info("Starting JForum. Debug mode is " + debug);
			ConfigLoader.startSystemglobals(appPath);
			ConfigLoader.startCacheEngine();
			// Configure the template engine
			Configuration templateCfg = new Configuration();
			if (!debug) {
				templateCfg.setTemplateUpdateDelay(3600);
			} else {
				templateCfg.setTemplateUpdateDelay(2);
			}
			templateCfg.setSetting("number_format", "#");
			templateCfg.setSharedVariable("startupTime", new Long(new Date().getTime()));
			// Create the default template loader
			String defaultPath = SystemGlobals.getApplicationPath() + "/templates";
			FileTemplateLoader defaultLoader = new FileTemplateLoader(new File(defaultPath));
			String extraTemplatePath = SystemGlobals.getValue(ConfigKeys.FREEMARKER_EXTRA_TEMPLATE_PATH);
			if (StringUtils.isNotBlank(extraTemplatePath)) {
				// An extra template path is configured, we need a MultiTemplateLoader
				FileTemplateLoader extraLoader = new FileTemplateLoader(new File(extraTemplatePath));
				TemplateLoader[] loaders = new TemplateLoader[] {
					extraLoader, defaultLoader
				};
				MultiTemplateLoader multiLoader = new MultiTemplateLoader(loaders);
				templateCfg.setTemplateLoader(multiLoader);
			} else {
				// An extra template path is not configured, we only need the default loader
				templateCfg.setTemplateLoader(defaultLoader);
				System.out.println("INFOR: finish to read all the files in Freemarker Template Folder from '/templates' ...");
			}
			ModulesRepository.init(SystemGlobals.getValue(ConfigKeys.CONFIG_DIR));
			loadConfigStuff();
			JForumExecutionContext.setTemplateConfig(templateCfg);
			System.out.println("INFOR: The 'templateCfg' is built ready in 'JForumExecutionContext' ...");
		} catch (Exception e) {
			throw new ForumStartupException("Error while starting JForum", e);
		}
	}

	protected void loadConfigStuff() {
		System.out.println("--> [JForumBaseServlet.loadConfigStuff] ......");
		ConfigLoader.loadUrlPatterns();
		I18n.load();
		Tpl.load(SystemGlobals.getValue(ConfigKeys.TEMPLATES_MAPPING));
		// BB Code
		BBCodeRepository.setBBCollection(new BBCodeHandler().parse());
	}
}
