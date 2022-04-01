package me.laurens.FPL;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.stream.Collectors;

import org.apache.commons.dbcp2.BasicDataSource;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import me.laurens.FPL.GUI.Menu;
import me.laurens.FPL.Utils.Config;
import me.laurens.FPL.api.JsonReader;
import me.laurens.FPL.api.writeJson;
import me.laurens.FPL.sql.FPLSQL;

public class Main {

	public static void main(String[] args) {

		//Path to download data from the api
		String path = null;
		try {
			path = Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
			path = new File(path).getParentFile().getPath();
			path = path + "\\";
		} catch (URISyntaxException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		System.out.println(path);

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
		BasicDataSource dataSource = null;
		FPLSQL fplSQL = null;
		
		try {
			dataSource = mysqlSetup(config);
			initDb(dataSource);

			fplSQL = new FPLSQL(dataSource, path);
			
		} catch (SQLException | IOException e) {
			e.printStackTrace();
		}	
		
		/*
		double points;
		double minutes;
		for (int i : getPlayers.getTrimmedPlayers()) {
			
			minutes = pastFixturesSQL.getExpectedMinutes(i, 27);
			points = pastFixturesSQL.getExpectedPoints(i, 27);
			
			if (minutes > 60) {
				points *= getPlayers.getPlayChance(i)/100.0;
				System.out.println(getPlayers.getString("SELECT name FROM players WHERE id=" + i + ";") + " has " + points + " expected points.");
			} else if (minutes > 45) {
				points *= 0.75;
				points *= getPlayers.getPlayChance(i)/100.0;
				System.out.println(getPlayers.getString("SELECT name FROM players WHERE id=" + i + ";") + " has " + points + " expected points.");
			} else if (minutes > 30) {
				points *= 0.5;
				points *= getPlayers.getPlayChance(i)/100.0;
				System.out.println(getPlayers.getString("SELECT name FROM players WHERE id=" + i + ";") + " has " + points + " expected points.");
			} else {
				points *= 0;
				System.out.println(getPlayers.getString("SELECT name FROM players WHERE id=" + i + ";") + " has " + points + " expected points.");
			}
		}
		*/
		
		//Create GUI
		Menu gui = new Menu(fplSQL);
	}

	//Creates the mysql connection.
	private static BasicDataSource mysqlSetup(Config config) throws SQLException {

		BasicDataSource dataSource = new BasicDataSource();

		dataSource.setUrl("jdbc:mysql://" + config.getValue("db.host") + ":" + config.getValue("db.port") +  "/fantasy_premier_league?useUnicode=true&characterEncoding=UTF-8&useFastDateParsing=false");
		//dataSource.setServerName(config.getValue("db.host"));
		//dataSource.setPortNumber(Integer.parseInt(config.getValue("db.port")));
		//dataSource.setDatabaseName("fantasy_premier_league");
		dataSource.setUsername(config.getValue("db.username"));
		dataSource.setPassword(config.getValue("db.password"));
		
		dataSource.setMinIdle(5);
        dataSource.setMaxIdle(10);
        dataSource.setMaxOpenPreparedStatements(100);

		testDataSource(dataSource);
		return dataSource;

	}

	private static void testDataSource(BasicDataSource dataSource) throws SQLException{
		try (Connection connection = dataSource.getConnection()) {
			if (!connection.isValid(1000)) {
				throw new SQLException("Could not establish database connection.");
			}
		}
	}

	private static void initDb(BasicDataSource dataSource) throws SQLException, IOException {
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
