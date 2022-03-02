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
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

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
	private JButton testSquadButton;
	private JButton getTeamButton;
	private JButton updateDatabaseButton;
	private JButton updatePlayerHistoryButton;
	private JButton updatePastFixturesButton;

	private ImageIcon pitchIcon;
	private ImageIcon icon;

	private JTextField[] playerLabels;

	private JDesktopPane desktopPane;

	private int height;
	private int width;

	private double minutes;
	private double points;
	private double expPoints;

	private int teamID;
	private int teamStrength;
	private ArrayList<Integer> opponentTeams;
	private int strengthDiff;

	private FPLSQL fplSQL;

	public Menu(FPLSQL fplSQL) {

		this.fplSQL = fplSQL;

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


			}
		});
		getSquadButton.setBounds(0, 0, 196, 48);
		desktopPane.add(getSquadButton);

		if (fplSQL.hasSquad()) {
			showSquad();
		}

		getTeamButton = new JButton("Get Team for Current Squad");
		getTeamButton.setFont(new Font("Serif", Font.BOLD, 12));
		getTeamButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				if (!fplSQL.hasSquad()) {
					System.out.println("No squad found, please create an initial squad first.");
					return;
				}

				PlayerValueCurrent[] players = new PlayerValueCurrent[15];
				int id;

				double points;

				for (int i = 0; i < 15; i++) {

					id = fplSQL.getId(i);

					points = expectedPoints(id, 28);
					
					players[i] = new PlayerValueCurrent(id, fplSQL.getPosition(id), fplSQL.getTeam(id), fplSQL.getCost(id), points);

				}

				TeamSelection teamSelection = new TeamSelection(players, fplSQL);
				teamSelection.solve();

				teamSelection.show();

			}	
		});
		getTeamButton.setBounds(196, 0, 196, 48);
		desktopPane.add(getTeamButton);

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

					points = expectedPoints(id, 28);

					players[i] = new PlayerValueCurrent(id, fplSQL.getPosition(id), fplSQL.getTeam(id), fplSQL.getCost(id), points);

				}

				TeamSelection teamSelection = new TeamSelection(players, fplSQL);
				teamSelection.solve();
				double expectedPointsCurrent = teamSelection.expectedPoints(0);
				System.out.println("Expected points for existing team: " + expectedPointsCurrent);			

				ArrayList<PlayerValueCurrent> list = new ArrayList<>();
				ArrayList<Integer> player_ids = fplSQL.getPlayers();

				for (int id1 : player_ids) {

					points = expectedPoints(id1, 28);

					list.add(new PlayerValueCurrent(id1, fplSQL.getPosition(id1), fplSQL.getTeam(id1), fplSQL.getCost(id1), points));					

				}

				ArrayList<Integer> teams = fplSQL.getTeamIDs();

				SquadUpdate solver = new SquadUpdate(list, teams, fplSQL);

				//Test optimal squad for up to 11 transfers.
				//More than 11 transfers would be counter-intuitive since it would mean the bench is transferred also.
				boolean changed = false;
				int transfers = 0;
				String[] names = new String[15];
				for (int i = 1; i <= 11; i++) {

					solver.setTransferCount(i);
					solver.setup();
					solver.solve();


					int[] squad = solver.squad;
					squad = fplSQL.sortByPosition(squad);
					names = fplSQL.getNames(squad);

					for (int j = 0; j < 15; j++) {

						id = squad[j];

						points = expectedPoints(id, 28);

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
					fplSQL.setMoneyRemaining(currentValue-newValue);

					showSquad();

				}

			}
		});
		updateSquadButton.setBounds(196, 48, 196, 48);
		desktopPane.add(updateSquadButton);

		updateSquadButtonW = new JButton("Update Current Squad - Wildcard");
		updateSquadButtonW.setFont(new Font("Serif", Font.BOLD, 12));
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

					points = expectedPoints(id, 28);

					players[i] = new PlayerValueCurrent(id, fplSQL.getPosition(id), fplSQL.getTeam(id), fplSQL.getCost(id), points);

				}

				TeamSelection teamSelection = new TeamSelection(players, fplSQL);
				teamSelection.solve();
				double expectedPointsCurrent = teamSelection.expectedPoints(0);
				System.out.println("Expected points for existing team: " + expectedPointsCurrent);			

				ArrayList<PlayerValueCurrent> list = new ArrayList<>();
				ArrayList<Integer> player_ids = fplSQL.getPlayers();

				for (int id1 : player_ids) {

					points = expectedPoints(id1, 28);

					list.add(new PlayerValueCurrent(id1, fplSQL.getPosition(id1), fplSQL.getTeam(id1), fplSQL.getCost(id1), points));					

				}

				ArrayList<Integer> teams = fplSQL.getTeamIDs();

				SquadUpdate solver = new SquadUpdate(list, teams, fplSQL);

				//Test optimal squad for up to 11 transfers.
				//More than 11 transfers would be counter-intuitive since it would mean the bench is transferred also.
				boolean changed = false;
				int transfers = 0;
				String[] names = new String[15];
				for (int i = 1; i <= 11; i++) {

					solver.setTransferCount(i);
					solver.setup();
					solver.solve();


					int[] squad = solver.squad;
					squad = fplSQL.sortByPosition(squad);
					names = fplSQL.getNames(squad);

					for (int j = 0; j < 15; j++) {

						id = squad[j];

						points = expectedPoints(id, 28);

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
					fplSQL.setMoneyRemaining(currentValue-newValue);

					showSquad();

				}

			}
		});
		updateSquadButtonW.setBounds(196, 96, 196, 48);
		desktopPane.add(updateSquadButtonW);

		testSquadButton = new JButton("Update Current Squad - Test");
		testSquadButton.setFont(new Font("Serif", Font.BOLD, 12));
		testSquadButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

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

					points = expectedPoints(id, 28);

					players[i] = new PlayerValueCurrent(id, fplSQL.getPosition(id), fplSQL.getTeam(id), fplSQL.getCost(id), points);

				}

				TeamSelection teamSelection = new TeamSelection(players, fplSQL);
				teamSelection.solve();
				double expectedPointsCurrent = teamSelection.expectedPoints(0);
				double expectedPointsCurrentW = expectedPointsCurrent;
				System.out.println("Expected points for existing team: " + expectedPointsCurrent);			

				ArrayList<PlayerValueCurrent> list = new ArrayList<>();

				for (int id1 : fplSQL.getTrimmedPlayers()) {

					points = expectedPoints(id1, 28);

					list.add(new PlayerValueCurrent(id1, fplSQL.getPosition(id1), fplSQL.getTeam(id1), fplSQL.getCost(id1), points));					

				}

				ArrayList<Integer> teams = fplSQL.getTeamIDs();

				SquadUpdate solver = new SquadUpdate(list, teams, fplSQL);

				//Test optimal squad for up to 11 transfers.
				//More than 11 transfers would be counter-intuitive since it would mean the bench is transferred also.
				boolean changed = false;
				boolean changedW = false;
				int transfers = 0;
				double squadPoints = 0;
				double squadPointsW = 0;
				String[] names = new String[15];
				for (int i = 1; i <= 11; i++) {

					solver.setTransferCount(i);
					solver.setup();
					solver.solve();

					int[] squad = solver.squad;
					squad = fplSQL.sortByPosition(squad);
					names = fplSQL.getNames(squad);

					for (int j = 0; j < 15; j++) {

						id = squad[j];

						points = expectedPoints(id, 28);

						players[j] = new PlayerValueCurrent(id, fplSQL.getPosition(id), fplSQL.getTeam(id), fplSQL.getCost(id), points);

					}

					teamSelection = new TeamSelection(players, fplSQL);
					teamSelection.solve();

					//For each transfer over 1 add a modifier of -4 points.
					int modifier = (i-1) * -4;
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

						for (int h = 0; h < 15; h++) {

							currentSquadW[h] = squad[h];
							//System.out.println(names[h]);
						}
					}

				}
				if (!changed) {

					System.out.println("No better team could be found.");
				} else {

					System.out.println("Optimal Squad with " + transfers + " transfers.");
					System.out.println("Expected points for this team: " + expectedPointsCurrent);
					names = fplSQL.getNames(currentSquad);

					for (int i = 0; i < 15; i++) {

						System.out.println(names[i]);
						squadPoints += fplSQL.getAveragePoints(currentSquad[i]);

					}

					System.out.println("Average expected points for squad: " + squadPoints);

				}

				if (changedW) {

					System.out.println("Optimal Squad with wildcard.");
					System.out.println("Expected points for this team: " + expectedPointsCurrentW);
					names = fplSQL.getNames(currentSquadW);

					for (int i = 0; i < 15; i++) {

						System.out.println(names[i]);
						squadPointsW += fplSQL.getAveragePoints(currentSquadW[i]);

					}

					System.out.println("Average expected points for squad: " + squadPointsW);

				}
			}
		});
		testSquadButton.setBounds(392, 48, 196, 48);
		desktopPane.add(testSquadButton);

		updateDatabaseButton = new JButton("Update Database");
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

				fplSQL.updateUserData();		
				databaseLabel.setText("Last updated database at " + Time.getDate(fplSQL.getTime()));
			}
		});
		updateDatabaseButton.setBounds(0, 48, 196, 48);
		desktopPane.add(updateDatabaseButton);

		updatePlayerHistoryButton = new JButton("Update Player History");
		updatePlayerHistoryButton.setFont(new Font("Serif", Font.BOLD, 16));
		updatePlayerHistoryButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				fplSQL.updatePlayerHistory();

			}
		});
		updatePlayerHistoryButton.setBounds(0, 96, 196, 48);
		desktopPane.add(updatePlayerHistoryButton);

		updatePastFixturesButton = new JButton("Update Past Fixtures");
		updatePastFixturesButton.setFont(new Font("Serif", Font.BOLD, 16));
		updatePastFixturesButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				ArrayList<Integer> players = fplSQL.getTrimmedPlayers();
				fplSQL.getPastFixtures(players);

			}
		});
		updatePastFixturesButton.setBounds(0, 144, 196, 48);
		desktopPane.add(updatePastFixturesButton);

		infoPanel = new JPanel();
		infoPanel.setBorder(new TitledBorder(null, "", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		infoPanel.setBounds(0, (int) (604/(double)720*height), (int) (1266/(double)1280*width), (int) (79/(double)720*height));
		desktopPane.add(infoPanel);
		infoPanel.setLayout(null);

		databaseLabel = new JLabel();

		if (fplSQL.getTime() == 0 ) {
			databaseLabel = new JLabel("The database has never been updated.");
		} else {
			databaseLabel = new JLabel("Last updated database at " + Time.getDate(fplSQL.getTime()));
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
		databaseLabel.setBounds((int) (6/(double)1280*width), (int) (15/(double)720*height), (int) (196/(double)1280*width), (int) (48/(double)720*height));

		getSquadButton.setBounds(0, 0, (int) (196/(double)1280*width), (int) (48/(double)720*height));
		updateSquadButton.setBounds((int) (196/(double)1280*width), (int) (48/(double)720*height), (int) (196/(double)1280*width), (int) (48/(double)720*height));
		updateSquadButton.setBounds((int) (196/(double)1280*width), (int) (96/(double)720*height), (int) (196/(double)1280*width), (int) (48/(double)720*height));
		testSquadButton.setBounds((int) (392/(double)1280*width), (int) (48/(double)720*height), (int) (196/(double)1280*width), (int) (48/(double)720*height));
		getTeamButton.setBounds((int) (196/(double)1280*width), 0, (int) (196/(double)1280*width), (int) (48/(double)720*height));
		updateDatabaseButton.setBounds(0, (int) (49/(double)720*height), (int) (196/(double)1280*width), (int) (48/(double)720*height));

		resizeSquad();
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

	private double expectedPoints(int id, int gameweek) {

		minutes = fplSQL.getExpectedMinutes(id, 28);
		points = fplSQL.getExpectedPoints(id, 28);

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


		//Get team strength of player.
		teamID = fplSQL.getInt("SELECT id FROM teams WHERE code=" + fplSQL.getTeam(id) + ";");
		teamStrength = fplSQL.getInt("SELECT strength FROM teams WHERE id=" + teamID + ";");

		//Get opponent teams for all upcoming fixtures in gameweek.
		opponentTeams = fplSQL.opponentTeams(teamID, gameweek);

		expPoints = 0;

		//If the team has no fixtures in the next gameweek, return 0 expected points.
		if (opponentTeams.size() == 0) {

			return expPoints;

		}

		//Iterate trough all fixtures and calculate expected points.
		for (int i : opponentTeams) {

			//Get the strength difference.
			strengthDiff = teamStrength - fplSQL.getInt("SELECT strength FROM teams WHERE id=" + i + ";");

			//Add multipliers for each strength difference.
			//A strength difference of 1 is disregarded.
			switch (strengthDiff) {

			case -4:
				expPoints += points * 0.4;
				break;
			case -3:
				expPoints += points * 0.6;
				break;
			case -2:
				expPoints += points * 0.8;
				break;
			case 2:
				expPoints += points * 1.2;
				break;
			case 3:
				expPoints += points * 1.4;
				break;
			case 4:
				expPoints += points * 1.6;
				break;
			default:
				expPoints += points;
			}

		}

		return expPoints;		

	}

}