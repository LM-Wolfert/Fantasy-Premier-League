package me.laurens.FPL;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mysql.cj.jdbc.MysqlConnectionPoolDataSource;
import com.mysql.cj.jdbc.MysqlDataSource;

import me.laurens.FPL.Utils.Config;
import me.laurens.FPL.api.JsonReader;
import me.laurens.FPL.api.writeJson;
import me.laurens.FPL.sql.GetFixtures;
import me.laurens.FPL.sql.GetGameweeks;
import me.laurens.FPL.sql.GetPlayerHistory;
import me.laurens.FPL.sql.GetPlayers;
import me.laurens.FPL.sql.GetTeams;
import me.laurens.FPL.sql.SquadData;
import me.laurens.FPL.sql.UserData;
import me.laurens.FPL.GUI.Menu;

public class Main {

	public static void main(String[] args) {

		//Path to download data from the api
		String path = "C:\\Users\\Laurens\\Documents\\FPL\\";

		//Setup config
		Config config = new Config(path);
		
		//Update the data from the FPL api
		try {
			JsonReader jsonReaderGeneral = new JsonReader("https://fantasy.premierleague.com/api/bootstrap-static/");
			JsonObject jsonObject = jsonReaderGeneral.ReadAPIObject();

			writeJson.writeJsonObject(jsonObject, path+"general.json");
			
			JsonReader jsonReaderFixtures = new JsonReader("https://fantasy.premierleague.com/api/fixtures/");
			JsonArray jsonArray = jsonReaderFixtures.ReadAPIArray();

			writeJson.writeJsonArray(jsonArray, path+"fixtures.json");

		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		
		//Setup MySQL
		DataSource dataSource = null;
		GetPlayers getPlayers = null;
		GetTeams getTeams = null;
		GetFixtures getFixtures = null;
		GetGameweeks getGameweeks = null;
		GetPlayerHistory getPlayerHistory = null;
		UserData userData = null;
		SquadData squadData = null;
		
		try {
			dataSource = mysqlSetup(config);
			initDb(dataSource);

			getPlayers = new GetPlayers(path, dataSource);
			getTeams = new GetTeams(path, dataSource);
			getGameweeks = new GetGameweeks(path, dataSource);			
			getFixtures = new GetFixtures(path, dataSource);
			getPlayerHistory = new GetPlayerHistory(dataSource, getPlayers);
			userData = new UserData(dataSource);
			squadData = new SquadData(dataSource, getPlayers);
			
		} catch (SQLException | IOException e) {
			e.printStackTrace();
		}	

		//Create GUI
		Menu gui = new Menu(getPlayers, getTeams, getGameweeks, getFixtures, getPlayerHistory, userData, squadData);
	}

	//Creates the mysql connection.
	private static DataSource mysqlSetup(Config config) throws SQLException {

		MysqlDataSource dataSource = new MysqlConnectionPoolDataSource();

		dataSource.setServerName(config.getValue("db.host"));
		dataSource.setPortNumber(Integer.parseInt(config.getValue("db.port")));
		dataSource.setDatabaseName("fantasy_premier_league");
		dataSource.setUser(config.getValue("db.username"));
		dataSource.setPassword(config.getValue("db.password"));

		testDataSource(dataSource);
		return dataSource;

	}

	private static void testDataSource(DataSource dataSource) throws SQLException{
		try (Connection connection = dataSource.getConnection()) {
			if (!connection.isValid(1000)) {
				throw new SQLException("Could not establish database connection.");
			}
		}
	}

	private static void initDb(DataSource dataSource) throws SQLException, IOException {
		// first lets read our setup file.
		// This file contains statements to create our inital tables.
		// it is located in the resources.
		String setup;
		ClassLoader loader = Main.class.getClassLoader();
		try (InputStream in = loader.getResourceAsStream("dbsetup.sql")) {
			// Legacy way
			setup = new BufferedReader(new InputStreamReader(in)).lines().collect(Collectors.joining("\n"));
		} catch (IOException e) {
			System.out.println("Could not read db setup file.");
			e.printStackTrace();
			throw e;
		}
		// Mariadb can only handle a single query per statement. We need to split at ;.
		String[] queries = setup.split(";");
		// execute each query to the database.
		for (String query : queries) {
			// If you use the legacy way you have to check for empty queries here.
			if (query.trim().isEmpty()) continue;
			try (Connection conn = dataSource.getConnection();
					PreparedStatement stmt = conn.prepareStatement(query)) {
				stmt.execute();
			}
		}
		System.out.println("Database setup complete.");
	}

}
