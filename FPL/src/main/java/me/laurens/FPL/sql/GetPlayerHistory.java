package me.laurens.FPL.sql;

import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.sql.DataSource;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import me.laurens.FPL.Utils.HistoryPast;
import me.laurens.FPL.Utils.PlayerHistory;
import me.laurens.FPL.api.JsonReader;

public class GetPlayerHistory {

	private DataSource dataSource;
	HistoryPast historyPast;
	JsonReader jsonReader;
	Gson gson;

	private GetPlayers getPlayers;

	public GetPlayerHistory(DataSource dataSource, GetPlayers getPlayers) {
		this.dataSource = dataSource;
		this.getPlayers = getPlayers;

	}

	private Connection conn() throws SQLException {
		return dataSource.getConnection();
	}

	public void update() {
		ArrayList<Integer> players = getPlayers.getPlayers();
		gson = new Gson();

		for (int p : players) {

			try {
				jsonReader = new JsonReader("https://fantasy.premierleague.com/api/element-summary/" + p + "/");
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
			updateDatabase(p);

		}

		System.out.println("Updated Player History Table");
	}

	public void readJson() {

		try {

			historyPast = gson.fromJson(jsonReader.ReadAPIObject(), HistoryPast.class);	

		} catch (JsonSyntaxException | JsonIOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void updateDatabase(int code) {

		clearDatabase(code);
		readJson();

		if (historyPast == null) {
			return;
		}

		for (PlayerHistory h : historyPast.history_past) {
			if (h.season_name.equals("2020/21")) {
				try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
						"INSERT INTO player_history(id, start_cost, end_cost, total_points, minutes, ict_index) VALUES(?, ?, ?, ?, ?, ?);")) {
					statement.setInt(1, code);
					statement.setDouble(2, Double.parseDouble(h.start_cost));
					statement.setDouble(3, Double.parseDouble(h.end_cost));
					statement.setInt(4, Integer.parseInt(h.total_points));
					statement.setInt(5, Integer.parseInt(h.minutes));
					statement.setDouble(6, Double.parseDouble(h.ict_index));

					statement.executeUpdate();

				} catch (SQLException e) {
					e.printStackTrace();
				}

				break;
			}
		}
	}

	public void clearDatabase(int id) {
		try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
				"DELETE FROM player_history WHERE id=?;")) {
			statement.setInt(1, id);
			statement.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public boolean hasHistory(int id) {

		try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
				"SELECT id FROM player_history WHERE id=?;")) {
			statement.setInt(1, id);

			try (ResultSet results = statement.executeQuery()) {

				return results.next();

			}

		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}


	}

	public int maxPoints() {

		try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
				"SELECT total_points FROM player_history ORDER BY total_points DESC;")) {

			try (ResultSet results = statement.executeQuery()) {

				results.next();
				return results.getInt(1);

			}

		} catch (SQLException e) {
			e.printStackTrace();
			return 0;
		}
	}

	public double maxIct() {

		try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
				"SELECT ict_index FROM player_history ORDER BY ict_index DESC;")) {

			try (ResultSet results = statement.executeQuery()) {

				results.next();

				return results.getDouble(1);

			}

		} catch (SQLException e) {
			e.printStackTrace();
			return 0;
		}
	}

	public int maxMinutes() {

		try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
				"SELECT minutes FROM player_history ORDER BY minutes DESC;")) {

			try (ResultSet results = statement.executeQuery()) {

				results.next();

				return results.getInt(1);

			}

		} catch (SQLException e) {
			e.printStackTrace();
			return 0;
		}
	}

	public double getComp(int id, double max_points, double max_ict, double max_minutes) {

		try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
				"SELECT total_points, ict_index, minutes FROM player_history WHERE id=?;")) {
			statement.setInt(1, id);

			try (ResultSet results = statement.executeQuery()) {

				results.next();

				return ((results.getInt("total_points")/max_points) + (results.getDouble("ict_index")/max_ict) + (results.getInt("minutes")/max_minutes));

			}

		} catch (SQLException e) {
			e.printStackTrace();
			return 0;
		}

	}
}
