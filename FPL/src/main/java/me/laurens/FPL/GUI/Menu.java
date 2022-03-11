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
	private JLabel databaseLabel;
	private JLabel squadValueLabel;

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
				double hist_max_minutes = fplSQL.getDouble("SELECT minutes FROM player_history ORDER BY minutes DESC;");

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

				showSquad();
				showTeam();
				showSquadUpdate();


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
				System.out.println("Expected points for existing team: " + expectedPointsCurrent);			

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
				int transfers = 0;
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
						transfers = i;
						System.out.println("Expected points for this team: " + expectedPoints);
						changed = true;
						//Update current squad.
						for (int h = 0; h < 15; h++) {

							currentSquad[h] = squad[h];
						}
					}

				}
				if (!changed) {

					System.out.println("No better team could be found.");
				} else {

					HashMap<Integer,Integer> existingSquad = fplSQL.getSquad();

					int currentValue = fplSQL.sellValues();

					System.out.println("Optimal Squad with " + transfers + " transfers.");
					names = fplSQL.getNames(currentSquad);


					for (int i = 0; i < 15; i++) {

						System.out.println(names[i]);						

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

					showSquad();
					showTeam();
					showSquadUpdate();

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
				System.out.println("Expected points for existing team: " + expectedPointsCurrent);			

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
				int transfers = 0;
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
						transfers = i;
						changed = true;
						//Update current squad.
						for (int h = 0; h < 15; h++) {

							currentSquad[h] = squad[h];
						}
					}

				}
				if (!changed) {

					System.out.println("No better team could be found.");
				} else {
					System.out.println("Expected points for this team: " + expectedPointsCurrent);

					HashMap<Integer,Integer> existingSquad = fplSQL.getSquad();

					int currentValue = fplSQL.sellValues();

					System.out.println("Optimal Squad with " + transfers + " transfers.");
					names = fplSQL.getNames(currentSquad);


					for (int i = 0; i < 15; i++) {

						System.out.println(names[i]);						

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

					showSquad();
					showTeam();
					showSquadUpdate();

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
					databaseLabel.setText("Last updated database at " + Time.getDate(Time.currentTime()));

				} else {

					fplSQL.update("INSERT INTO user_data(data, value) VALUES('database_update', " + Time.currentTime() + ");");
					databaseLabel.setText("Last updated database at " + Time.getDate(Time.currentTime()));

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
				
				showTeam();

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

			label = "<html>" + "Update Database" + "<br>" + "No database records available" + "</html>";

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

					label = "<html>" + "Update Database" + "<br>" + "No database records available" + "</html>";

				}
				
				updatePlayerHistoryButton.setText(label);

			}
		});
		updatePlayerHistoryButton.setBounds(0, 64, 294, 64);
		desktopPane.add(updatePlayerHistoryButton);

		if (fplSQL.hasRow("SELECT data FROM user_data WHERE data='pastfixtures_update'")) {

			label = "<html>" + "Update Past Fixtures" + "<br>" + "Last updated at " + Time.getDate(fplSQL.getLong("SELECT value FROM user_data WHERE data='pastfixtures_update'")) + "</html>";

		} else {

			label = "<html>" + "Update Database" + "<br>" + "No database records available" + "</html>";

		}
		
		updatePastFixturesButton = new JButton(label);
		updatePastFixturesButton.setFont(new Font("Serif", Font.BOLD, 16));
		updatePastFixturesButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				ArrayList<Integer> players = fplSQL.getTrimmedPlayers();
				fplSQL.getPastFixtures(players);

				//Set database update time in user_data.
				if (fplSQL.hasRow("SELECT data FROM user_data WHERE data='pastfixtures_update';")) {

					fplSQL.update("UPDATE user_data SET value = " + Time.currentTime() + " WHERE data='pastfixtures_update';");

				} else {

					fplSQL.update("INSERT INTO user_data(data, value) VALUES('pastfixtures_update', " + Time.currentTime() + ");");

				}

				showTeam();

				if (fplSQL.hasRow("SELECT data FROM user_data WHERE data='database_update';") &&
						fplSQL.hasRow("SELECT data FROM user_data WHERE data='pastfixtures_update';") &&
						fplSQL.hasRow("SELECT data FROM user_data WHERE data='playerhistory_update';")) {

					showSquadUpdate();

				}
				
				if (fplSQL.hasRow("SELECT data FROM user_data WHERE data='pastfixtures_update'")) {

					label = "<html>" + "Update Past Fixtures" + "<br>" + "Last updated at " + Time.getDate(fplSQL.getLong("SELECT value FROM user_data WHERE data='pastfixtures_update'")) + "</html>";

				} else {

					label = "<html>" + "Update Database" + "<br>" + "No database records available" + "</html>";

				}
				
				updatePastFixturesButton.setText(label);

			}
		});
		updatePastFixturesButton.setBounds(0, 128, 294, 64);
		desktopPane.add(updatePastFixturesButton);

		infoPanel = new JPanel();
		infoPanel.setBorder(new TitledBorder(null, "", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		infoPanel.setBounds(0, (int) (604/(double)720*height), (int) (1266/(double)1280*width), (int) (79/(double)720*height));
		desktopPane.add(infoPanel);
		infoPanel.setLayout(null);

		databaseLabel = new JLabel();

		if (fplSQL.hasRow("SELECT data FROM user_data WHERE data='database_update';")) {

			databaseLabel = new JLabel("Last updated database at " + Time.getDate(fplSQL.getLong("SELECT value FROM user_data WHERE data='database_update'")));

		} else {

			databaseLabel = new JLabel("The database has never been updated.");

		}


		databaseLabel.setBounds((int) (6/(double)1280*width), (int) (5/(double)720*height), (int) (320/(double)1280*width), (int) (25/(double)720*height));
		databaseLabel.setFont(new Font("Serif", Font.BOLD, 16));

		infoPanel.add(databaseLabel);

		if (fplSQL.hasSquad()) {
			squadValueLabel = new JLabel("Squad value: " + fplSQL.value());
		} else {
			squadValueLabel = new JLabel("No squad has been selected yet");
		}

		squadValueLabel.setBounds((int) (6/(double)1280*width), (int) (25/(double)720*height), (int) (320/(double)1280*width), (int) (45/(double)720*height));
		squadValueLabel.setFont(new Font("Serif", Font.BOLD, 16));

		infoPanel.add(squadValueLabel);

		if (fplSQL.hasRow("SELECT data FROM user_data WHERE data='database_update';") &&
				fplSQL.hasRow("SELECT data FROM user_data WHERE data='pastfixtures_update';") &&
				fplSQL.hasRow("SELECT data FROM user_data WHERE data='playerhistory_update';")) {

			showSquadUpdate();

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

		infoPanel.setBounds(0, (int) (604/(double)720*height), (int) (1266/(double)1280*width), (int) (79/(double)720*height));
		databaseLabel.setBounds((int) (6/(double)1280*width), (int) (5/(double)720*height), (int) (320/(double)1280*width), (int) (25/(double)720*height));
		squadValueLabel.setBounds((int) (6/(double)1280*width), (int) (25/(double)720*height), (int) (320/(double)1280*width), (int) (45/(double)720*height));

		getSquadButton.setBounds((int) (294/(double)1280*width), 0, (int) (294/(double)1280*width), (int) (64/(double)720*height));
		updateSquadButton.setBounds((int) (294/(double)1280*width), (int) (64/(double)720*height), (int) (294/(double)1280*width), (int) (64/(double)720*height));
		updateSquadButtonW.setBounds((int) (294/(double)1280*width), (int) (128/(double)720*height), (int) (294/(double)1280*width), (int) (64/(double)720*height));
		//testSquadButton.setBounds((int) (392/(double)1280*width), (int) (48/(double)720*height), (int) (196/(double)1280*width), (int) (48/(double)720*height));
		updateDatabaseButton.setBounds(0, 0, (int) (294/(double)1280*width), (int) (64/(double)720*height));
		updatePlayerHistoryButton.setBounds(0, (int) (64/(double)720*height), (int) (294/(double)1280*width), (int) (64/(double)720*height));
		updatePastFixturesButton.setBounds(0, (int) (128/(double)720*height), (int) (294/(double)1280*width), (int) (64/(double)720*height));

		scrollPane.setBounds((int) (0/(double)1280*width), (int) (192/(double)720*height), (int) (588/(double)1280*width), (int) (476/(double)720*height));

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

		minutes = fplSQL.getExpectedMinutes(id, gameweek);
		points = fplSQL.getExpectedPoints(id, gameweek);

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
		System.out.println("Expected points for existing team: " + expectedPointsCurrent);			

		ArrayList<PlayerValueCurrent> list = new ArrayList<>();

		for (int id1 : fplSQL.getTrimmedPlayers()) {

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
				//System.out.println("Expected points for this team: " + expectedPoints);
				changed = true;
				transfers = i;
				//Update current squad.
				for (int h = 0; h < 15; h++) {

					currentSquad[h] = squad[h];
					//System.out.println(names[h]);
				}
			}

			if (expectedPointsW > expectedPointsCurrentW) {

				expectedPointsCurrentW = expectedPointsW;
				changedW = true;
				transfersW = i;

				for (int h = 0; h < 15; h++) {

					currentSquadW[h] = squad[h];
					//System.out.println(names[h]);
				}
			}

		}

		String[][] ASquadUpdate = new String[19][2];

		if (!changed) {

			System.out.println("No better team could be found.");

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


		}

		String[] columns = {"Updated Squad","Updated Squad with Wildcard or Free Hit"};

		squadUpdateTable = new JTable(new DefaultTableModel(ASquadUpdate, columns) {

			@Override
			public boolean isCellEditable(int row, int column) {
				//all cells false
				return false;
			}
		});

		squadUpdateTable.setFont(new Font("Serif", Font.BOLD, 16));

		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment( JLabel.CENTER );

		squadUpdateTable.setDefaultRenderer(String.class, centerRenderer);

		squadUpdateTable.setVisible(true);

		scrollPane = new JScrollPane(squadUpdateTable);
		squadUpdateTable.setFillsViewportHeight(true);

		scrollPane.setBounds((int) (0/(double)1280*width), (int) (192/(double)720*height), (int) (588/(double)1280*width), (int) (476/(double)720*height));
		desktopPane.add(scrollPane);

	}
}