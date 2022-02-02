package me.laurens.FPL.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import me.laurens.FPL.Utils.Time;

public class UserData {

	DataSource dataSource;

	public UserData(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	private Connection conn() throws SQLException {
		return dataSource.getConnection();
	}

	public void updateDatabase() {
		
		clearDatabase();

		try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
				"INSERT INTO user_data(last_update) VALUES(?);")) {
			statement.setLong(1, Time.currentTime());
			statement.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	public void clearDatabase() {
		
		try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
				"DELETE FROM user_data;")) {
			statement.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public long getTime() {
		
		try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
				"SELECT last_update FROM user_data;")) {
			ResultSet results = statement.executeQuery();
			
			if (results.next()) {
				return results.getLong(1);
			} else {
				return 0;
			}

		} catch (SQLException e) {
			e.printStackTrace();
			return 0;
		}
		
	}

}
