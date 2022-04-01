package me.laurens.FPL.GUI;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import me.laurens.FPL.Utils.Time;
import me.laurens.FPL.optimisation.PlayerValueCurrent;
import me.laurens.FPL.optimisation.PlayerValueInitial;
import me.laurens.FPL.optimisation.SquadSolver;
import me.laurens.FPL.optimisation.SquadUpdate;
import me.laurens.FPL.optimisation.TeamSelection;
import me.laurens.FPL.sql.FPLSQL;

public class Menu {

	private JLabel pitchLabel;

	private JLabel squadValueLabel;
	private JLabel moneyRemainingLabel;
	private JLabel expectedPointsLabel;
	private JLabel averagePointsLabel;
	private JLabel tripleCaptainLabel;
	private JLabel benchBoostLabel;

	private JPanel infoPanel;

	private JButton getSquadButton;
	private JButton updateSquadButton;
	private JButton updateSquadButtonW;
	private JButton updateDatabaseButton;
	private JButton updatePlayerHistoryButton;
	private JButton updatePastFixturesButton;

	private ImageIcon pitchIcon;
	private ImageIcon icon;

	private JTextField[] playerLabels;
	private JTextField[] teamLabels;

	private JDesktopPane desktopPane;

	private JTable squadUpdateTable;
	private JScrollPane scrollPane;

	private int height;
	private int width;

	private double minutes;
	private double points;
	private double expPoints;
	
	private double hist_max_minutes;
	private double standard_minutes;

	private int teamID;
	private ArrayList<Integer> opponentTeams;
	private double strengthDiff;

	private FPLSQL fplSQL;

	private int gameweek;
	private double strength;

	private String label;

