
package net.jforum.dao.generic;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import net.jforum.entities.UserSession;
import net.jforum.exceptions.DatabaseException;
import net.jforum.util.DbUtils;
import net.jforum.util.preferences.SystemGlobals;

public class GenericUserSessionDAO implements net.jforum.dao.UserSessionDAO {

	public void add(UserSession us, Connection conn) {
		add(us, conn, false);
	}

	private void add(UserSession us, Connection conn, boolean checked) {
		System.out.println("--> [GenericUserSessionDAO.add] ......");
		System.out.println("DEBUG: checked = " + checked);
		if (!checked && selectById(us, conn) != null) {
			return;
		}
		PreparedStatement p = null;
		try {
			p = conn.prepareStatement(SystemGlobals.getSql("UserSessionModel.add"));
			System.out.println("INFOR: Calling SQL Query for 'UserSessionModel.add' ...");
			p.setString(1, us.getSessionId());
			p.setInt(2, us.getUserId());
			p.setTimestamp(3, new Timestamp(us.getStartTime().getTime()));
			p.executeUpdate();
		} catch (SQLException e) {
			throw new DatabaseException(e);
		} finally {
			DbUtils.close(p);
		}
	}

	public void update(UserSession us, Connection conn) {
		System.out.println("--> [GenericUserSessionDAO.update] ......");
		if (selectById(us, conn) == null) {
			add(us, conn, true);
			return;
		}
		PreparedStatement p = null;
		try {
			p = conn.prepareStatement(SystemGlobals.getSql("UserSessionModel.update"));
			System.out.println("INFOR: Calling SQL Query for 'UserSessionModel.update' ...");
			p.setTimestamp(1, new Timestamp(us.getStartTime().getTime()));
			p.setLong(2, us.getSessionTime());
			p.setString(3, us.getSessionId());
			p.setInt(4, us.getUserId());
			p.executeUpdate();
		} catch (SQLException e) {
			throw new DatabaseException(e);
		} finally {
			DbUtils.close(p);
		}
	}

	public UserSession selectById(UserSession us, Connection conn) {
		System.out.println("--> [GenericUserSessionDAO.selectById] ......");
		PreparedStatement p = null;
		ResultSet rs = null;
		try {
			p = conn.prepareStatement(SystemGlobals.getSql("UserSessionModel.selectById"));
			System.out.println("INFOR: Calling SQL Query for 'UserSessionModel.selectById' ...");
			p.setInt(1, us.getUserId());
			rs = p.executeQuery();
			boolean found = false;
			UserSession returnUs = new UserSession(us);
			if (rs.next()) {
				returnUs.setSessionTime(rs.getLong("session_time"));
				returnUs.setStartTime(rs.getTimestamp("session_start"));
				found = true;
			}
			return found ? returnUs : null;
		} catch (SQLException e) {
			throw new DatabaseException(e);
		} finally {
			DbUtils.close(rs, p);
		}
	}
}
