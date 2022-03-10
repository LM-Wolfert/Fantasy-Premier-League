package me.laurens.FPL.sql;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.dbcp2.BasicDataSource;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import me.laurens.FPL.Utils.Elements;
import me.laurens.FPL.Utils.Events;
import me.laurens.FPL.Utils.Fixture;
import me.laurens.FPL.Utils.Gameweek;
import me.laurens.FPL.Utils.History;
import me.laurens.FPL.Utils.HistoryPast;
import me.laurens.FPL.Utils.PastFixtures;
import me.laurens.FPL.Utils.Player;
import me.laurens.FPL.Utils.PlayerHistory;
import me.laurens.FPL.Utils.Team;
import me.laurens.FPL.Utils.Teams;
import me.laurens.FPL.api.JsonReader;

public class FPLSQL {

	public BasicDataSource dataSource;

	private String path;

	private List<Fixture> fixtures;
	private Events events;
	private HistoryPast historyPast;
	private History history;
	private Elements elements;
	private Teams teams;

	private Gson gson;
	private JsonReader jsonReader;

	public FPLSQL(BasicDataSource dataSource, String path) {

		this.path = path;
		this.dataSource = dataSource;

		gson = new Gson();

	}

	private Connection conn() throws SQLException {

		return dataSource.getConnection();

	}

	public void readFixtures() {

		try {

			fixtures = gson.fromJson(new InputStreamReader(new FileInputStream(path+"fixtures.json"), StandardCharsets.UTF_8), new TypeToken<List<Fixture>>() {}.getType());	

		} catch (JsonSyntaxException | JsonIOException | FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void updateFixtures() {

		clearFixtures();

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

				statement.addBatch();

			}

			statement.executeBatch();

		} catch (SQLException e) {
			e.printStackTrace();
		}

		System.out.println("Updated Fixtures Table");

	}

