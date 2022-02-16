package me.laurens.FPL.sql;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import me.laurens.FPL.Utils.Fixture;

public class GetFixtures {

	String path;
	DataSource dataSource;
	List<Fixture> fixtures;

	public GetFixtures(String path, DataSource dataSource) {
		this.path = path;
		this.dataSource = dataSource;
	}

	private Connection conn() throws SQLException {
		return dataSource.getConnection();
	}

	public void readJson() {

		Gson gson = new Gson();

		try {

			fixtures = gson.fromJson(new FileReader(path+"fixtures.json"), new TypeToken<List<Fixture>>() {}.getType());	

		} catch (JsonSyntaxException | JsonIOException | FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void updateDatabase() {

		clearDatabase();

		try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
				"INSERT INTO fixtures(event, id, away, home) VALUES(?, ?, ?, ?);")) {

			for (Fixture f : fixtures) {

				if (f.event == null) {
					continue;
				}

				statement.setInt(1, Integer.parseInt(f.event));
				statement.setInt(2, Integer.parseInt(f.id));
				statement.setInt(3, Integer.parseInt(f.team_a));
				statement.setInt(4, Integer.parseInt(f.team_h));

				statement.executeUpdate();

			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		System.out.println("Updated Fixtures Table");

	}

	public void clearDatabase() {
		try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
				"DELETE FROM fixtures;")) {

			statement.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}
