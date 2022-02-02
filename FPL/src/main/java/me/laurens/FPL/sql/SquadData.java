package me.laurens.FPL.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

public class SquadData {
	
	DataSource dataSource;
	GetPlayers getPlayers;

	public SquadData(DataSource dataSource, GetPlayers getPlayers) {
		this.dataSource = dataSource;
		this.getPlayers = getPlayers;
	}

	private Connection conn() throws SQLException {
		return dataSource.getConnection();
	}
	
	private boolean hasPosition(int position) {
		
		try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
				"SELECT position FROM squad_data WHERE position=?")) {
			statement.setInt(1,position);
			
			ResultSet results = statement.executeQuery();
			return results.next();

		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	public void setPosition(int position, int id, String name) {

		if (hasPosition(position)) {
			try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
					"UPDATE squad_data SET id=?, name=? WHERE position=?;")) {
				statement.setInt(1, id);
				statement.setString(2, name);
				statement.setInt(3, position);
				statement.executeUpdate();

			} catch (SQLException e) {
				e.printStackTrace();
			}
		} else {
			try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
					"INSERT INTO squad_data(position, id, name) VALUES(?,?,?);")) {
				statement.setInt(1, position);
				statement.setInt(2, id);
				statement.setString(3, name);
				statement.executeUpdate();

			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	
	public boolean hasSquad() {
		
		try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
				"SELECT position FROM squad_data;")) {

			ResultSet results = statement.executeQuery();
			return results.next();

		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public String getName(int position) {
		
		try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
				"SELECT name FROM squad_data WHERE position=?;")) {
			statement.setInt(1, position);
			
			ResultSet results = statement.executeQuery();
			results.next();
			return results.getString(1);

		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public double value() {
		
		double sum = 0;
		
		try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
				"SELECT id FROM squad_data")) {
			
			ResultSet results = statement.executeQuery();
			
			while (results.next()) {
				
				sum += getPlayers.getCost(results.getInt(1));
				
			}
			
			return sum/10;

		} catch (SQLException e) {
			e.printStackTrace();
			return 0;
		}
		
	}
}
