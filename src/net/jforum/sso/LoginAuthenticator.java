
package net.jforum.sso;

import java.util.Map;

import net.jforum.dao.UserDAO;
import net.jforum.entities.User;

public interface LoginAuthenticator {

	@SuppressWarnings("rawtypes")
	User validateLogin(String username, String password, Map extraParams);

	void setUserModel(UserDAO dao);
}
