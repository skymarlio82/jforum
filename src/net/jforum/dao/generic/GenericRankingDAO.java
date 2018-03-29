
package net.jforum.dao.generic;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import net.jforum.JForumExecutionContext;
import net.jforum.entities.Ranking;
import net.jforum.exceptions.DatabaseException;
import net.jforum.util.DbUtils;
import net.jforum.util.preferences.SystemGlobals;

public class GenericRankingDAO implements net.jforum.dao.RankingDAO {

	public Ranking selectById(int rankingId) {
		Ranking ranking = new Ranking();
		PreparedStatement p = null;
		ResultSet rs = null;
		try {
			p = JForumExecutionContext.getConnection().prepareStatement(SystemGlobals.getSql("RankingModel.selectById"));
			p.setInt(1, rankingId);
			rs = p.executeQuery();
			if (rs.next()) {
				ranking = buildRanking(rs);
			}
			return ranking;
		} catch (SQLException e) {
			throw new DatabaseException(e);
		} finally {
			DbUtils.close(rs, p);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public List selectAll() {
		System.out.println("--> [GenericRankingDAO.selectAll] ......");
		List l = new ArrayList();
		PreparedStatement p = null;
		ResultSet rs = null;
		try {
			System.out.println("INFOR: Calling SQL Query for 'RankingModel.selectAll' ...");
			p = JForumExecutionContext.getConnection().prepareStatement(SystemGlobals.getSql("RankingModel.selectAll"));
			rs = p.executeQuery();
			while (rs.next()) {
				Ranking ranking = buildRanking(rs);
				l.add(ranking);
			}
			System.out.println("DEBUG: the number of RankingModel.selectAll = " + l.size());
			return l;
		} catch (SQLException e) {
			throw new DatabaseException(e);
		} finally {
			DbUtils.close(rs, p);
		}
	}

	public void delete(int rankingId) {
		PreparedStatement p = null;
		try {
			p = JForumExecutionContext.getConnection().prepareStatement(SystemGlobals.getSql("RankingModel.delete"));
			p.setInt(1, rankingId);
			p.executeUpdate();
		} catch (SQLException e) {
			throw new DatabaseException(e);
		} finally {
			DbUtils.close(p);
		}
	}

	public void update(Ranking ranking) {
		PreparedStatement p = null;
		try {
			p = JForumExecutionContext.getConnection().prepareStatement(SystemGlobals.getSql("RankingModel.update"));
			p.setString(1, ranking.getTitle());
			p.setString(2, ranking.getImage());
			p.setInt(3, ranking.isSpecial() ? 1 : 0);
			p.setInt(4, ranking.getMin());
			p.setInt(5, ranking.getId());
			p.executeUpdate();
		} catch (SQLException e) {
			throw new DatabaseException(e);
		} finally {
			DbUtils.close(p);
		}
	}

	public void addNew(Ranking ranking) {
		PreparedStatement p = null;
		try {
			p = JForumExecutionContext.getConnection().prepareStatement(SystemGlobals.getSql("RankingModel.addNew"));
			p.setString(1, ranking.getTitle());
			p.setInt(2, ranking.getMin());
			p.setInt(3, ranking.isSpecial() ? 1 : 0);
			p.executeUpdate();
		} catch (SQLException e) {
			throw new DatabaseException(e);
		} finally {
			DbUtils.close(p);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public List selectSpecials() {
		List l = new ArrayList();
		PreparedStatement p = null;
		ResultSet rs = null;
		try {
			p = JForumExecutionContext.getConnection().prepareStatement(SystemGlobals.getSql("RankingModel.selectSpecials"));
			rs = p.executeQuery();
			while (rs.next()) {
				Ranking ranking = this.buildRanking(rs);
				l.add(ranking);
			}
		} catch (SQLException e) {
			throw new DatabaseException(e);
		} finally {
			DbUtils.close(rs, p);
		}
		return l;
	}

	private Ranking buildRanking(ResultSet rs) throws SQLException {
		Ranking ranking = new Ranking();
		ranking.setId(rs.getInt("rank_id"));
		ranking.setTitle(rs.getString("rank_title"));
		ranking.setImage(rs.getString("rank_image"));
		ranking.setMin(rs.getInt("rank_min"));
		ranking.setSpecial(rs.getInt("rank_special") == 1);
		return ranking;
	}
}
