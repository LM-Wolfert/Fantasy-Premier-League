package me.laurens.FPL.sql;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import me.laurens.FPL.Utils.Events;
import me.laurens.FPL.Utils.Gameweek;

public class GetGameweeks {

	String path;
	DataSource dataSource;
	Events events;

	public GetGameweeks(String path, DataSource dataSource) {
		this.path = path;
		this.dataSource = dataSource;
	}

	private Connection conn() throws SQLException {
		return dataSource.getConnection();
	}

	public void readJson() {

		Gson gson = new Gson();

		try {

			events = gson.fromJson(new FileReader(path+"general.json"), Events.class);	

		} catch (JsonSyntaxException | JsonIOException | FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void updateDatabase() {

		clearDatabase();

		try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
				"INSERT INTO gameweeks(id, deadline, is_current, is_next) VALUES(?, ?, ?, ?);")) {

			for (Gameweek gw : events.events) {


				statement.setInt(1, Integer.parseInt(gw.id));
				statement.setLong(2, Long.parseLong(gw.deadline_time_epoch));
				statement.setBoolean(3, Boolean.parseBoolean(gw.is_current));
				statement.setBoolean(4, Boolean.parseBoolean(gw.is_next));

				statement.executeUpdate();

			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		System.out.println("Updated Gameweeks Table");

	}

	public void clearDatabase() {
		try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
				"DELETE FROM gameweeks;")) {

			statement.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}
