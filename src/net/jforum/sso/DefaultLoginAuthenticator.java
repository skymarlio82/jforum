
package net.jforum.sso;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import net.jforum.JForumExecutionContext;
import net.jforum.dao.UserDAO;
import net.jforum.entities.User;
import net.jforum.exceptions.ForumException;
import net.jforum.util.DbUtils;
import net.jforum.util.MD5;
import net.jforum.util.preferences.SystemGlobals;

public class DefaultLoginAuthenticator implements LoginAuthenticator {

	private UserDAO userModel = null;

	public void setUserModel(UserDAO userModel) {
		this.userModel = userModel;
	}

	@SuppressWarnings("rawtypes")
	public User validateLogin(String username, String password, Map extraParams) {
		User user = null;
		ResultSet rs = null;
		PreparedStatement p = null;
		try {
			p = JForumExecutionContext.getConnection().prepareStatement(SystemGlobals.getSql("UserModel.login"));
			p.setString(1, username);
			p.setString(2, MD5.crypt(password));
			rs = p.executeQuery();
			if (rs.next() && rs.getInt("user_id") > 0) {
				user = userModel.selectById(rs.getInt("user_id"));
			}
		} catch (SQLException e) {
			throw new ForumException(e);
		} finally {
			DbUtils.close(rs, p);
		}
		if (user != null && !user.isDeleted() && (user.getActivationKey() == null || user.isActive())) {
			return user;
		}
		return null;
	}
}
