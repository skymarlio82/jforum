
package net.jforum.dao.generic;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import net.jforum.JForumExecutionContext;
import net.jforum.entities.Config;
import net.jforum.exceptions.DatabaseException;
import net.jforum.util.DbUtils;
import net.jforum.util.preferences.SystemGlobals;

public class GenericConfigDAO implements net.jforum.dao.ConfigDAO {

	public void insert(Config config) {
		PreparedStatement p = null;
		try {
			p = JForumExecutionContext.getConnection().prepareStatement(SystemGlobals.getSql("ConfigModel.insert"));
			p.setString(1, config.getName());
			p.setString(2, config.getValue());
			p.executeUpdate();
		} catch (SQLException e) {
			throw new DatabaseException(e);
		} finally {
			DbUtils.close(p);
		}
	}

	public void update(Config config) {
		PreparedStatement p = null;
		try {
			p = JForumExecutionContext.getConnection().prepareStatement(SystemGlobals.getSql("ConfigModel.update"));
			p.setString(1, config.getValue());
			p.setString(2, config.getName());
			p.executeUpdate();
		} catch (SQLException e) {
			throw new DatabaseException(e);
		} finally {
			DbUtils.close(p);
		}
	}

	public void delete(Config config) {
		PreparedStatement p = null;
		try {
			p = JForumExecutionContext.getConnection().prepareStatement(SystemGlobals.getSql("ConfigModel.delete"));
			p.setInt(1, config.getId());
			p.executeUpdate();
		} catch (SQLException e) {
			throw new DatabaseException(e);
		} finally {
			DbUtils.close(p);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public List selectAll() {
		List l = new ArrayList();
		PreparedStatement p = null;
		ResultSet rs = null;
		try {
			p = JForumExecutionContext.getConnection().prepareStatement(SystemGlobals.getSql("ConfigModel.selectAll"));
			rs = p.executeQuery();
			while (rs.next()) {
				l.add(this.makeConfig(rs));
			}
			return l;
		} catch (SQLException e) {
			throw new DatabaseException(e);
		} finally {
			DbUtils.close(rs, p);
		}
	}

	public Config selectByName(String name) {
		System.out.println("--> [GenericConfigDAO.selectByName] ......");
		System.out.println("DEBUG: name = " + name);
		PreparedStatement p = null;
		ResultSet rs = null;
		try {
			System.out.println("INFOR: Calling SQL Query for 'ConfigModel.selectByName' ...");
			p = JForumExecutionContext.getConnection().prepareStatement(SystemGlobals.getSql("ConfigModel.selectByName"));
			p.setString(1, name);
			rs = p.executeQuery();
			Config c = null;
			if (rs.next()) {
				c = makeConfig(rs);
			}
			return c;
		} catch (SQLException e) {
			throw new DatabaseException(e);
		} finally {
			DbUtils.close(rs, p);
		}
	}

	protected Config makeConfig(ResultSet rs) throws SQLException {
		Config c = new Config();
		c.setId(rs.getInt("config_id"));
		c.setName(rs.getString("config_name"));
		c.setValue(rs.getString("config_value"));
		return c;
	}
}