	public Menu(FPLSQL fplSQL) {

		this.fplSQL = fplSQL;

		gameweek = fplSQL.getInt("SELECT id FROM gameweeks WHERE is_next=1;");
		strength = fplSQL.getInt("SELECT SUM(strength) FROM teams;")/20.0;
		
		hist_max_minutes = fplSQL.getDouble("SELECT minutes FROM player_history ORDER BY minutes DESC;");

		pitchIcon = new ImageIcon(getClass().getClassLoader().getResource("pitch.jpg"));

		JFrame frame = new JFrame();

		frame.setPreferredSize(new Dimension(1280,720));
		width = 1280;
		height = 720;
		frame.setMinimumSize(new Dimension(960, 540));

		frame.addComponentListener(new ComponentAdapter() {

			public void componentResized(ComponentEvent ce) {

				if (frame.getHeight() != height || frame.getWidth() != width) {
					height = frame.getHeight();
					width = frame.getWidth();

					resize();
				}
			}

		});

		desktopPane = new JDesktopPane();	
		
		squadUpdateTable = new JTable();
		squadUpdateTable.setFont(new Font("Serif", Font.BOLD, 16));

		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment( JLabel.CENTER );

		squadUpdateTable.setDefaultRenderer(String.class, centerRenderer);

		squadUpdateTable.setVisible(true);

		scrollPane = new JScrollPane(squadUpdateTable);
		squadUpdateTable.setFillsViewportHeight(true);

		scrollPane.setBounds((int) (0/(double)1280*width), (int) (192/(double)720*height), (int) (588/(double)1280*width), (int) (412/(double)720*height));
		desktopPane.add(scrollPane);

		pitchLabel = new JLabel();
		pitchLabel.setBounds((int) (856/(double)1280*width), 0, (int) (410/(double)1280*width), (int) (604/(double)720*height));
		scaleImage();

		desktopPane.add(pitchLabel);

		getSquadButton = new JButton("Get Initial Squad");
		getSquadButton.setFont(new Font("Serif", Font.BOLD, 16));
		getSquadButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				ArrayList<PlayerValueInitial> list = new ArrayList<>();
				ArrayList<Integer> player_ids = fplSQL.getPlayers();

				double max_ict = fplSQL.getDouble("SELECT ict_index FROM players ORDER BY ict_index DESC;");
				double max_form = fplSQL.getDouble("SELECT form FROM players ORDER BY form DESC;");
				double max_points = fplSQL.getDouble("SELECT point_per_game FROM players ORDER BY point_per_game DESC;");
				double max_minutes = fplSQL.getDouble("SELECT minutes FROM players ORDER BY minutes DESC;");

				double hist_max_points = fplSQL.getDouble("SELECT total_points FROM player_history ORDER BY total_points DESC;");
				double hist_max_ict = fplSQL.getDouble("SELECT ict_index FROM player_history ORDER BY ict_index DESC;");
				hist_max_minutes = fplSQL.getDouble("SELECT minutes FROM player_history ORDER BY minutes DESC;");

				double history;
				double ict_comp;
				double form_comp;
				double points_comp;
				double minutes_comp;

				for (int id : player_ids) {

					if (fplSQL.hasHistory(id)) {

						history = fplSQL.getCompHistory(id, hist_max_points, hist_max_ict, hist_max_minutes);

					} else {

						history = 1;

					}

					ict_comp = fplSQL.getIctComp(id, max_ict);
					form_comp = fplSQL.getFormComp(id, max_form);
					points_comp = fplSQL.getPointsComp(id, max_points);
					minutes_comp = fplSQL.getMinutesComp(id, max_minutes);

					list.add(new PlayerValueInitial(id, fplSQL.getPosition(id), fplSQL.getTeam(id), fplSQL.getCost(id), ict_comp, form_comp, history, points_comp, minutes_comp));					

				}

				ArrayList<Integer> teams = fplSQL.getTeamIDs();

				SquadSolver solver = new SquadSolver(list, teams);
				solver.setup();
				solver.solve();

				int[] squad = solver.squad;
				squad = fplSQL.sortByPosition(squad);
				String[] names = fplSQL.getNames(squad);

				for (int i = 0; i < 15; i++) {
					fplSQL.setPosition(i, squad[i], names[i], fplSQL.getCost(squad[i]));
				}

				int newValue = fplSQL.sellValues();
				
				if (fplSQL.hasRow("SELECT data FROM user_data WHERE data='money_remaining';")) {
					
					fplSQL.update("UPDATE user_data SET value = " + (1000 - newValue) + " WHERE data='money_remaining';");

				} else {

					fplSQL.update("INSERT INTO user_data(data, value) VALUES('money_remaining', " + (1000 - newValue) + ");");
				}
				moneyRemainingLabel.setText("Money Remaining: " + fplSQL.getLong("SELECT value FROM user_data WHERE data='money_remaining';")/10.0);
				squadValueLabel.setText("Squad Value: " + fplSQL.value());

				showSquad();
				showTeam();
				showSquadUpdate();

				//Expected Points
				//Get the expected points for the current team;
				PlayerValueCurrent[] players = new PlayerValueCurrent[15];
				int[] currentSquad = new int[15];
				int id;


				for (int i = 0; i < 15; i++) {

					id = fplSQL.getId(i);
					currentSquad[i] = id;

					points = expectedPoints(id);

					players[i] = new PlayerValueCurrent(id, fplSQL.getPosition(id), fplSQL.getTeam(id), fplSQL.getCost(id), points);

				}

				TeamSelection teamSelection = new TeamSelection(players, fplSQL);
				teamSelection.solve();
				points = teamSelection.expectedPoints(0);

				expectedPointsLabel.setText("Expected Points: " + String.format("%.2f", points));

				//Average Points
				points = 0;

				for (int i = 0; i < 15; i++) {

					id = fplSQL.getId(i);
					points += fplSQL.getAveragePoints(id);

				}

				averagePointsLabel.setText("Average Points: " + String.format("%.2f", points));

				//Triple Captain
				points = 0;

				for (int i = 0; i < 15; i++) {

					id = fplSQL.getId(i);
					if (expectedPoints(id) > points) {

						points = expectedPoints(id);

					}

				}

				tripleCaptainLabel.setText("Triple Captain: " + String.format("%.2f", points));

				//Bench Boost
				points = teamSelection.benchPoints();
				benchBoostLabel.setText("Bench Boost: " + String.format("%.2f", points));

			}
		});
		getSquadButton.setBounds(294, 0, 294, 64);
		desktopPane.add(getSquadButton);

		if (fplSQL.hasSquad()) {
			showSquad();
			showTeam();
		}

		updateSquadButton = new JButton("Update Current Squad");
		updateSquadButton.setFont(new Font("Serif", Font.BOLD, 16));
		updateSquadButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				int id;
				double points;

				//Get the expected points for the current team;
				PlayerValueCurrent[] players = new PlayerValueCurrent[15];
				int[] currentSquad = new int[15];

				for (int i = 0; i < 15; i++) {

					id = fplSQL.getId(i);
					currentSquad[i] = id;

					points = expectedPoints(id);

					players[i] = new PlayerValueCurrent(id, fplSQL.getPosition(id), fplSQL.getTeam(id), fplSQL.getCost(id), points);

				}

				TeamSelection teamSelection = new TeamSelection(players, fplSQL);
				teamSelection.solve();
				double expectedPointsCurrent = teamSelection.expectedPoints(0);		

				ArrayList<PlayerValueCurrent> list = new ArrayList<>();
				ArrayList<Integer> player_ids = fplSQL.getPlayers();

				for (int id1 : player_ids) {

					points = expectedPoints(id1);

					list.add(new PlayerValueCurrent(id1, fplSQL.getPosition(id1), fplSQL.getTeam(id1), fplSQL.getCost(id1), points));					

				}

				ArrayList<Integer> teams = fplSQL.getTeamIDs();

				SquadUpdate solver = new SquadUpdate(list, teams, fplSQL);

				//Test optimal squad for up to 11 transfers.
				//More than 11 transfers would be counter-intuitive since it would mean the bench is transferred also.
				boolean changed = false;
				String[] names = new String[15];
				for (int i = 1; i <= 15; i++) {

					solver.setTransferCount(i);
					solver.setup();
					solver.solve();


					int[] squad = solver.squad;
					squad = fplSQL.sortByPosition(squad);
					names = fplSQL.getNames(squad);

					for (int j = 0; j < 15; j++) {

						id = squad[j];

						points = expectedPoints(id);

						players[j] = new PlayerValueCurrent(id, fplSQL.getPosition(id), fplSQL.getTeam(id), fplSQL.getCost(id), points);

					}

					teamSelection = new TeamSelection(players, fplSQL);
					teamSelection.solve();

					//For each transfer over 1 add a modifier of -4 points.
					int modifier = (i-1) * -4;
					double expectedPoints = teamSelection.expectedPoints(modifier);

					if (expectedPoints > expectedPointsCurrent) {
						//Update current value.
						expectedPointsCurrent = expectedPoints;
						changed = true;
						//Update current squad.
						for (int h = 0; h < 15; h++) {

							currentSquad[h] = squad[h];
						}
					}

				}
				if (!changed) {

				} else {

					HashMap<Integer,Integer> existingSquad = fplSQL.getSquad();

					int currentValue = fplSQL.sellValues();

					names = fplSQL.getNames(currentSquad);


					for (int i = 0; i < 15; i++) {					

						if (existingSquad.containsKey(currentSquad[i])) {

							fplSQL.setPosition(i, currentSquad[i], names[i], existingSquad.get(currentSquad[i]));

						} else {

							fplSQL.setPosition(i, currentSquad[i], names[i], fplSQL.getCost(currentSquad[i]));

						}
					}

					int newValue = fplSQL.sellValues();
					if (fplSQL.hasRow("SELECT data FROM user_data WHERE data='money_remaining';")) {
						
						fplSQL.update("UPDATE user_data SET value = value+" + (currentValue - newValue) + " WHERE data='money_remaining';");

					} else {

						fplSQL.update("INSERT INTO user_data(data, value) VALUES('money_remaining', " + (1000 - newValue) + ");");

					}
					moneyRemainingLabel.setText("Money Remaining: " + fplSQL.getLong("SELECT value FROM user_data WHERE data='money_remaining';")/10.0);
					squadValueLabel.setText("Squad Value: " + fplSQL.value());
					
					showSquad();
					showTeam();
					showSquadUpdate();

					//Expected Points
					//Get the expected points for the current team;
					players = new PlayerValueCurrent[15];
					currentSquad = new int[15];
					int[] currentSquadW = new int[15];


					for (int i = 0; i < 15; i++) {

						id = fplSQL.getId(i);
						currentSquad[i] = id;
						currentSquadW[i] = id;

						points = expectedPoints(id);

						players[i] = new PlayerValueCurrent(id, fplSQL.getPosition(id), fplSQL.getTeam(id), fplSQL.getCost(id), points);

					}

					teamSelection = new TeamSelection(players, fplSQL);
					teamSelection.solve();
					points = teamSelection.expectedPoints(0);

					expectedPointsLabel.setText("Expected Points: " + String.format("%.2f", points));

					//Average Points
					points = 0;

					for (int i = 0; i < 15; i++) {

						id = fplSQL.getId(i);
						points += fplSQL.getAveragePoints(id);

					}

					averagePointsLabel.setText("Average Points: " + String.format("%.2f", points));

					//Triple Captain
					points = 0;

					for (int i = 0; i < 15; i++) {

						id = fplSQL.getId(i);
						if (expectedPoints(id) > points) {

							points = expectedPoints(id);

						}

					}

					tripleCaptainLabel.setText("Triple Captain: " + String.format("%.2f", points));

					//Bench Boost
					points = teamSelection.benchPoints();
					benchBoostLabel.setText("Bench Boost: " + String.format("%.2f", points));

				}

			}
		});
		updateSquadButton.setBounds(294, 64, 294, 64);
		desktopPane.add(updateSquadButton);

		updateSquadButtonW = new JButton("Update Current Squad - Wildcard");
		updateSquadButtonW.setFont(new Font("Serif", Font.BOLD, 16));
		updateSquadButtonW.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				int id;
				double points;

				//Get the expected points for the current team;
				PlayerValueCurrent[] players = new PlayerValueCurrent[15];
				int[] currentSquad = new int[15];

				for (int i = 0; i < 15; i++) {

					id = fplSQL.getId(i);
					currentSquad[i] = id;

					points = expectedPoints(id);

					players[i] = new PlayerValueCurrent(id, fplSQL.getPosition(id), fplSQL.getTeam(id), fplSQL.getCost(id), points);

				}

				TeamSelection teamSelection = new TeamSelection(players, fplSQL);
				teamSelection.solve();
				double expectedPointsCurrent = teamSelection.expectedPoints(0);	

				ArrayList<PlayerValueCurrent> list = new ArrayList<>();
				ArrayList<Integer> player_ids = fplSQL.getPlayers();

				for (int id1 : player_ids) {

					points = expectedPoints(id1);

					list.add(new PlayerValueCurrent(id1, fplSQL.getPosition(id1), fplSQL.getTeam(id1), fplSQL.getCost(id1), points));					

				}

				ArrayList<Integer> teams = fplSQL.getTeamIDs();

				SquadUpdate solver = new SquadUpdate(list, teams, fplSQL);

				//Test optimal squad for up to 11 transfers.
				//More than 11 transfers would be counter-intuitive since it would mean the bench is transferred also.
				boolean changed = false;
				String[] names = new String[15];
				for (int i = 1; i <= 15; i++) {

					solver.setTransferCount(i);
					solver.setup();
					solver.solve();


					int[] squad = solver.squad;
					squad = fplSQL.sortByPosition(squad);
					names = fplSQL.getNames(squad);

					for (int j = 0; j < 15; j++) {

						id = squad[j];

						points = expectedPoints(id);

						players[j] = new PlayerValueCurrent(id, fplSQL.getPosition(id), fplSQL.getTeam(id), fplSQL.getCost(id), points);

					}

					teamSelection = new TeamSelection(players, fplSQL);
					teamSelection.solve();

					double expectedPoints = teamSelection.expectedPoints(0);

					if (expectedPoints > expectedPointsCurrent) {
						//Update current value.
						expectedPointsCurrent = expectedPoints;
						changed = true;
						//Update current squad.
						for (int h = 0; h < 15; h++) {

							currentSquad[h] = squad[h];
						}
					}

				}
				if (!changed) {
				} else {

					HashMap<Integer,Integer> existingSquad = fplSQL.getSquad();

					int currentValue = fplSQL.sellValues();

					names = fplSQL.getNames(currentSquad);


					for (int i = 0; i < 15; i++) {					

						if (existingSquad.containsKey(currentSquad[i])) {

							fplSQL.setPosition(i, currentSquad[i], names[i], existingSquad.get(currentSquad[i]));

						} else {

							fplSQL.setPosition(i, currentSquad[i], names[i], fplSQL.getCost(currentSquad[i]));

						}
					}

					int newValue = fplSQL.sellValues();
					if (fplSQL.hasRow("SELECT data FROM user_data WHERE data='money_remaining';")) {

						fplSQL.update("UPDATE user_data SET value = " + (currentValue - newValue) + " WHERE data='money_remaining';");

					} else {

						fplSQL.update("INSERT INTO user_data(data, value) VALUES('money_remaining', " + (currentValue - newValue) + ");");

					}
					moneyRemainingLabel.setText("Money Remaining: " + fplSQL.getLong("SELECT value FROM user_data WHERE data='money_remaining';")/10.0);
					squadValueLabel.setText("Squad Value: " + fplSQL.value());
					
					showSquad();
					showTeam();
					showSquadUpdate();

					//Expected Points
					//Get the expected points for the current team;
					players = new PlayerValueCurrent[15];
					currentSquad = new int[15];
					int[] currentSquadW = new int[15];


					for (int i = 0; i < 15; i++) {

						id = fplSQL.getId(i);
						currentSquad[i] = id;
						currentSquadW[i] = id;

						points = expectedPoints(id);

						players[i] = new PlayerValueCurrent(id, fplSQL.getPosition(id), fplSQL.getTeam(id), fplSQL.getCost(id), points);

					}

					teamSelection = new TeamSelection(players, fplSQL);
					teamSelection.solve();
					points = teamSelection.expectedPoints(0);

					expectedPointsLabel.setText("Expected Points: " + String.format("%.2f", points));

					//Average Points
					points = 0;

					for (int i = 0; i < 15; i++) {

						id = fplSQL.getId(i);
						points += fplSQL.getAveragePoints(id);

					}

					averagePointsLabel.setText("Average Points: " + String.format("%.2f", points));
					
					//Triple Captain
					points = 0;

					for (int i = 0; i < 15; i++) {

						id = fplSQL.getId(i);
						if (expectedPoints(id) > points) {

							points = expectedPoints(id);

						}

					}

					tripleCaptainLabel.setText("Triple Captain: " + String.format("%.2f", points));

					//Bench Boost
					points = teamSelection.benchPoints();
					benchBoostLabel.setText("Bench Boost: " + String.format("%.2f", points));
				}
			}
		});
		updateSquadButtonW.setBounds(294, 128, 294, 64);
		desktopPane.add(updateSquadButtonW);

		if (fplSQL.hasRow("SELECT data FROM user_data WHERE data='database_update'")) {

			label = "<html>" + "Update Database" + "<br>" + "Last updated at " + Time.getDate(fplSQL.getLong("SELECT value FROM user_data WHERE data='database_update'")) + "</html>";

		} else {

			label = "<html>" + "Update Database" + "<br>" + "No database records available" + "</html>";

		}

		updateDatabaseButton = new JButton(label);
		updateDatabaseButton.setFont(new Font("Serif", Font.BOLD, 16));
		updateDatabaseButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				fplSQL.readPlayers();
				fplSQL.updatePlayers();

				fplSQL.readTeams();
				fplSQL.updateTeams();

				fplSQL.readGameweeks();
				fplSQL.updateGameweeks();

				fplSQL.readFixtures();
				fplSQL.updateFixtures();
				
				//Set database update time in user_data.
				if (fplSQL.hasRow("SELECT data FROM user_data WHERE data='database_update';")) {

					fplSQL.update("UPDATE user_data SET value = " + Time.currentTime() + " WHERE data='database_update';");

				} else {

					fplSQL.update("INSERT INTO user_data(data, value) VALUES('database_update', " + Time.currentTime() + ");");

				}

				//Update database reliant variables gameweek and average team strength.
				gameweek = fplSQL.getInt("SELECT id FROM gameweeks WHERE is_next=1;");
				strength = fplSQL.getInt("SELECT SUM(strength) FROM teams;")/20.0;

				if (fplSQL.hasRow("SELECT data FROM user_data WHERE data='database_update'")) {

					label = "<html>" + "Update Database" + "<br>" + "Last updated at " + Time.getDate(fplSQL.getLong("SELECT value FROM user_data WHERE data='database_update'")) + "</html>";

				} else {

					label = "<html>" + "Update Database" + "<br>" + "No database records available" + "</html>";

				}

				updateDatabaseButton.setText(label);

				if (fplSQL.hasSquad()) {
					showTeam();
				}

				if (fplSQL.hasRow("SELECT data FROM user_data WHERE data='database_update';") &&
						fplSQL.hasRow("SELECT data FROM user_data WHERE data='pastfixtures_update';") &&
						fplSQL.hasRow("SELECT data FROM user_data WHERE data='playerhistory_update';")) {

					showSquadUpdate();

				}
			}
		});
		updateDatabaseButton.setBounds(0, 0, 294, 64);
		desktopPane.add(updateDatabaseButton);

		if (fplSQL.hasRow("SELECT data FROM user_data WHERE data='playerhistory_update'")) {

			label = "<html>" + "Update Player History" + "<br>" + "Last updated at " + Time.getDate(fplSQL.getLong("SELECT value FROM user_data WHERE data='playerhistory_update'")) + "</html>";

		} else {

			label = "<html>" + "Update Player History" + "<br>" + "No database records available" + "</html>";

		}

		updatePlayerHistoryButton = new JButton(label);
		updatePlayerHistoryButton.setFont(new Font("Serif", Font.BOLD, 16));
		updatePlayerHistoryButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				fplSQL.updatePlayerHistory();

				//Set database update time in user_data.
				if (fplSQL.hasRow("SELECT data FROM user_data WHERE data='playerhistory_update';")) {

					fplSQL.update("UPDATE user_data SET value = " + Time.currentTime() + " WHERE data='playerhistory_update';");

				} else {

					fplSQL.update("INSERT INTO user_data(data, value) VALUES('playerhistory_update', " + Time.currentTime() + ");");

				}

				if (fplSQL.hasRow("SELECT data FROM user_data WHERE data='database_update';") &&
						fplSQL.hasRow("SELECT data FROM user_data WHERE data='pastfixtures_update';") &&
						fplSQL.hasRow("SELECT data FROM user_data WHERE data='playerhistory_update';")) {

					showSquadUpdate();

				}

				if (fplSQL.hasRow("SELECT data FROM user_data WHERE data='playerhistory_update'")) {

					label = "<html>" + "Update Player History" + "<br>" + "Last updated at " + Time.getDate(fplSQL.getLong("SELECT value FROM user_data WHERE data='playerhistory_update'")) + "</html>";

				} else {

					label = "<html>" + "Update Player History" + "<br>" + "No database records available" + "</html>";

				}

				hist_max_minutes = fplSQL.getDouble("SELECT minutes FROM player_history ORDER BY minutes DESC;");
				updatePlayerHistoryButton.setText(label);

			}
		});
		updatePlayerHistoryButton.setBounds(0, 64, 294, 64);
		desktopPane.add(updatePlayerHistoryButton);

		if (fplSQL.hasRow("SELECT data FROM user_data WHERE data='pastfixtures_update'")) {

			label = "<html>" + "Update Past Fixtures" + "<br>" + "Last updated at " + Time.getDate(fplSQL.getLong("SELECT value FROM user_data WHERE data='pastfixtures_update'")) + "</html>";

		} else {

			label = "<html>" + "Update Past Fixtures" + "<br>" + "No database records available" + "</html>";

		}

		updatePastFixturesButton = new JButton(label);
		updatePastFixturesButton.setFont(new Font("Serif", Font.BOLD, 16));
		updatePastFixturesButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				ArrayList<Integer> players = fplSQL.getTrimmedPlayers(gameweek);
				fplSQL.getPastFixtures(players);

				//Set database update time in user_data.
				if (fplSQL.hasRow("SELECT data FROM user_data WHERE data='pastfixtures_update';")) {

					fplSQL.update("UPDATE user_data SET value = " + Time.currentTime() + " WHERE data='pastfixtures_update';");

				} else {

					fplSQL.update("INSERT INTO user_data(data, value) VALUES('pastfixtures_update', " + Time.currentTime() + ");");

				}

				if (fplSQL.hasSquad()) {
					showTeam();
				}

				if (fplSQL.hasRow("SELECT data FROM user_data WHERE data='database_update';") &&
						fplSQL.hasRow("SELECT data FROM user_data WHERE data='pastfixtures_update';") &&
						fplSQL.hasRow("SELECT data FROM user_data WHERE data='playerhistory_update';")) {

					showSquadUpdate();

				}

				if (fplSQL.hasRow("SELECT data FROM user_data WHERE data='pastfixtures_update'")) {

					label = "<html>" + "Update Past Fixtures" + "<br>" + "Last updated at " + Time.getDate(fplSQL.getLong("SELECT value FROM user_data WHERE data='pastfixtures_update'")) + "</html>";

				} else {

					label = "<html>" + "Update Past Fixtures" + "<br>" + "No database records available" + "</html>";

				}

				updatePastFixturesButton.setText(label);

			}
		});
		updatePastFixturesButton.setBounds(0, 128, 294, 64);
		desktopPane.add(updatePastFixturesButton);

		//Info Panel
		infoPanel = new JPanel();
		infoPanel.setBorder(new TitledBorder(null, "", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		infoPanel.setBounds(0, (int) (604/(double)720*height), (int) (1280/(double)1280*width), (int) (80/(double)720*height));
		desktopPane.add(infoPanel);
		infoPanel.setLayout(null);

		//Squad Value
		if (fplSQL.hasSquad()) {
			squadValueLabel = new JLabel("Squad Value: " + fplSQL.value());
		} else {
			squadValueLabel = new JLabel("No squad has been selected yet");
		}

		squadValueLabel.setBounds((int) (6/(double)1280*width), (int) (0/(double)720*height), (int) (320/(double)1280*width), (int) (40/(double)720*height));
		squadValueLabel.setFont(new Font("Serif", Font.BOLD, 16));
		squadValueLabel.setAlignmentX(0.5f);

		infoPanel.add(squadValueLabel);

		//Money Remaining
		if (fplSQL.hasRow("SELECT data FROM user_data WHERE data='money_remaining';")) {
			moneyRemainingLabel = new JLabel("Money Remaining: " + fplSQL.getLong("SELECT value FROM user_data WHERE data='money_remaining';")/10.0);
		} else {
			moneyRemainingLabel = new JLabel("Money Remaining: 100.0");
		}

		moneyRemainingLabel.setBounds((int) (6/(double)1280*width), (int) (40/(double)720*height), (int) (320/(double)1280*width), (int) (40/(double)720*height));
		moneyRemainingLabel.setFont(new Font("Serif", Font.BOLD, 16));
		moneyRemainingLabel.setAlignmentX(0.5f);

		infoPanel.add(moneyRemainingLabel);

		//Expected Points
		if (fplSQL.hasSquad()) {

			//Get the expected points for the current team;
			PlayerValueCurrent[] players = new PlayerValueCurrent[15];
			int[] currentSquad = new int[15];
			int[] currentSquadW = new int[15];
			int id;

			double points;

			for (int i = 0; i < 15; i++) {

				id = fplSQL.getId(i);
				currentSquad[i] = id;
				currentSquadW[i] = id;

				points = expectedPoints(id);

				players[i] = new PlayerValueCurrent(id, fplSQL.getPosition(id), fplSQL.getTeam(id), fplSQL.getCost(id), points);

			}

			TeamSelection teamSelection = new TeamSelection(players, fplSQL);
			teamSelection.solve();
			points = teamSelection.expectedPoints(0);

			expectedPointsLabel = new JLabel("Expected Points: " + String.format("%.2f", points), SwingConstants.CENTER);
		} else {
			expectedPointsLabel = new JLabel("No squad has been selected yet", SwingConstants.CENTER);
		}

		expectedPointsLabel.setBounds((int) (588/(double)1280*width), (int) (0/(double)720*height), (int) (268/(double)1280*width), (int) (40/(double)720*height));
		expectedPointsLabel.setFont(new Font("Serif", Font.BOLD, 16));

		infoPanel.add(expectedPointsLabel);

		//Average Points
		if (fplSQL.hasSquad()) {

			int id;
			double points = 0;

			for (int i = 0; i < 15; i++) {

				id = fplSQL.getId(i);
				points += fplSQL.getAveragePoints(id);

			}

			averagePointsLabel = new JLabel("Average Points: " + String.format("%.2f", points), SwingConstants.CENTER);
		} else {
			averagePointsLabel = new JLabel("No squad has been selected yet", SwingConstants.CENTER);
		}

		averagePointsLabel.setBounds((int) (588/(double)1280*width), (int) (40/(double)720*height), (int) (268/(double)1280*width), (int) (40/(double)720*height));
		averagePointsLabel.setFont(new Font("Serif", Font.BOLD, 16));

		infoPanel.add(averagePointsLabel);

		//Triple Captain
		if (fplSQL.hasSquad()) {

			int id;
			double points = 0;

			for (int i = 0; i < 15; i++) {

				id = fplSQL.getId(i);
				if (expectedPoints(id) > points) {

					points = expectedPoints(id);

				}

			}

			tripleCaptainLabel = new JLabel("Triple Captain: " + String.format("%.2f", points), SwingConstants.CENTER);
		} else {
			tripleCaptainLabel = new JLabel("No squad has been selected yet", SwingConstants.CENTER);
		}

		tripleCaptainLabel.setBounds((int) (788/(double)1280*width), (int) (0/(double)720*height), (int) (268/(double)1280*width), (int) (40/(double)720*height));
		tripleCaptainLabel.setFont(new Font("Serif", Font.BOLD, 16));

		infoPanel.add(tripleCaptainLabel);

		//Bench Boost
		if (fplSQL.hasSquad()) {

			PlayerValueCurrent[] players = new PlayerValueCurrent[15];
			TeamSelection teamSelection = new TeamSelection(players, fplSQL);
			HashMap<Integer, Integer> squad = fplSQL.getSquad();
			Object[] keys = squad.keySet().toArray();
			int id;

			for (int i = 0; i < 15; i++) {

				id = (int) keys[i];

				points = expectedPoints(id);

				players[i] = new PlayerValueCurrent(id, fplSQL.getPosition(id), fplSQL.getTeam(id), fplSQL.getCost(id), points);

			}

			teamSelection = new TeamSelection(players, fplSQL);
			teamSelection.solve();

			points = teamSelection.benchPoints();

			benchBoostLabel = new JLabel("Bench Boost: " + String.format("%.2f", points), SwingConstants.CENTER);
		} else {
			benchBoostLabel = new JLabel("No squad has been selected yet", SwingConstants.CENTER);
		}

		benchBoostLabel.setBounds((int) (788/(double)1280*width), (int) (40/(double)720*height), (int) (268/(double)1280*width), (int) (40/(double)720*height));
		benchBoostLabel.setFont(new Font("Serif", Font.BOLD, 16));

		infoPanel.add(benchBoostLabel);

		if (fplSQL.hasRow("SELECT data FROM user_data WHERE data='database_update';") &&
				fplSQL.hasRow("SELECT data FROM user_data WHERE data='pastfixtures_update';") &&
				fplSQL.hasRow("SELECT data FROM user_data WHERE data='playerhistory_update';")) {

			if (fplSQL.hasSquad()) {
				showSquadUpdate();
			}

		}

		frame.getContentPane().add(desktopPane, BorderLayout.CENTER);

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setTitle("FPL Fantasy Gui");
		frame.pack();
		frame.setVisible(true);

	}

	private void scaleImage() {

		Image scaleImage = pitchIcon.getImage().getScaledInstance(pitchLabel.getWidth(), pitchLabel.getHeight(), Image.SCALE_SMOOTH);
		icon = new ImageIcon(scaleImage);
		pitchLabel.setIcon(icon);
	}

	private void resize() {
		pitchLabel.setBounds((int) (856/(double)1280*width), 0, (int) (410/(double)1280*width), (int) (604/(double)720*height));
		scaleImage();

		infoPanel.setBounds(0, (int) (604/(double)720*height), (int) (1280/(double)1280*width), (int) (80/(double)720*height));
		squadValueLabel.setBounds((int) (6/(double)1280*width), (int) (0/(double)720*height), (int) (320/(double)1280*width), (int) (40/(double)720*height));
		moneyRemainingLabel.setBounds((int) (6/(double)1280*width), (int) (40/(double)720*height), (int) (320/(double)1280*width), (int) (40/(double)720*height));
		expectedPointsLabel.setBounds((int) (588/(double)1280*width), (int) (0/(double)720*height), (int) (268/(double)1280*width), (int) (40/(double)720*height));
		averagePointsLabel.setBounds((int) (588/(double)1280*width), (int) (40/(double)720*height), (int) (268/(double)1280*width), (int) (40/(double)720*height));
		tripleCaptainLabel.setBounds((int) (788/(double)1280*width), (int) (0/(double)720*height), (int) (268/(double)1280*width), (int) (40/(double)720*height));
		benchBoostLabel.setBounds((int) (788/(double)1280*width), (int) (40/(double)720*height), (int) (268/(double)1280*width), (int) (40/(double)720*height));

		getSquadButton.setBounds((int) (294/(double)1280*width), 0, (int) (294/(double)1280*width), (int) (64/(double)720*height));
		updateSquadButton.setBounds((int) (294/(double)1280*width), (int) (64/(double)720*height), (int) (294/(double)1280*width), (int) (64/(double)720*height));
		updateSquadButtonW.setBounds((int) (294/(double)1280*width), (int) (128/(double)720*height), (int) (294/(double)1280*width), (int) (64/(double)720*height));
		//testSquadButton.setBounds((int) (392/(double)1280*width), (int) (48/(double)720*height), (int) (196/(double)1280*width), (int) (48/(double)720*height));
		updateDatabaseButton.setBounds(0, 0, (int) (294/(double)1280*width), (int) (64/(double)720*height));
		updatePlayerHistoryButton.setBounds(0, (int) (64/(double)720*height), (int) (294/(double)1280*width), (int) (64/(double)720*height));
		updatePastFixturesButton.setBounds(0, (int) (128/(double)720*height), (int) (294/(double)1280*width), (int) (64/(double)720*height));

		scrollPane.setBounds((int) (0/(double)1280*width), (int) (192/(double)720*height), (int) (588/(double)1280*width), (int) (412/(double)720*height));

		resizeSquad();
		resizeTeam();
	}

	private void showSquad() {

		for (int i = 0; i < 15; i++) {

			if (playerLabels != null) {

				desktopPane.remove(playerLabels[i]);

			}
		}

		playerLabels = new JTextField[15];

		for (int i = 0; i < 15; i++) {

			playerLabels[i] = new JTextField(fplSQL.getName(i));

			switch (i+1) {

			case 1:

				playerLabels[i].setFont(new Font("Serif", Font.BOLD, 16));
				playerLabels[i].setBounds((int) (910/(double)1280*width), (int) (546/(double)720*height), (int) (200/(double)1280*width), (int) (20/(double)720*height));
				playerLabels[i].setOpaque(false);
				playerLabels[i].setEditable(false);
				playerLabels[i].setBorder(BorderFactory.createEmptyBorder());
				playerLabels[i].setHorizontalAlignment(JLabel.CENTER);

				desktopPane.setLayer(playerLabels[i], 1);
				desktopPane.add(playerLabels[i]);
				break;

			case 2:

				playerLabels[i].setFont(new Font("Serif", Font.BOLD, 16));
				playerLabels[i].setBounds((int) (1010/(double)1280*width), (int) (546/(double)720*height), (int) (200/(double)1280*width), (int) (20/(double)720*height));
				playerLabels[i].setOpaque(false);
				playerLabels[i].setEditable(false);
				playerLabels[i].setBorder(BorderFactory.createEmptyBorder());
				playerLabels[i].setHorizontalAlignment(JLabel.CENTER);

				desktopPane.setLayer(playerLabels[i], 1);
				desktopPane.add(playerLabels[i]);
				break;

			case 3:

				playerLabels[i].setFont(new Font("Serif", Font.BOLD, 16));
				playerLabels[i].setBounds((int) (832/(double)1280*width), (int) (400/(double)720*height), (int) (200/(double)1280*width), (int) (20/(double)720*height));
				playerLabels[i].setOpaque(false);
				playerLabels[i].setEditable(false);
				playerLabels[i].setBorder(BorderFactory.createEmptyBorder());
				playerLabels[i].setHorizontalAlignment(JLabel.CENTER);

				desktopPane.setLayer(playerLabels[i], 1);
				desktopPane.add(playerLabels[i]);
				break;

			case 4:

				playerLabels[i].setFont(new Font("Serif", Font.BOLD, 16));
				playerLabels[i].setBounds((int) (885/(double)1280*width), (int) (430/(double)720*height), (int) (200/(double)1280*width), (int) (20/(double)720*height));
				playerLabels[i].setOpaque(false);
				playerLabels[i].setEditable(false);
				playerLabels[i].setBorder(BorderFactory.createEmptyBorder());
				playerLabels[i].setHorizontalAlignment(JLabel.CENTER);

				desktopPane.setLayer(playerLabels[i], 1);
				desktopPane.add(playerLabels[i]);
				break;

			case 5:

				playerLabels[i].setFont(new Font("Serif", Font.BOLD, 16));
				playerLabels[i].setBounds((int) (958/(double)1280*width), (int) (400/(double)720*height), (int) (200/(double)1280*width), (int) (20/(double)720*height));
				playerLabels[i].setOpaque(false);
				playerLabels[i].setEditable(false);
				playerLabels[i].setBorder(BorderFactory.createEmptyBorder());
				playerLabels[i].setHorizontalAlignment(JLabel.CENTER);

				desktopPane.setLayer(playerLabels[i], 1);
				desktopPane.add(playerLabels[i]);
				break;

			case 6:

				playerLabels[i].setFont(new Font("Serif", Font.BOLD, 16));
				playerLabels[i].setBounds((int) (1031/(double)1280*width), (int) (430/(double)720*height), (int) (200/(double)1280*width), (int) (20/(double)720*height));
				playerLabels[i].setOpaque(false);
				playerLabels[i].setEditable(false);
				playerLabels[i].setBorder(BorderFactory.createEmptyBorder());
				playerLabels[i].setHorizontalAlignment(JLabel.CENTER);

				desktopPane.setLayer(playerLabels[i], 1);
				desktopPane.add(playerLabels[i]);
				break;

			case 7:

				playerLabels[i].setFont(new Font("Serif", Font.BOLD, 16));
				playerLabels[i].setBounds((int) (1084/(double)1280*width), (int) (400/(double)720*height), (int) (200/(double)1280*width), (int) (20/(double)720*height));
				playerLabels[i].setOpaque(false);
				playerLabels[i].setEditable(false);
				playerLabels[i].setBorder(BorderFactory.createEmptyBorder());
				playerLabels[i].setHorizontalAlignment(JLabel.CENTER);

				desktopPane.setLayer(playerLabels[i], 1);
				desktopPane.add(playerLabels[i]);
				break;

			case 8:

				playerLabels[i].setFont(new Font("Serif", Font.BOLD, 16));
				playerLabels[i].setBounds((int) (832/(double)1280*width), (int) (285/(double)720*height), (int) (200/(double)1280*width), (int) (20/(double)720*height));
				playerLabels[i].setOpaque(false);
				playerLabels[i].setEditable(false);
				playerLabels[i].setBorder(BorderFactory.createEmptyBorder());
				playerLabels[i].setHorizontalAlignment(JLabel.CENTER);

				desktopPane.setLayer(playerLabels[i], 1);
				desktopPane.add(playerLabels[i]);
				break;

			case 9:

				playerLabels[i].setFont(new Font("Serif", Font.BOLD, 16));
				playerLabels[i].setBounds((int) (885/(double)1280*width), (int) (305/(double)720*height), (int) (200/(double)1280*width), (int) (20/(double)720*height));
				playerLabels[i].setOpaque(false);
				playerLabels[i].setEditable(false);
				playerLabels[i].setBorder(BorderFactory.createEmptyBorder());
				playerLabels[i].setHorizontalAlignment(JLabel.CENTER);

				desktopPane.setLayer(playerLabels[i], 1);
				desktopPane.add(playerLabels[i]);
				break;

			case 10:

				playerLabels[i].setFont(new Font("Serif", Font.BOLD, 16));
				playerLabels[i].setBounds((int) (958/(double)1280*width), (int) (285/(double)720*height), (int) (200/(double)1280*width), (int) (20/(double)720*height));
				playerLabels[i].setOpaque(false);
				playerLabels[i].setEditable(false);
				playerLabels[i].setBorder(BorderFactory.createEmptyBorder());
				playerLabels[i].setHorizontalAlignment(JLabel.CENTER);

				desktopPane.setLayer(playerLabels[i], 1);
				desktopPane.add(playerLabels[i]);
				break;

			case 11:

				playerLabels[i].setFont(new Font("Serif", Font.BOLD, 16));
				playerLabels[i].setBounds((int) (1031/(double)1280*width), (int) (305/(double)720*height), (int) (200/(double)1280*width), (int) (20/(double)720*height));
				playerLabels[i].setOpaque(false);
				playerLabels[i].setEditable(false);
				playerLabels[i].setBorder(BorderFactory.createEmptyBorder());
				playerLabels[i].setHorizontalAlignment(JLabel.CENTER);

				desktopPane.setLayer(playerLabels[i], 1);
				desktopPane.add(playerLabels[i]);
				break;

			case 12:

				playerLabels[i].setFont(new Font("Serif", Font.BOLD, 16));
				playerLabels[i].setBounds((int) (1084/(double)1280*width), (int) (285/(double)720*height), (int) (200/(double)1280*width), (int) (20/(double)720*height));
				playerLabels[i].setOpaque(false);
				playerLabels[i].setEditable(false);
				playerLabels[i].setBorder(BorderFactory.createEmptyBorder());
				playerLabels[i].setHorizontalAlignment(JLabel.CENTER);

				desktopPane.setLayer(playerLabels[i], 1);
				desktopPane.add(playerLabels[i]);
				break;

			case 13:

				playerLabels[i].setFont(new Font("Serif", Font.BOLD, 16));
				playerLabels[i].setBounds((int) (875/(double)1280*width), (int) (180/(double)720*height), (int) (200/(double)1280*width), (int) (20/(double)720*height));
				playerLabels[i].setOpaque(false);
				playerLabels[i].setEditable(false);
				playerLabels[i].setBorder(BorderFactory.createEmptyBorder());
				playerLabels[i].setHorizontalAlignment(JLabel.CENTER);

				desktopPane.setLayer(playerLabels[i], 1);
				desktopPane.add(playerLabels[i]);
				break;

			case 14:

				playerLabels[i].setFont(new Font("Serif", Font.BOLD, 16));
				playerLabels[i].setBounds((int) (958/(double)1280*width), (int) (150/(double)720*height), (int) (200/(double)1280*width), (int) (20/(double)720*height));
				playerLabels[i].setOpaque(false);
				playerLabels[i].setEditable(false);
				playerLabels[i].setBorder(BorderFactory.createEmptyBorder());
				playerLabels[i].setHorizontalAlignment(JLabel.CENTER);

				desktopPane.setLayer(playerLabels[i], 1);
				desktopPane.add(playerLabels[i]);
				break;

			case 15:

				playerLabels[i].setFont(new Font("Serif", Font.BOLD, 16));
				playerLabels[i].setBounds((int) (1041/(double)1280*width), (int) (180/(double)720*height), (int) (200/(double)1280*width), (int) (20/(double)720*height));
				playerLabels[i].setOpaque(false);
				playerLabels[i].setEditable(false);
				playerLabels[i].setBorder(BorderFactory.createEmptyBorder());
				playerLabels[i].setHorizontalAlignment(JLabel.CENTER);

				desktopPane.setLayer(playerLabels[i], 1);
				desktopPane.add(playerLabels[i]);
				break;

			}

		}
	}

	private void showTeam() {

		for (int i = 0; i < 17; i++) {

			if (teamLabels != null) {

				desktopPane.remove(teamLabels[i]);

			}
		}

		teamLabels = new JTextField[17];

		//Get the expected points for the current team;
		PlayerValueCurrent[] players = new PlayerValueCurrent[15];
		int[] currentSquad = new int[15];
		int[] currentSquadW = new int[15];
		int id;

		double points;

		for (int i = 0; i < 15; i++) {

			id = fplSQL.getId(i);
			currentSquad[i] = id;
			currentSquadW[i] = id;

			points = expectedPoints(id);

			players[i] = new PlayerValueCurrent(id, fplSQL.getPosition(id), fplSQL.getTeam(id), fplSQL.getCost(id), points);

		}

		TeamSelection teamSelection = new TeamSelection(players, fplSQL);
		teamSelection.solve();
		String[][] expectedPoints = teamSelection.getExpectedPoints();

		teamLabels[0] = new JTextField("Starting Eleven:");
		teamLabels[12] = new JTextField("Bench:");

		for (int i = 1; i < 12; i++) {

			teamLabels[i] = new JTextField(expectedPoints[i-1][0] + " - " + expectedPoints[i-1][1]);

		}

		for (int i = 13; i < 17; i++) {

			teamLabels[i] = new JTextField(expectedPoints[i-2][0] + " - " + expectedPoints[i-2][1]);

		}

		for (int i = 0; i < 17; i++) {

			switch (i+1) {

			case 1:

				teamLabels[i].setFont(new Font("Serif", Font.BOLD, 16));
				teamLabels[i].setBounds((int) (588/(double)1280*width), (int) (0/(double)720*height), (int) (268/(double)1280*width), (int) (32/(double)720*height));
				teamLabels[i].setEditable(false);
				teamLabels[i].setHorizontalAlignment(JLabel.CENTER);

				desktopPane.setLayer(teamLabels[i], 1);
				desktopPane.add(teamLabels[i]);
				break;

			case 2:

				teamLabels[i].setFont(new Font("Serif", Font.BOLD, 16));
				teamLabels[i].setBounds((int) (588/(double)1280*width), (int) (32/(double)720*height), (int) (268/(double)1280*width), (int) (36/(double)720*height));
				teamLabels[i].setEditable(false);
				teamLabels[i].setHorizontalAlignment(JLabel.CENTER);

				desktopPane.setLayer(teamLabels[i], 1);
				desktopPane.add(teamLabels[i]);
				break;

			case 3:

				teamLabels[i].setFont(new Font("Serif", Font.BOLD, 16));
				teamLabels[i].setBounds((int) (588/(double)1280*width), (int) (68/(double)720*height), (int) (268/(double)1280*width), (int) (36/(double)720*height));
				teamLabels[i].setEditable(false);
				teamLabels[i].setHorizontalAlignment(JLabel.CENTER);

				desktopPane.setLayer(teamLabels[i], 1);
				desktopPane.add(teamLabels[i]);
				break;

			case 4:

				teamLabels[i].setFont(new Font("Serif", Font.BOLD, 16));
				teamLabels[i].setBounds((int) (588/(double)1280*width), (int) (104/(double)720*height), (int) (268/(double)1280*width), (int) (36/(double)720*height));
				teamLabels[i].setEditable(false);
				teamLabels[i].setHorizontalAlignment(JLabel.CENTER);


				desktopPane.setLayer(teamLabels[i], 1);
				desktopPane.add(teamLabels[i]);
				break;

			case 5:

				teamLabels[i].setFont(new Font("Serif", Font.BOLD, 16));
				teamLabels[i].setBounds((int) (588/(double)1280*width), (int) (140/(double)720*height), (int) (268/(double)1280*width), (int) (36/(double)720*height));
				teamLabels[i].setEditable(false);
				teamLabels[i].setHorizontalAlignment(JLabel.CENTER);

				desktopPane.setLayer(teamLabels[i], 1);
				desktopPane.add(teamLabels[i]);
				break;

			case 6:

				teamLabels[i].setFont(new Font("Serif", Font.BOLD, 16));
				teamLabels[i].setBounds((int) (588/(double)1280*width), (int) (176/(double)720*height), (int) (268/(double)1280*width), (int) (36/(double)720*height));
				teamLabels[i].setEditable(false);
				teamLabels[i].setHorizontalAlignment(JLabel.CENTER);

				desktopPane.setLayer(teamLabels[i], 1);
				desktopPane.add(teamLabels[i]);
				break;

			case 7:

				teamLabels[i].setFont(new Font("Serif", Font.BOLD, 16));
				teamLabels[i].setBounds((int) (588/(double)1280*width), (int) (212/(double)720*height), (int) (268/(double)1280*width), (int) (36/(double)720*height));
				teamLabels[i].setEditable(false);
				teamLabels[i].setHorizontalAlignment(JLabel.CENTER);

				desktopPane.setLayer(teamLabels[i], 1);
				desktopPane.add(teamLabels[i]);
				break;

			case 8:

				teamLabels[i].setFont(new Font("Serif", Font.BOLD, 16));
				teamLabels[i].setBounds((int) (588/(double)1280*width), (int) (248/(double)720*height), (int) (268/(double)1280*width), (int) (36/(double)720*height));
				teamLabels[i].setEditable(false);
				teamLabels[i].setHorizontalAlignment(JLabel.CENTER);

				desktopPane.setLayer(teamLabels[i], 1);
				desktopPane.add(teamLabels[i]);
				break;

			case 9:

				teamLabels[i].setFont(new Font("Serif", Font.BOLD, 16));
				teamLabels[i].setBounds((int) (588/(double)1280*width), (int) (284/(double)720*height), (int) (268/(double)1280*width), (int) (36/(double)720*height));
				teamLabels[i].setEditable(false);
				teamLabels[i].setHorizontalAlignment(JLabel.CENTER);

				desktopPane.setLayer(teamLabels[i], 1);
				desktopPane.add(teamLabels[i]);
				break;

			case 10:

				teamLabels[i].setFont(new Font("Serif", Font.BOLD, 16));
				teamLabels[i].setBounds((int) (588/(double)1280*width), (int) (320/(double)720*height), (int) (268/(double)1280*width), (int) (36/(double)720*height));
				teamLabels[i].setEditable(false);
				teamLabels[i].setHorizontalAlignment(JLabel.CENTER);

				desktopPane.setLayer(teamLabels[i], 1);
				desktopPane.add(teamLabels[i]);
				break;

			case 11:

				teamLabels[i].setFont(new Font("Serif", Font.BOLD, 16));
				teamLabels[i].setBounds((int) (588/(double)1280*width), (int) (356/(double)720*height), (int) (268/(double)1280*width), (int) (36/(double)720*height));
				teamLabels[i].setEditable(false);
				teamLabels[i].setHorizontalAlignment(JLabel.CENTER);

				desktopPane.setLayer(teamLabels[i], 1);
				desktopPane.add(teamLabels[i]);
				break;

			case 12:

				teamLabels[i].setFont(new Font("Serif", Font.BOLD, 16));
				teamLabels[i].setBounds((int) (588/(double)1280*width), (int) (392/(double)720*height), (int) (268/(double)1280*width), (int) (36/(double)720*height));
				teamLabels[i].setEditable(false);
				teamLabels[i].setHorizontalAlignment(JLabel.CENTER);

				desktopPane.setLayer(teamLabels[i], 1);
				desktopPane.add(teamLabels[i]);
				break;

			case 13:

				teamLabels[i].setFont(new Font("Serif", Font.BOLD, 16));
				teamLabels[i].setBounds((int) (588/(double)1280*width), (int) (428/(double)720*height), (int) (268/(double)1280*width), (int) (32/(double)720*height));
				teamLabels[i].setEditable(false);
				teamLabels[i].setHorizontalAlignment(JLabel.CENTER);

				desktopPane.setLayer(teamLabels[i], 1);
				desktopPane.add(teamLabels[i]);
				break;

			case 14:

				teamLabels[i].setFont(new Font("Serif", Font.BOLD, 16));
				teamLabels[i].setBounds((int) (588/(double)1280*width), (int) (460/(double)720*height), (int) (268/(double)1280*width), (int) (36/(double)720*height));
				teamLabels[i].setEditable(false);
				teamLabels[i].setHorizontalAlignment(JLabel.CENTER);

				desktopPane.setLayer(teamLabels[i], 1);
				desktopPane.add(teamLabels[i]);
				break;

			case 15:

				teamLabels[i].setFont(new Font("Serif", Font.BOLD, 16));
				teamLabels[i].setBounds((int) (588/(double)1280*width), (int) (496/(double)720*height), (int) (268/(double)1280*width), (int) (36/(double)720*height));
				teamLabels[i].setEditable(false);
				teamLabels[i].setHorizontalAlignment(JLabel.CENTER);

				desktopPane.setLayer(teamLabels[i], 1);
				desktopPane.add(teamLabels[i]);
				break;

			case 16:

				teamLabels[i].setFont(new Font("Serif", Font.BOLD, 16));
				teamLabels[i].setBounds((int) (588/(double)1280*width), (int) (532/(double)720*height), (int) (268/(double)1280*width), (int) (36/(double)720*height));
				teamLabels[i].setEditable(false);
				teamLabels[i].setHorizontalAlignment(JLabel.CENTER);

				desktopPane.setLayer(teamLabels[i], 1);
				desktopPane.add(teamLabels[i]);
				break;

			case 17:

				teamLabels[i].setFont(new Font("Serif", Font.BOLD, 16));
				teamLabels[i].setBounds((int) (588/(double)1280*width), (int) (568/(double)720*height), (int) (268/(double)1280*width), (int) (36/(double)720*height));
				teamLabels[i].setEditable(false);
				teamLabels[i].setHorizontalAlignment(JLabel.CENTER);

				desktopPane.setLayer(teamLabels[i], 1);
				desktopPane.add(teamLabels[i]);
				break;
			}
		}	
	}

	private void resizeSquad() {	
		for (int i = 0; i < 15; i++) {

			if (playerLabels[i] == null) {
				return;
			}

			switch (i+1) {

			case 1:

				playerLabels[i].setBounds((int) (910/(double)1280*width), (int) (546/(double)720*height), (int) (200/(double)1280*width), (int) (20/(double)720*height));
				break;

			case 2:

				playerLabels[i].setBounds((int) (1010/(double)1280*width), (int) (546/(double)720*height), (int) (200/(double)1280*width), (int) (20/(double)720*height));
				break;

			case 3:

				playerLabels[i].setBounds((int) (832/(double)1280*width), (int) (400/(double)720*height), (int) (200/(double)1280*width), (int) (20/(double)720*height));
				break;

			case 4:

				playerLabels[i].setBounds((int) (885/(double)1280*width), (int) (430/(double)720*height), (int) (200/(double)1280*width), (int) (20/(double)720*height));
				break;

			case 5:

				playerLabels[i].setBounds((int) (958/(double)1280*width), (int) (400/(double)720*height), (int) (200/(double)1280*width), (int) (20/(double)720*height));
				break;

			case 6:

				playerLabels[i].setBounds((int) (1031/(double)1280*width), (int) (430/(double)720*height), (int) (200/(double)1280*width), (int) (20/(double)720*height));
				break;

			case 7:

				playerLabels[i].setBounds((int) (1084/(double)1280*width), (int) (400/(double)720*height), (int) (200/(double)1280*width), (int) (20/(double)720*height));
				break;

			case 8:

				playerLabels[i].setBounds((int) (832/(double)1280*width), (int) (285/(double)720*height), (int) (200/(double)1280*width), (int) (20/(double)720*height));
				break;

			case 9:

				playerLabels[i].setBounds((int) (885/(double)1280*width), (int) (305/(double)720*height), (int) (200/(double)1280*width), (int) (20/(double)720*height));
				break;

			case 10:

				playerLabels[i].setBounds((int) (958/(double)1280*width), (int) (285/(double)720*height), (int) (200/(double)1280*width), (int) (20/(double)720*height));
				break;

			case 11:

				playerLabels[i].setBounds((int) (1031/(double)1280*width), (int) (305/(double)720*height), (int) (200/(double)1280*width), (int) (20/(double)720*height));
				break;

			case 12:

				playerLabels[i].setBounds((int) (1084/(double)1280*width), (int) (285/(double)720*height), (int) (200/(double)1280*width), (int) (20/(double)720*height));
				break;

			case 13:

				playerLabels[i].setBounds((int) (875/(double)1280*width), (int) (180/(double)720*height), (int) (200/(double)1280*width), (int) (20/(double)720*height));
				break;

			case 14:

				playerLabels[i].setBounds((int) (958/(double)1280*width), (int) (150/(double)720*height), (int) (200/(double)1280*width), (int) (20/(double)720*height));
				break;

			case 15:

				playerLabels[i].setBounds((int) (1041/(double)1280*width), (int) (180/(double)720*height), (int) (200/(double)1280*width), (int) (20/(double)720*height));
				break;

			}
		}
	}

	private void resizeTeam() {

		for (int i = 0; i < 17; i++) {

			switch (i+1) {

			case 1:

				teamLabels[i].setBounds((int) (588/(double)1280*width), (int) (0/(double)720*height), (int) (268/(double)1280*width), (int) (32/(double)720*height));
				break;

			case 2:

				teamLabels[i].setBounds((int) (588/(double)1280*width), (int) (32/(double)720*height), (int) (268/(double)1280*width), (int) (36/(double)720*height));
				break;

			case 3:

				teamLabels[i].setBounds((int) (588/(double)1280*width), (int) (68/(double)720*height), (int) (268/(double)1280*width), (int) (36/(double)720*height));
				break;

			case 4:

				teamLabels[i].setBounds((int) (588/(double)1280*width), (int) (104/(double)720*height), (int) (268/(double)1280*width), (int) (36/(double)720*height));
				break;

			case 5:

				teamLabels[i].setBounds((int) (588/(double)1280*width), (int) (140/(double)720*height), (int) (268/(double)1280*width), (int) (36/(double)720*height));
				break;

			case 6:

				teamLabels[i].setBounds((int) (588/(double)1280*width), (int) (176/(double)720*height), (int) (268/(double)1280*width), (int) (36/(double)720*height));
				break;

			case 7:

				teamLabels[i].setBounds((int) (588/(double)1280*width), (int) (212/(double)720*height), (int) (268/(double)1280*width), (int) (36/(double)720*height));
				break;

			case 8:

				teamLabels[i].setBounds((int) (588/(double)1280*width), (int) (248/(double)720*height), (int) (268/(double)1280*width), (int) (36/(double)720*height));
				break;

			case 9:

				teamLabels[i].setBounds((int) (588/(double)1280*width), (int) (284/(double)720*height), (int) (268/(double)1280*width), (int) (36/(double)720*height));
				break;

			case 10:

				teamLabels[i].setBounds((int) (588/(double)1280*width), (int) (320/(double)720*height), (int) (268/(double)1280*width), (int) (36/(double)720*height));
				break;

			case 11:

				teamLabels[i].setBounds((int) (588/(double)1280*width), (int) (356/(double)720*height), (int) (268/(double)1280*width), (int) (36/(double)720*height));
				break;

			case 12:

				teamLabels[i].setBounds((int) (588/(double)1280*width), (int) (392/(double)720*height), (int) (268/(double)1280*width), (int) (36/(double)720*height));
				break;

			case 13:

				teamLabels[i].setBounds((int) (588/(double)1280*width), (int) (428/(double)720*height), (int) (268/(double)1280*width), (int) (32/(double)720*height));
				break;

			case 14:

				teamLabels[i].setBounds((int) (588/(double)1280*width), (int) (460/(double)720*height), (int) (268/(double)1280*width), (int) (36/(double)720*height));
				break;

			case 15:

				teamLabels[i].setBounds((int) (588/(double)1280*width), (int) (496/(double)720*height), (int) (268/(double)1280*width), (int) (36/(double)720*height));
				break;

			case 16:

				teamLabels[i].setBounds((int) (588/(double)1280*width), (int) (532/(double)720*height), (int) (268/(double)1280*width), (int) (36/(double)720*height));
				break;

			case 17:

				teamLabels[i].setBounds((int) (588/(double)1280*width), (int) (568/(double)720*height), (int) (268/(double)1280*width), (int) (36/(double)720*height));
				break;
			}
		}		
	}

	private double expectedPoints(int id) {
		
		//If the gameweek is less than 7 and the player has played less than 120 minutes
		//use an alternative calculation to get expected points based on the stats of the previous season.
		//If no stats exist for the previous season give them an expected points value of 0.
		if (gameweek < 7 && fplSQL.hasRow("SELECT id FROM players WHERE minutes<120")) {
			
			//If the player has historical data from the previous season.
			if (fplSQL.hasHistory(id)) {
				
				//Get standardised minutes.
				standard_minutes = fplSQL.getDouble("SELECT minutes FROM player_history WHERE id=" + id + ";")/hist_max_minutes;
				
				//Calculate average points.
				points = (fplSQL.getDouble("SELECT total_points FROM player_history WHERE id=" + id + ";") / 38.0) * standard_minutes;
				
				//Calculate average minutes.
				minutes = standard_minutes * 90;
				
			}
			
		} else {
			
			minutes = fplSQL.getExpectedMinutes(id, gameweek);
			points = fplSQL.getExpectedPoints(id, gameweek);
			
		}

		if (minutes > 60) {
			points *= fplSQL.getPlayChance(id)/100.0;
		} else if (minutes > 45) {
			points *= 0.75;
			points *= fplSQL.getPlayChance(id)/100.0;
		} else if (minutes > 30) {
			points *= 0.5;
			points *= fplSQL.getPlayChance(id)/100.0;
		} else {
			points *= 0;
		}

		//Get opponent teams for all upcoming fixtures in gameweek.
		teamID = fplSQL.getInt("SELECT id FROM teams WHERE code=" + fplSQL.getTeam(id) + ";");
		opponentTeams = fplSQL.opponentTeams(teamID, gameweek);

		expPoints = 0;

		//If the team has no fixtures in the next gameweek, return 0 expected points.
		if (opponentTeams.size() == 0) {

			return expPoints;

		}

		//Iterate trough all fixtures and calculate expected points.
		for (int i : opponentTeams) {

			//Get the strength difference.
			strengthDiff = strength - fplSQL.getInt("SELECT strength FROM teams WHERE id=" + i + ";");

			//Add multipliers for each strength difference.
			expPoints += (1 + (strengthDiff/4)) * points;

		}

		return expPoints;		

	}

	@SuppressWarnings("serial")
	private void showSquadUpdate() {

		int id;
		//Get the expected points for the current team;
		PlayerValueCurrent[] players = new PlayerValueCurrent[15];
		int[] currentSquad = new int[15];
		int[] currentSquadW = new int[15];

		double points;

		for (int i = 0; i < 15; i++) {

			id = fplSQL.getId(i);
			currentSquad[i] = id;
			currentSquadW[i] = id;

			points = expectedPoints(id);

			players[i] = new PlayerValueCurrent(id, fplSQL.getPosition(id), fplSQL.getTeam(id), fplSQL.getCost(id), points);

		}

		TeamSelection teamSelection = new TeamSelection(players, fplSQL);
		teamSelection.solve();
		double expectedPointsCurrent = teamSelection.expectedPoints(0);
		double expectedPointsCurrentW = expectedPointsCurrent;		

		ArrayList<PlayerValueCurrent> list = new ArrayList<>();

		for (int id1 : fplSQL.getTrimmedPlayers(gameweek)) {

			points = expectedPoints(id1);

			list.add(new PlayerValueCurrent(id1, fplSQL.getPosition(id1), fplSQL.getTeam(id1), fplSQL.getCost(id1), points));					

		}

		ArrayList<Integer> teams = fplSQL.getTeamIDs();

		SquadUpdate solver = new SquadUpdate(list, teams, fplSQL);

		//Test optimal squad for up to 11 transfers.
		//More than 11 transfers would be counter-intuitive since it would mean the bench is transferred also.
		boolean changed = false;
		boolean changedW = false;
		int transfers = 0;
		int transfersW = 0;
		double squadPoints = 0;
		double squadPointsW = 0;
		String[] names = new String[15];
		for (int i = 1; i <= 15; i++) {

			solver.setTransferCount(i);
			solver.setup();
			solver.solve();

			int[] squad = solver.squad;
			squad = fplSQL.sortByPosition(squad);
			names = fplSQL.getNames(squad);

			for (int j = 0; j < 15; j++) {

				id = squad[j];

				points = expectedPoints(id);

				players[j] = new PlayerValueCurrent(id, fplSQL.getPosition(id), fplSQL.getTeam(id), fplSQL.getCost(id), points);

			}

			teamSelection = new TeamSelection(players, fplSQL);
			teamSelection.solve();

			//For each transfer over 1 add a modifier of -4 points.
			int modifier = 0;
			if (i != 0) {
				modifier = (i-1) * -4;
			}
			double expectedPoints = teamSelection.expectedPoints(modifier);
			double expectedPointsW = teamSelection.expectedPoints(0);

			if (expectedPoints > expectedPointsCurrent) {
				//Update current value.
				expectedPointsCurrent = expectedPoints;
				changed = true;
				transfers = i;
				//Update current squad.
				for (int h = 0; h < 15; h++) {

					currentSquad[h] = squad[h];
				}
			}

			if (expectedPointsW > expectedPointsCurrentW) {

				expectedPointsCurrentW = expectedPointsW;
				changedW = true;
				transfersW = i;

				for (int h = 0; h < 15; h++) {

					currentSquadW[h] = squad[h];
					
				}
			}

		}

		String[][] ASquadUpdate = new String[21][2];

		if (!changed) {

		} else {

			names = fplSQL.getNames(currentSquad);

			for (int i = 0; i < 15; i++) {

				ASquadUpdate[i][0] = names[i] + " - " + String.format("%.2f", expectedPoints(currentSquad[i]));

			}

			ASquadUpdate[15][0] = "Squad Information:";
			ASquadUpdate[16][0] = "Transfers = " + transfers;
			ASquadUpdate[17][0] = "Expected Points = " + String.format("%.2f", expectedPointsCurrent);

			for (int i = 0; i < 15; i++) {

				squadPoints += fplSQL.getAveragePoints(currentSquad[i]);

			}

			ASquadUpdate[18][0] = "Average Points = " + String.format("%.2f", squadPoints);

			points = 0;

			for (int i = 0; i < 15; i++) {

				if (expectedPoints(currentSquad[i]) > points) {

					points = expectedPoints(currentSquad[i]);

				}

			}

			ASquadUpdate[19][0] = "Triple Captain = " + String.format("%.2f", points);

			for (int i = 0; i < 15; i++) {

				points = expectedPoints(currentSquad[i]);

				players[i] = new PlayerValueCurrent(currentSquad[i], fplSQL.getPosition(currentSquad[i]), fplSQL.getTeam(currentSquad[i]), fplSQL.getCost(currentSquad[i]), points);

			}

			teamSelection = new TeamSelection(players, fplSQL);
			teamSelection.solve();

			points = teamSelection.benchPoints();

			ASquadUpdate[20][0] = "Bench Boost = " + String.format("%.2f", points);

		}

		if (changedW) {

			names = fplSQL.getNames(currentSquadW);

			for (int i = 0; i < 15; i++) {

				ASquadUpdate[i][1] = names[i] + " - " + String.format("%.2f", expectedPoints(currentSquadW[i]));

			}

			ASquadUpdate[15][1] = "Squad Information:";
			ASquadUpdate[16][1] = "Transfers = " + transfersW;
			ASquadUpdate[17][1] = "Expected Points = " + String.format("%.2f", expectedPointsCurrentW);

			for (int i = 0; i < 15; i++) {

				squadPointsW += fplSQL.getAveragePoints(currentSquadW[i]);

			}

			ASquadUpdate[18][1] = "Average Points = " + String.format("%.2f", squadPointsW); 

			names = fplSQL.getNames(currentSquadW);

			for (int i = 0; i < 15; i++) {

				squadPointsW += fplSQL.getAveragePoints(currentSquadW[i]);

			}

			points = 0;

			for (int i = 0; i < 15; i++) {

				if (expectedPoints(currentSquadW[i]) > points) {

					points = expectedPoints(currentSquadW[i]);

				}

			}

			ASquadUpdate[19][1] = "Triple Captain = " + String.format("%.2f", points);

			for (int i = 0; i < 15; i++) {

				points = expectedPoints(currentSquadW[i]);

				players[i] = new PlayerValueCurrent(currentSquadW[i], fplSQL.getPosition(currentSquadW[i]), fplSQL.getTeam(currentSquadW[i]), fplSQL.getCost(currentSquadW[i]), points);

			}

			teamSelection = new TeamSelection(players, fplSQL);
			teamSelection.solve();

			points = teamSelection.benchPoints();

			ASquadUpdate[20][1] = "Bench Boost = " + String.format("%.2f", points);


		}

		String[] columns = {"Updated Squad","Updated Squad with Wildcard or Free Hit"};
		
		squadUpdateTable.setModel(new DefaultTableModel(ASquadUpdate, columns) {

			@Override
			public boolean isCellEditable(int row, int column) {
				//all cells false
				return false;
			}
		});
	}
}