
package net.jforum;

import java.sql.Connection;

import org.apache.log4j.Logger;

import net.jforum.dao.CategoryDAO;
import net.jforum.dao.ConfigDAO;
import net.jforum.dao.DataAccessDriver;
import net.jforum.dao.ForumDAO;
import net.jforum.exceptions.DatabaseException;
import net.jforum.exceptions.RepositoryStartupException;
import net.jforum.repository.ForumRepository;

public class ForumStartup {

	private final static Logger log = Logger.getLogger(ForumStartup.class);

	public static boolean startDatabase() {
		System.out.println("--> [ForumStartup.startDatabase] ......");
		try {
			if (DBConnection.createInstance()) {
				DBConnection.getImplementation().init();
				// Check if we're in fact up and running
				Connection conn = DBConnection.getImplementation().getConnection();
				DBConnection.getImplementation().releaseConnection(conn);
			}
		} catch (Exception e) {
			throw new DatabaseException("Error while trying to start the database: " + e, e);
		}
		return true;
	}

	public static void startForumRepository() {
		System.out.println("--> [ForumStartup.startForumRepository] ......");
		try {
			ForumDAO fm = DataAccessDriver.getInstance().newForumDAO();
			System.out.println("INFOR: the Instance of ForumDAO is created by 'GenericForumDAO'");
			CategoryDAO cm = DataAccessDriver.getInstance().newCategoryDAO();
			System.out.println("INFOR: the Instance of CategoryDAO is created by 'GenericCategoryDAO'");
			ConfigDAO configModel = DataAccessDriver.getInstance().newConfigDAO();
			System.out.println("INFOR: the Instance of ConfigDAO is created by 'GenericConfigDAO'");
			ForumRepository.start(fm, cm, configModel);
		} catch (Exception e) {
			log.error("Unable to bootstrap JForum repository.", e);
			throw new RepositoryStartupException("Error while trying to start ForumRepository: " + e, e);
		}
	}
}
