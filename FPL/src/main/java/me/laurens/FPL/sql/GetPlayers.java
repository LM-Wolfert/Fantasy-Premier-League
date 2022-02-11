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

import me.laurens.FPL.Utils.Elements;
import me.laurens.FPL.Utils.Player;

public class GetPlayers {

	String path;
	DataSource dataSource;
	Elements elements;

	public GetPlayers(String path, DataSource dataSource) {
		this.path = path;
		this.dataSource = dataSource;
	}

	private Connection conn() throws SQLException {
		return dataSource.getConnection();
	}

	public void readJson() {

		Gson gson = new Gson();

		try {

			elements = gson.fromJson(new FileReader(path+"general.json"), Elements.class);	

		} catch (JsonSyntaxException | JsonIOException | FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void updateDatabase() {

		clearDatabase();

		for (Player p : elements.elements) {

			try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
					"INSERT INTO players(id, name, playChance_next, position, form, value_form, value_season, team_code, total_points, point_per_game, ep_next, cost, ict_index_rank, ict_index, minutes) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);")) {
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

				statement.executeUpdate();

			} catch (SQLException e) {
				e.printStackTrace();
			}

		}

		System.out.println("Updated Players Table");

	}

	public void clearDatabase() {
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
				"SELECT id FROM players;")) {

			ResultSet results = statement.executeQuery();

			while (results.next()) {
				players.add(results.getInt(1));
			}

			return players;

		} catch (SQLException e) {
			e.printStackTrace();
			return players;
		}

	}

	public double maxIct() {

		try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
				"SELECT ict_index FROM players ORDER BY ict_index DESC;")) {

			ResultSet results = statement.executeQuery();

			results.next();

			return results.getDouble(1);

		} catch (SQLException e) {
			e.printStackTrace();
			return 0;
		}		
	}

	public double maxForm() {

		try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
				"SELECT form FROM players ORDER BY form DESC;")) {

			ResultSet results = statement.executeQuery();

			results.next();

			return results.getDouble(1);

		} catch (SQLException e) {
			e.printStackTrace();
			return 0;
		}
	}

	public double getIctComp(int id, double max_ict) {

		try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
				"SELECT ict_index FROM players WHERE id=?;")) {
			statement.setInt(1, id);

			ResultSet results = statement.executeQuery();

			results.next();

			return (results.getDouble(1)/max_ict);

		} catch (SQLException e) {
			e.printStackTrace();
			return 0;
		}		
	}

	public double getFormComp(int id, double max_form) {

		try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
				"SELECT form FROM players WHERE id=?;")) {
			statement.setInt(1, id);

			ResultSet results = statement.executeQuery();

			results.next();

			return (results.getDouble(1)/max_form);

		} catch (SQLException e) {
			e.printStackTrace();
			return 0;
		}
	}

	public int getPosition(int id) {

		try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
				"SELECT position FROM players WHERE id=?;")) {
			statement.setInt(1, id);

			ResultSet results = statement.executeQuery();

			results.next();

			return (results.getInt(1));

		} catch (SQLException e) {
			e.printStackTrace();
			return 0;
		}
	}

	public int getTeam(int id) {

		try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
				"SELECT team_code FROM players WHERE id=?;")) {
			statement.setInt(1, id);

			ResultSet results = statement.executeQuery();

			results.next();

			return (results.getInt(1));

		} catch (SQLException e) {
			e.printStackTrace();
			return 0;
		}
	}

	public int getCost(int id) {

		try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
				"SELECT cost FROM players WHERE id=?;")) {
			statement.setInt(1, id);

			ResultSet results = statement.executeQuery();

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
					"SELECT name FROM players WHERE id=?;")) {
				statement.setInt(1, i);

				ResultSet results = statement.executeQuery();

				results.next();

				names[counter] = results.getString(1);
				counter++;

			} catch (SQLException e) {
				e.printStackTrace();
				return names;
			}

		}

		return names;

	}

	public int[] getTeams(int[] playerIDs) {

		int[] teams = new int[playerIDs.length];
		int counter = 0;

		for (int i : playerIDs) {

			try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
					"SELECT team_code FROM players WHERE id=?;")) {
				statement.setInt(1, i);

				ResultSet results = statement.executeQuery();

				results.next();

				teams[counter] = results.getInt(1);
				counter++;

			} catch (SQLException e) {
				e.printStackTrace();
				return teams;
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
	
	public double maxPoints() {

		try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
				"SELECT point_per_game FROM players ORDER BY point_per_game DESC;")) {

			ResultSet results = statement.executeQuery();

			results.next();

			return results.getDouble(1);

		} catch (SQLException e) {
			e.printStackTrace();
			return 0;
		}
	}
	
	public double maxMinutes() {

		try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
				"SELECT minutes FROM players ORDER BY minutes DESC;")) {

			ResultSet results = statement.executeQuery();

			results.next();

			return results.getDouble(1);

		} catch (SQLException e) {
			e.printStackTrace();
			return 0;
		}
	}
	
	public double getPointsComp(int id, double max_points) {

		try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
				"SELECT point_per_game FROM players WHERE id=?;")) {
			statement.setInt(1, id);

			ResultSet results = statement.executeQuery();

			results.next();

			return (results.getDouble(1)/max_points);

		} catch (SQLException e) {
			e.printStackTrace();
			return 0;
		}
	}
	
	public double getMinutesComp(int id, double max_minutes) {

		try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
				"SELECT minutes FROM players WHERE id=?;")) {
			statement.setInt(1, id);

			ResultSet results = statement.executeQuery();

			results.next();

			return (results.getDouble(1)/max_minutes);

		} catch (SQLException e) {
			e.printStackTrace();
			return 0;
		}
	}
	
	public double getEp(int id) {

		try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
				"SELECT ep_next FROM players WHERE id=?;")) {
			statement.setInt(1, id);

			ResultSet results = statement.executeQuery();

			results.next();

			return (results.getDouble(1));

		} catch (SQLException e) {
			e.printStackTrace();
			return 0;
		}
	}
	
	public int getPlayChance(int id) {

		try (Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
				"SELECT playChance_next FROM players WHERE id=?;")) {
			statement.setInt(1, id);

			ResultSet results = statement.executeQuery();

			results.next();

			return (results.getInt(1));

		} catch (SQLException e) {
			e.printStackTrace();
			return 0;
		}
	}
}
