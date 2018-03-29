
package net.jforum.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DbUtils {

	public static void close(ResultSet rs, Statement st) {
		close(rs);
		close(st);
	}

	public static void close(ResultSet rs) {
		if (rs != null) {
			try {
				rs.close();
			} catch (Exception e) {

			}
		}
	}

	public static void close(Statement st) {
		if (st != null) {
			try {
				st.clearWarnings();
				st.close();
			} catch (SQLException e) {

			}
		}
	}
}
