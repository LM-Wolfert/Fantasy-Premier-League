package me.laurens.FPL.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import me.laurens.FPL.Utils.Time;

public class UserData {

	private DataSource dataSource;

	public UserData(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	private Connection conn() throws SQLException {
		return dataSource.getConnection();
	}

	public void updateDatabase() {

		if (exists()) {
			
			try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
					"UPDATE user_data SET last_update=?;")) {
				statement.setLong(1, Time.currentTime());
				statement.executeUpdate();

			} catch (SQLException e) {
				e.printStackTrace();
			}
			
		} else {

		try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
				"INSERT INTO user_data(last_update, money_remaining) VALUES(?,?);")) {
			statement.setLong(1, Time.currentTime());
			statement.setDouble(2, 0);
			statement.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		}

	}
	
	public boolean exists() {
		
		try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
				"SELECT last_update FROM user_data;")) {
			
			try (ResultSet results = statement.executeQuery()) {
				
				return results.next();
				
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
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

			try (ResultSet results = statement.executeQuery()) {

				if (results.next()) {
					return results.getLong(1);
				} else {
					return 0;
				}
			}

		} catch (SQLException e) {
			e.printStackTrace();
			return 0;
		}

	}

	public void setMoneyRemaining(int val) {

		try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
				"UPDATE user_data SET money_remaining=money_remaining+" + val + ";")) {
			statement.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	public int getMoneyRemaining() {

		try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
				"SELECT money_remaining FROM user_data;")) {

			try (ResultSet results = statement.executeQuery()) {

				results.next();

				return results.getInt(1);

			}


		} catch (SQLException e) {
			e.printStackTrace();
			return 0;
		}

	}

}
