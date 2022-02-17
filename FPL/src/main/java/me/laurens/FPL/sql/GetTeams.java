package me.laurens.FPL.sql;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.sql.DataSource;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import me.laurens.FPL.Utils.Team;
import me.laurens.FPL.Utils.Teams;

public class GetTeams {
	String path;
	private DataSource dataSource;
	Teams teams;

	public GetTeams(String path, DataSource dataSource) {
		this.path = path;
		this.dataSource = dataSource;
	}

	private Connection conn() throws SQLException {
		return dataSource.getConnection();
	}

	public void readJson() {

		Gson gson = new Gson();

		try {

			teams = gson.fromJson(new FileReader(path+"general.json"), Teams.class);	

		} catch (JsonSyntaxException | JsonIOException | FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void updateDatabase() {

		clearDatabase();

		try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
				"INSERT INTO teams(code, id, name, strength) VALUES(?, ?, ?, ?);")) {

			for (Team t : teams.teams) {

				statement.setInt(1, Integer.parseInt(t.code));
				statement.setInt(2, Integer.parseInt(t.id));
				statement.setString(3, t.name);
				statement.setInt(4, Integer.parseInt(t.strength));

				statement.executeUpdate();

			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		System.out.println("Updated Teams Table");

	}

	public void clearDatabase() {
		try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
				"DELETE FROM teams;")) {

			statement.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public ArrayList<Integer> getTeamIDs() {

		ArrayList<Integer> teams = new ArrayList<Integer>();
		try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
				"SELECT code FROM teams;")) {

			try (ResultSet results = statement.executeQuery()) {

				while (results.next()) {
					teams.add(results.getInt(1));
				}

				return teams;

			}

		} catch (SQLException e) {
			e.printStackTrace();
			return teams;
		}
	}

}
