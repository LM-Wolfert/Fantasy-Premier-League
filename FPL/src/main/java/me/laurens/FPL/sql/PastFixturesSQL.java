package me.laurens.FPL.sql;

import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.sql.DataSource;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import me.laurens.FPL.Utils.History;
import me.laurens.FPL.Utils.PastFixtures;
import me.laurens.FPL.api.JsonReader;

public class PastFixturesSQL {

	Gson gson;
	JsonReader jsonReader;

	History history;

	private DataSource dataSource;

	public PastFixturesSQL(DataSource dataSource) {
		this.dataSource = dataSource;

	}

	private Connection conn() throws SQLException {
		return dataSource.getConnection();
	}

	public void getPastFixtures(ArrayList<Integer> players) {
		gson = new Gson();

		for (int p : players) {

			try {
				jsonReader = new JsonReader("https://fantasy.premierleague.com/api/element-summary/" + p + "/");
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
			setHistory(p);

		}

		System.out.println("Updated Past Fixtures Table");
	}

	public void readJson() {

		try {

			history = gson.fromJson(jsonReader.ReadAPIObject(), History.class);	

		} catch (JsonSyntaxException | JsonIOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void setHistory(int code) {

		readJson();

		if (history == null) {
			return;
		}

		try (Connection conn = conn();) {

			//Add any fixtures not yet added.
			for (PastFixtures pf : history.history) {

				if (!hasRow("SELECT id FROM past_fixtures WHERE id=" + code + " AND fixture=" + Integer.parseInt(pf.fixture) + ";")) {

					try (PreparedStatement statement = conn.prepareStatement("INSERT INTO past_fixtures(id,fixture,opponent,points,round,minutes) VALUES(?,?,?,?,?,?);")) {

						statement.setInt(1, code);
						statement.setInt(2, Integer.parseInt(pf.fixture));
						statement.setInt(3, Integer.parseInt(pf.opponent_team));
						statement.setInt(4, Integer.parseInt(pf.total_points));
						statement.setInt(5, Integer.parseInt(pf.round));
						statement.setInt(6, Integer.parseInt(pf.minutes));

						statement.executeUpdate();
						statement.close();

					}
				}		
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public boolean hasRow(String sql) {

		try (Connection conn = conn();
				PreparedStatement statement = conn.prepareStatement(sql);
				ResultSet results = statement.executeQuery()) {

			return results.next();

		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	//Get expected points for player based on past performances,
	//online count games where 60 or more minutes were played.
	//Weigh games based on how long ago there happened.
	public double getExpectedPoints(int id, int gameweek) {

		HashMap<Integer, Integer> map = getIntHashMap("SELECT round,points FROM past_fixtures WHERE id=" + id + " AND minutes>=60 ORDER BY round DESC;");

		double sum = 0;
		double points = 0;

		//Get sum of gameweek differences.
		for (int round : map.keySet()) {

			sum += 1.0/(gameweek - round);

		}

		//Get the weighted average of expected points.
		for (Entry<Integer, Integer> entry : map.entrySet()) {

			points += entry.getValue() * ((1.0/(gameweek - entry.getKey()))/sum);

		}

		return points;


	}

	public HashMap<Integer, Integer> getIntHashMap(String sql) {

		HashMap<Integer, Integer> map = new HashMap<>();

		try (Connection conn = conn();
				PreparedStatement statement = conn.prepareStatement(sql);
				ResultSet results = statement.executeQuery()) {

			while (results.next()) {

				map.put(results.getInt(1), results.getInt(2));

			}


		} catch (SQLException e) {
			e.printStackTrace();
		}

		return map;	

	}

	public double getExpectedMinutes(int id, int gameweek) {

		HashMap<Integer, Integer> map = getIntHashMap("SELECT round,minutes FROM past_fixtures WHERE id=" + id + " ORDER BY round DESC;");

		double sum = 0;
		double minutes = 0;

		//Get sum of gameweek differences.
		for (int round : map.keySet()) {

			sum += 1.0/(gameweek - round);

		}

		//Get the weighted average of expected points.
		for (Entry<Integer, Integer> entry : map.entrySet()) {

			minutes += entry.getValue() * ((1.0/(gameweek - entry.getKey()))/sum);

		}

		return minutes;




	}
}