	public void clearFixtures() {
		try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
				"DELETE FROM fixtures;")) {

			statement.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public ArrayList<Integer> opponentTeams(int teamID, int gameweek) {

		ArrayList<Integer> list = new ArrayList<>();

		try (Connection conn = conn();
				PreparedStatement statement = conn.prepareStatement(
						"SELECT away,home FROM fixtures WHERE event=" + gameweek + " AND (home=" + teamID + " OR away=" + teamID + ");");
				ResultSet results = statement.executeQuery()) {

			while (results.next()) {

				if (results.getInt("away") == teamID) {

					list.add(results.getInt("home"));

				} else {

					list.add(results.getInt("away"));

				}
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return list;

	}

	public void readGameweeks() {

		try {

			events = gson.fromJson(new InputStreamReader(new FileInputStream(path+"general.json"), StandardCharsets.UTF_8), Events.class);	

		} catch (JsonSyntaxException | JsonIOException | FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void updateGameweeks() {

		clearGameweeks();

		try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
				"INSERT INTO gameweeks(id, deadline, is_current, is_next) VALUES(?, ?, ?, ?);")) {

			for (Gameweek gw : events.events) {


				statement.setInt(1, Integer.parseInt(gw.id));
				statement.setLong(2, Long.parseLong(gw.deadline_time_epoch));
				statement.setBoolean(3, Boolean.parseBoolean(gw.is_current));
				statement.setBoolean(4, Boolean.parseBoolean(gw.is_next));

				statement.addBatch();

			}

			statement.executeBatch();

		} catch (SQLException e) {
			e.printStackTrace();
		}

		System.out.println("Updated Gameweeks Table");

	}

	public void clearGameweeks() {
		try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
				"DELETE FROM gameweeks;")) {

			statement.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void updatePlayerHistory() {

		ArrayList<Integer> players = getPlayers();

		for (int p : players) {

			try {
				jsonReader = new JsonReader("https://fantasy.premierleague.com/api/element-summary/" + p + "/");
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
			updatePlayerHistory(p);

		}

		System.out.println("Updated Player History Table");
	}

	public void readPlayerHistory() {

		try {

			historyPast = gson.fromJson(jsonReader.ReadAPIObject(), HistoryPast.class);	

		} catch (JsonSyntaxException | JsonIOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void updatePlayerHistory(int code) {

		clearPlayerHistory(code);
		readPlayerHistory();

		if (historyPast == null) {
			return;
		}

		try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
				"INSERT INTO player_history(id, start_cost, end_cost, total_points, minutes, ict_index) VALUES(?, ?, ?, ?, ?, ?);")) {

			for (PlayerHistory h : historyPast.history_past) {
				if (h.season_name.equals("2020/21")) {

					statement.setInt(1, code);
					statement.setDouble(2, Double.parseDouble(h.start_cost));
					statement.setDouble(3, Double.parseDouble(h.end_cost));
					statement.setInt(4, Integer.parseInt(h.total_points));
					statement.setInt(5, Integer.parseInt(h.minutes));
					statement.setDouble(6, Double.parseDouble(h.ict_index));

					statement.addBatch();

					break;
				}
			}

			statement.executeBatch();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void clearPlayerHistory(int id) {
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
				"SELECT id FROM player_history WHERE id=" + id +";");
				ResultSet results = statement.executeQuery()) {

			return results.next();

		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}


	}

	public double getCompHistory(int id, double max_points, double max_ict, double max_minutes) {

		try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
				"SELECT total_points, ict_index, minutes FROM player_history WHERE id=" + id+";");
				ResultSet results = statement.executeQuery()) {

			results.next();

			return ((results.getInt("total_points")/max_points) + (results.getDouble("ict_index")/max_ict) + (results.getInt("minutes")/max_minutes));

		} catch (SQLException e) {
			e.printStackTrace();
			return 0;
		}

	}

	public void readPlayers() {

		Gson gson = new Gson();

		try {

			elements = gson.fromJson(new InputStreamReader(new FileInputStream(path+"general.json"), StandardCharsets.UTF_8), new TypeToken<Elements>() { }.getType());

		} catch (JsonSyntaxException | JsonIOException | FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void updatePlayers() {

		clearPlayers();

		try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
				"INSERT INTO players(id, name, playChance_next, position, form, value_form, value_season, team_code, total_points, point_per_game, ep_next, cost, ict_index_rank, ict_index, minutes) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);")) {

			for (Player p : elements.elements) {

				statement.setInt(1, Integer.parseInt(p.id));
				statement.setString(2, p.web_name);
				if (p.chance_of_playing_next_round ==  null) {
					statement.setInt(3, 100);
				} else {
					statement.setInt(3, Integer.parseInt(p.chance_of_playing_next_round));
				}
				statement.setInt(4, Integer.parseInt(p.element_type));
				statement.setDouble(5, Double.parseDouble(p.form));
				statement.setDouble(6, Double.parseDouble(p.value_form));
				statement.setDouble(7, Double.parseDouble(p.value_season));
				statement.setInt(8, Integer.parseInt(p.team_code));
				statement.setInt(9, Integer.parseInt(p.total_points));
				statement.setDouble(10, Double.parseDouble(p.points_per_game));
				statement.setDouble(11, Double.parseDouble(p.ep_next));
				statement.setInt(12, Integer.parseInt(p.now_cost));
				statement.setInt(13, Integer.parseInt(p.ict_index_rank));
				statement.setDouble(14, Double.parseDouble(p.ict_index));
				statement.setInt(15, Integer.parseInt(p.minutes));

				statement.addBatch();

			}

			statement.executeBatch();

		} catch (SQLException e) {
			e.printStackTrace();
		}

		System.out.println("Updated Players Table");

	}

	public void clearPlayers() {
		try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
				"DELETE FROM players;")) {

			statement.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public ArrayList<Integer> getPlayers() {

		ArrayList<Integer> players = new ArrayList<Integer>();

		try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
				"SELECT id FROM players;");
				ResultSet results = statement.executeQuery()) {

			while (results.next()) {
				players.add(results.getInt(1));
			}

			return players;

		} catch (SQLException e) {
			e.printStackTrace();
			return players;
		}

	}

	public double getIctComp(int id, double max_ict) {

		try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
				"SELECT ict_index FROM players WHERE id=" + id+";");
				ResultSet results = statement.executeQuery()) {

			results.next();

			return (results.getDouble(1)/max_ict);

		} catch (SQLException e) {
			e.printStackTrace();
			return 0;
		}		
	}

	public double getFormComp(int id, double max_form) {

		try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
				"SELECT form FROM players WHERE id=" + id+";");
				ResultSet results = statement.executeQuery()) {

			results.next();

			return (results.getDouble(1)/max_form);

		} catch (SQLException e) {
			e.printStackTrace();
			return 0;
		}
	}

	public int getPosition(int id) {

		try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
				"SELECT position FROM players WHERE id=" + id+ ";");
				ResultSet results = statement.executeQuery()) {

			results.next();

			return (results.getInt(1));

		} catch (SQLException e) {
			e.printStackTrace();
			return 0;
		}
	}

	public int getTeam(int id) {

		try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
				"SELECT team_code FROM players WHERE id=" + id+";");
				ResultSet results = statement.executeQuery()) {

			results.next();

			return (results.getInt(1));

		} catch (SQLException e) {
			e.printStackTrace();
			return 0;
		}
	}

	public int getCost(int id) {

		try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
				"SELECT cost FROM players WHERE id=" + id +";");
				ResultSet results = statement.executeQuery()) {

			results.next();

			return (results.getInt(1));

		} catch (SQLException e) {
			e.printStackTrace();
			return 0;
		}
	}

	public String[] getNames(int[] playerIDs) {

		String[] names = new String[playerIDs.length];
		int counter = 0;


		for (int i : playerIDs) {

			try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
					"SELECT name FROM players WHERE id=" + i + ";");
					ResultSet results = statement.executeQuery()) {

				results.next();

				names[counter] = results.getString(1);
				counter++;

			} catch (SQLException e) {
				e.printStackTrace();
			}

		}

		return names;

	}

	public int[] getTeams(int[] playerIDs) {

		int[] teams = new int[playerIDs.length];
		int counter = 0;

		for (int i : playerIDs) {

			try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
					"SELECT team_code FROM players WHERE id=" + i + ";");
					ResultSet results = statement.executeQuery()) {

				results.next();

				teams[counter] = results.getInt(1);
				counter++;

			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		return teams;

	}

	public int[] sortByPosition(int[] squad) {

		int[] sortedSquad = new int[15];

		for (int i = 0; i < 15; i++) {

			for (int j = 0; j < 15; j++) {

				if (squad[j] != 0) {

					if (i <= 1) {
						//Goalkeeper
						if (getPosition(squad[j]) == 1) {
							sortedSquad[i] = squad[j];
							squad[j] = 0;
							break;
						}
					} else if (i >=2 && i <= 6) {
						//Defender
						if (getPosition(squad[j]) == 2) {
							sortedSquad[i] = squad[j];
							squad[j] = 0;
							break;
						}
					} else if (i >=7 && i <= 11) {
						//Midfielder
						if (getPosition(squad[j]) == 3) {
							sortedSquad[i] = squad[j];
							squad[j] = 0;
							break;
						}
					} else if (i >= 12) {
						//Defender
						if (getPosition(squad[j]) == 4) {
							sortedSquad[i] = squad[j];
							squad[j] = 0;
							break;
						}
					}
				}				
			}
		}
		return sortedSquad;		
	}

	public double getPointsComp(int id, double max_points) {

		try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
				"SELECT point_per_game FROM players WHERE id="+ id +";");
				ResultSet results = statement.executeQuery()) {

			results.next();

			return (results.getDouble(1)/max_points);

		} catch (SQLException e) {
			e.printStackTrace();
			return 0;
		}
	}

	public double getMinutesComp(int id, double max_minutes) {

		try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
				"SELECT minutes FROM players WHERE id="+ id + ";");
				ResultSet results = statement.executeQuery()) {

			results.next();

			return (results.getDouble(1)/max_minutes);

		} catch (SQLException e) {
			e.printStackTrace();
			return 0;
		}
	}

	public double getEp(int id) {

		try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
				"SELECT ep_next FROM players WHERE id=" + id+ ";");
				ResultSet results = statement.executeQuery()) {

			results.next();

			return (results.getDouble(1));

		} catch (SQLException e) {
			e.printStackTrace();
			return 0;
		}
	}

	public int getPlayChance(int id) {

		try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
				"SELECT playChance_next FROM players WHERE id=" + id +";");
				ResultSet results = statement.executeQuery()) {

			results.next();

			return (results.getInt(1));

		} catch (SQLException e) {
			e.printStackTrace();
			return 0;
		}
	}

	public double getAveragePoints(int id) {

		try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
				"SELECT point_per_game FROM players WHERE id=" + id+ ";");
				ResultSet results = statement.executeQuery()) {

			results.next();

			return (results.getDouble(1));

		} catch (SQLException e) {
			e.printStackTrace();
			return 0;
		}
	}

	public int getInt(String sql) {

		try (Connection conn = conn();
				PreparedStatement statement = conn.prepareStatement(sql);
				ResultSet results = statement.executeQuery()) {

			if (results.next()) {

				return results.getInt(1);

			} else {

				return 0;

			}		

		} catch (SQLException e) {
			e.printStackTrace();
			return 0;			
		}		
	}

	public double getDouble(String sql) {

		try (Connection conn = conn();
				PreparedStatement statement = conn.prepareStatement(sql);
				ResultSet results = statement.executeQuery()) {

			if (results.next()) {

				return results.getDouble(1);

			} else {

				return 0;

			}		

		} catch (SQLException e) {
			e.printStackTrace();
			return 0;			
		}		
	}
	
	public long getLong(String sql) {

		try (Connection conn = conn();
				PreparedStatement statement = conn.prepareStatement(sql);
				ResultSet results = statement.executeQuery()) {

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

	public ArrayList<Integer> getTrimmedPlayers() {

		ArrayList<Integer> players = new ArrayList<>();

		try (Connection conn = conn();
				PreparedStatement statement = conn.prepareStatement("SELECT id FROM players WHERE total_points>0 AND minutes>0;");
				ResultSet results = statement.executeQuery()) {

			while (results.next()) {

				players.add(results.getInt(1));

			}

			return players;

		} catch (SQLException e) {
			e.printStackTrace();
			return players;
		}

	}

	public String getString(String sql) {

		try (Connection conn = conn();
				PreparedStatement statement = conn.prepareStatement(sql);
				ResultSet results = statement.executeQuery()) {

			if (results.next()) {

				return results.getString(1);

			} else {

				return null;

			}
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}			
	}

	public void readTeams() {

		Gson gson = new Gson();

		try {

			teams = gson.fromJson(new InputStreamReader(new FileInputStream(path+"general.json"), StandardCharsets.UTF_8), Teams.class);	

		} catch (JsonSyntaxException | JsonIOException | FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void updateTeams() {

		clearTeams();

		try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
				"INSERT INTO teams(code, id, name, strength) VALUES(?, ?, ?, ?);")) {

			for (Team t : teams.teams) {

				statement.setInt(1, Integer.parseInt(t.code));
				statement.setInt(2, Integer.parseInt(t.id));
				statement.setString(3, t.name);
				statement.setInt(4, Integer.parseInt(t.strength));

				statement.addBatch();

			}

			statement.executeBatch();

		} catch (SQLException e) {
			e.printStackTrace();
		}

		System.out.println("Updated Teams Table");

	}

	public void clearTeams() {
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
				"SELECT code FROM teams;");
				ResultSet results = statement.executeQuery()) {

			while (results.next()) {
				teams.add(results.getInt(1));
			}

			return teams;

		} catch (SQLException e) {
			e.printStackTrace();
			return teams;
		}
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

	public void readPastFixtures() {

		try {

			history = gson.fromJson(jsonReader.ReadAPIObject(), History.class);	

		} catch (JsonSyntaxException | JsonIOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void setHistory(int code) {

		readPastFixtures();

		if (history == null) {
			return;
		}

		try (Connection conn = conn();
				PreparedStatement statement = conn.prepareStatement("INSERT INTO past_fixtures(id,fixture,opponent,points,round,minutes) VALUES(?,?,?,?,?,?);")) {

			//Add any fixtures not yet added.
			for (PastFixtures pf : history.history) {

				if (!hasRow("SELECT id FROM past_fixtures WHERE id=" + code + " AND fixture=" + Integer.parseInt(pf.fixture) + ";")) {


					statement.setInt(1, code);
					statement.setInt(2, Integer.parseInt(pf.fixture));
					statement.setInt(3, Integer.parseInt(pf.opponent_team));
					statement.setInt(4, Integer.parseInt(pf.total_points));
					statement.setInt(5, Integer.parseInt(pf.round));
					statement.setInt(6, Integer.parseInt(pf.minutes));

					statement.addBatch();

				}		
			}

			statement.executeBatch();

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

	private boolean hasPosition(int position) {

		try (Connection conn = conn();
				PreparedStatement statement = conn
						.prepareStatement("SELECT position FROM squad_data WHERE position="+position+";");
				ResultSet results = statement.executeQuery()) {

			return results.next();

		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	public void setPosition(int position, int id, String name, int cost) {

		if (hasPosition(position)) {
			try (Connection conn = conn();
					PreparedStatement statement = conn
							.prepareStatement("UPDATE squad_data SET id=?, name=?, purchase_price=? WHERE position=?;")) {

				statement.setInt(1, id);
				statement.setString(2, name);
				statement.setInt(4, position);
				statement.setInt(3, cost);
				statement.executeUpdate();

			} catch (SQLException e) {
				e.printStackTrace();
			}
		} else {
			try (Connection conn = conn();
					PreparedStatement statement = conn
							.prepareStatement("INSERT INTO squad_data(position, id, name, purchase_price) VALUES(?,?,?.?);")) {

				statement.setInt(1, position);
				statement.setInt(2, id);
				statement.setString(3, name);
				statement.setInt(4, cost);
				statement.executeUpdate();

			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	public boolean hasSquad() {

		try (Connection conn = conn();
				PreparedStatement statement = conn.prepareStatement("SELECT position FROM squad_data;");
				ResultSet results = statement.executeQuery()) {

			return results.next();

		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	public String getName(int position) {

		try (Connection conn = conn();
				PreparedStatement statement = conn.prepareStatement("SELECT name FROM squad_data WHERE position=" + position + ";");
				ResultSet results = statement.executeQuery()) {

			results.next();
			return results.getString(1);

		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}

	public double value() {

		double sum = 0;

		try (Connection conn = conn();
				PreparedStatement statement = conn.prepareStatement("SELECT id FROM squad_data");
				ResultSet results = statement.executeQuery()) {

			while (results.next()) {

				sum += getCost(results.getInt(1));

			}

			return sum / 10;

		} catch (SQLException e) {
			e.printStackTrace();
			return 0;
		}

	}

	public int getId(int position) {

		try (Connection conn = conn();
				PreparedStatement statement = conn.prepareStatement("SELECT id FROM squad_data WHERE position=" + position + ";");
				ResultSet results = statement.executeQuery()) {

			results.next();
			return results.getInt(1);

		} catch (SQLException e) {
			e.printStackTrace();
			return 0;
		}
	}

	public int sellValues() {

		int sum = 0;

		try (Connection conn = conn();
				PreparedStatement statement = conn.prepareStatement("SELECT id, purchase_price FROM squad_data");
				ResultSet results = statement.executeQuery()) {

			while (results.next()) {

				//If the purchase price is greater than the current price then the sell price is the current price.
				//If the purchase price is less than the current price, calculate the sell price by current price - 50% of the profit (rounded to to nearest 0.1)
				if (results.getInt(2) < getCost(results.getInt(1))) {

					sum += getCost(results.getInt(1)) - Math.ceil((getCost(results.getInt(1)) - results.getInt(2))/2.0);

				} else {

					sum += getCost(results.getInt(1));

				}	
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return sum;


	}

	public double sellValue(int id) {

		try (Connection conn = conn();
				PreparedStatement statement = conn.prepareStatement("SELECT purchase_price FROM squad_data WHERE id=" + id + ";");
				ResultSet results = statement.executeQuery()) {

			results.next();

			//If the purchase price is greater than the current price then the sell price is the current price.
			//If the purchase price is less than the current price, calculate the sell price by current price - 50% of the profit (rounded to to nearest 0.1)
			if (results.getInt(1) < getCost(id)) {

				return (getCost(id) - Math.ceil((getCost(id) - results.getInt(1))/2.0));

			} else {

				return (getCost(results.getInt(1)));

			}

		} catch (SQLException e) {
			e.printStackTrace();
			return 0;
		}
	}

	public boolean inSquad(int id) {

		try (Connection conn = conn();
				PreparedStatement statement = conn.prepareStatement("SELECT id FROM squad_data WHERE id=" + id + ";");
				ResultSet results = statement.executeQuery()) {

			return results.next();

		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	public HashMap<Integer, Integer> getSquad() {

		HashMap<Integer, Integer> squad = new HashMap<>();

		try (Connection conn = conn();
				PreparedStatement statement = conn.prepareStatement("SELECT id, purchase_price FROM squad_data;");
				ResultSet results = statement.executeQuery()) {

			while (results.next()) {
				squad.put(results.getInt(1),results.getInt(2));
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return squad;
	}

	public boolean exists() {

		try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
				"SELECT last_update FROM user_data;");
				ResultSet results = statement.executeQuery()) {

			return results.next();

		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}

	}
	
	public boolean update(String sql) {
		
		try (Connection conn = conn();
				PreparedStatement statement = conn.prepareStatement(sql);) {
			
			int success = statement.executeUpdate();
			
			if (success > 1) {
				
				return true;
				
			} else {
				
				return false;
				
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		
		
	}
}
