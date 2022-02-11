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
import me.laurens.FPL.sql.GetFixtures;
import me.laurens.FPL.sql.GetGameweeks;
import me.laurens.FPL.sql.GetPlayerHistory;
import me.laurens.FPL.sql.GetPlayers;
import me.laurens.FPL.sql.GetTeams;
import me.laurens.FPL.sql.SquadData;
import me.laurens.FPL.sql.UserData;

public class Menu {

	private JLabel pitchLabel;
	private JLabel databaseLabel;
	private JLabel squadValueLabel;

	private JPanel infoPanel;

	private JButton getSquadButton;
	private JButton updateSquadButton;
	private JButton testSquadButton;
	private JButton getTeamButton;
	private JButton updateDatabaseButton;

	private ImageIcon pitchIcon;
	private ImageIcon icon;

	private JTextField[] playerLabels;

	private JDesktopPane desktopPane;

	private int height;
	private int width;

	private SquadData squadData;

	public Menu(GetPlayers getPlayers, GetTeams getTeams, GetGameweeks getGameweeks, GetFixtures getFixtures, GetPlayerHistory getPlayerHistory, UserData userData, SquadData squadData) {

		this.squadData = squadData;

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
				ArrayList<Integer> player_ids = getPlayers.getPlayers();

				double max_ict = getPlayers.maxIct();
				double max_form = getPlayers.maxForm();
				double max_points = getPlayers.maxPoints();
				double max_minutes = getPlayers.maxMinutes();

				double hist_max_points = getPlayerHistory.maxPoints();
				double hist_max_ict = getPlayerHistory.maxIct();
				double hist_max_minutes = getPlayerHistory.maxMinutes();

				double history;
				double ict_comp;
				double form_comp;
				double points_comp;
				double minutes_comp;

				for (int id : player_ids) {

					if (getPlayerHistory.hasHistory(id)) {

						history = getPlayerHistory.getComp(id, hist_max_points, hist_max_ict, hist_max_minutes);

					} else {

						history = 1;

					}

					ict_comp = getPlayers.getIctComp(id, max_ict);
					form_comp = getPlayers.getFormComp(id, max_form);
					points_comp = getPlayers.getPointsComp(id, max_points);
					minutes_comp = getPlayers.getMinutesComp(id, max_minutes);

					list.add(new PlayerValueInitial(id, getPlayers.getPosition(id), getPlayers.getTeam(id), getPlayers.getCost(id), ict_comp, form_comp, history, points_comp, minutes_comp));					

				}

				ArrayList<Integer> teams = getTeams.getTeamIDs();

				SquadSolver solver = new SquadSolver(list, teams);
				solver.setup();
				solver.solve();

				int[] squad = solver.squad;
				squad = getPlayers.sortByPosition(squad);
				String[] names = getPlayers.getNames(squad);

				for (int i = 0; i < 15; i++) {
					squadData.setPosition(i, squad[i], names[i]);
				}

				showSquad();


			}
		});
		getSquadButton.setBounds(0, 0, 196, 48);
		desktopPane.add(getSquadButton);

		if (squadData.hasSquad()) {
			showSquad();
		}

		getTeamButton = new JButton("Get Team for Current Squad");
		getTeamButton.setFont(new Font("Serif", Font.BOLD, 12));
		getTeamButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				if (!squadData.hasSquad()) {
					System.out.println("No squad found, please create an initial squad first.");
					return;
				}

				PlayerValueCurrent[] players = new PlayerValueCurrent[15];
				int id;

				for (int i = 0; i < 15; i++) {

					id = squadData.getId(i);
					players[i] = new PlayerValueCurrent(id, getPlayers.getPosition(id), getPlayers.getTeam(id), getPlayers.getCost(id), getPlayers.getEp(id), getPlayers.getPlayChance(id));

				}

				TeamSelection teamSelection = new TeamSelection(players, getPlayers);
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
				//Get the expected points for the current team;
				PlayerValueCurrent[] players = new PlayerValueCurrent[15];
				int[] currentSquad = new int[15];

				for (int i = 0; i < 15; i++) {

					id = squadData.getId(i);
					currentSquad[i] = id;
					players[i] = new PlayerValueCurrent(id, getPlayers.getPosition(id), getPlayers.getTeam(id), getPlayers.getCost(id), getPlayers.getEp(id), getPlayers.getPlayChance(id));

				}

				TeamSelection teamSelection = new TeamSelection(players, getPlayers);
				teamSelection.solve();
				double expectedPointsCurrent = teamSelection.expectedPoints(0);
				System.out.println("Expected points for existing team: " + expectedPointsCurrent);			

				ArrayList<PlayerValueCurrent> list = new ArrayList<>();
				ArrayList<Integer> player_ids = getPlayers.getPlayers();

				for (int id1 : player_ids) {

					list.add(new PlayerValueCurrent(id1, getPlayers.getPosition(id1), getPlayers.getTeam(id1), getPlayers.getCost(id1), getPlayers.getEp(id1), getPlayers.getPlayChance(id1)));					

				}

				ArrayList<Integer> teams = getTeams.getTeamIDs();

				SquadUpdate solver = new SquadUpdate(list, teams, squadData, userData);

				//Test optimal squad for up to 11 transfers.
				//More than 11 transfers would be counter-intuitive since it would mean the bench is transferred also.
				boolean changed = false;
				String[] names = new String[15];
				for (int i = 1; i <= 11; i++) {

					solver.setTransferCount(i);
					solver.setup();
					solver.solve();

					int[] squad = solver.squad;
					squad = getPlayers.sortByPosition(squad);
					names = getPlayers.getNames(squad);

					for (int j = 0; j < 15; j++) {

						id = squad[j];
						players[j] = new PlayerValueCurrent(id, getPlayers.getPosition(id), getPlayers.getTeam(id), getPlayers.getCost(id), getPlayers.getEp(id), getPlayers.getPlayChance(id));

					}

					teamSelection = new TeamSelection(players, getPlayers);
					teamSelection.solve();

					//For each transfer over 1 add a modifier of -4 points.
					int modifier = (i-1) * -4;
					double expectedPoints = teamSelection.expectedPoints(modifier);

					if (expectedPoints > expectedPointsCurrent) {
						//Update current value.
						expectedPointsCurrent = expectedPoints;
						System.out.println("Expected points for this team: " + expectedPoints);
						changed = true;
						//Update current squad.
						for (int h = 0; h < 15; h++) {

							currentSquad[h] = squad[h];
							System.out.println(names[h]);
						}
					}

				}
				if (!changed) {

					System.out.println("No better team could be found.");
				} else {

					ArrayList<Integer> existingSquad = squadData.getSquad();

					int currentValue = squadData.sellValues();

					for (int i = 0; i < 15; i++) {

						if (existingSquad.contains(currentSquad[i])) {

							squadData.setPosition(i, currentSquad[i], names[i]);

						} else {

							squadData.setPosition(i, currentSquad[i], names[i], getPlayers.getCost(currentSquad[i]));

						}
					}

					int newValue = squadData.sellValues();
					userData.setMoneyRemaining(currentValue-newValue);

					showSquad();
					
				}

			}
		});
		updateSquadButton.setBounds(196, 48, 196, 48);
		desktopPane.add(updateSquadButton);

		testSquadButton = new JButton("Update Current Squad - Test");
		testSquadButton.setFont(new Font("Serif", Font.BOLD, 12));
		testSquadButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				int id;
				//Get the expected points for the current team;
				PlayerValueCurrent[] players = new PlayerValueCurrent[15];
				int[] currentSquad = new int[15];

				for (int i = 0; i < 15; i++) {

					id = squadData.getId(i);
					currentSquad[i] = id;
					players[i] = new PlayerValueCurrent(id, getPlayers.getPosition(id), getPlayers.getTeam(id), getPlayers.getCost(id), getPlayers.getEp(id), getPlayers.getPlayChance(id));

				}

				TeamSelection teamSelection = new TeamSelection(players, getPlayers);
				teamSelection.solve();
				double expectedPointsCurrent = teamSelection.expectedPoints(0);
				System.out.println("Expected points for existing team: " + expectedPointsCurrent);			

				ArrayList<PlayerValueCurrent> list = new ArrayList<>();
				ArrayList<Integer> player_ids = getPlayers.getPlayers();

				for (int id1 : player_ids) {

					list.add(new PlayerValueCurrent(id1, getPlayers.getPosition(id1), getPlayers.getTeam(id1), getPlayers.getCost(id1), getPlayers.getEp(id1), getPlayers.getPlayChance(id1)));					

				}

				ArrayList<Integer> teams = getTeams.getTeamIDs();

				SquadUpdate solver = new SquadUpdate(list, teams, squadData, userData);

				//Test optimal squad for up to 11 transfers.
				//More than 11 transfers would be counter-intuitive since it would mean the bench is transferred also.
				boolean changed = false;
				for (int i = 1; i <= 11; i++) {

					solver.setTransferCount(i);
					solver.setup();
					solver.solve();

					int[] squad = solver.squad;
					squad = getPlayers.sortByPosition(squad);
					String[] names = getPlayers.getNames(squad);

					for (int j = 0; j < 15; j++) {

						id = squad[j];
						System.out.println(names[j]);
						players[j] = new PlayerValueCurrent(id, getPlayers.getPosition(id), getPlayers.getTeam(id), getPlayers.getCost(id), getPlayers.getEp(id), getPlayers.getPlayChance(id));

					}

					teamSelection = new TeamSelection(players, getPlayers);
					teamSelection.solve();

					//For each transfer over 1 add a modifier of -4 points.
					int modifier = (i-1) * -4;
					double expectedPoints = teamSelection.expectedPoints(modifier);

					if (expectedPoints > expectedPointsCurrent) {
						//Update current value.
						expectedPointsCurrent = expectedPoints;
						System.out.println("Expected points for this team: " + expectedPoints);
						changed = true;
						//Update current squad.
						for (int h = 0; h < 15; h++) {

							currentSquad[h] = squad[h];
							System.out.println(names[h]);
						}
					}

				}
				if (!changed) {
					System.out.println("No better team could be found.");
				}
			}
		});
		testSquadButton.setBounds(392, 48, 196, 48);
		desktopPane.add(testSquadButton);

		updateDatabaseButton = new JButton("Update Database");
		updateDatabaseButton.setFont(new Font("Serif", Font.BOLD, 16));
		updateDatabaseButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				getPlayers.readJson();
				getPlayers.updateDatabase();

				getTeams.readJson();
				getTeams.updateDatabase();

				getGameweeks.readJson();
				getGameweeks.updateDatabase();

				getFixtures.readJson();
				getFixtures.updateDatabase();

				getPlayerHistory.update();

				userData.updateDatabase();		
				databaseLabel.setText("Last updated database at " + Time.getDate(userData.getTime()));
			}
		});
		updateDatabaseButton.setBounds(0, 48, 196, 48);
		desktopPane.add(updateDatabaseButton);

		infoPanel = new JPanel();
		infoPanel.setBorder(new TitledBorder(null, "", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		infoPanel.setBounds(0, (int) (604/(double)720*height), (int) (1266/(double)1280*width), (int) (79/(double)720*height));
		desktopPane.add(infoPanel);
		infoPanel.setLayout(null);

		databaseLabel = new JLabel();

		if (userData.getTime() == 0 ) {
			databaseLabel = new JLabel("The database has never been updated.");
		} else {
			databaseLabel = new JLabel("Last updated database at " + Time.getDate(userData.getTime()));
		}

		databaseLabel.setBounds((int) (6/(double)1280*width), (int) (5/(double)720*height), (int) (320/(double)1280*width), (int) (25/(double)720*height));
		databaseLabel.setFont(new Font("Serif", Font.BOLD, 16));

		infoPanel.add(databaseLabel);

		if (squadData.hasSquad()) {
			squadValueLabel = new JLabel("Squad value: " + squadData.value());
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

			playerLabels[i] = new JTextField(squadData.getName(i));

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
				playerLabels[i].setBounds((int) (832/(double)1280*width), (int) (275/(double)720*height), (int) (200/(double)1280*width), (int) (20/(double)720*height));
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
				playerLabels[i].setBounds((int) (958/(double)1280*width), (int) (275/(double)720*height), (int) (200/(double)1280*width), (int) (20/(double)720*height));
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
				playerLabels[i].setBounds((int) (1084/(double)1280*width), (int) (275/(double)720*height), (int) (200/(double)1280*width), (int) (20/(double)720*height));
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

				playerLabels[i].setBounds((int) (832/(double)1280*width), (int) (275/(double)720*height), (int) (200/(double)1280*width), (int) (20/(double)720*height));
				break;

			case 9:

				playerLabels[i].setBounds((int) (885/(double)1280*width), (int) (305/(double)720*height), (int) (200/(double)1280*width), (int) (20/(double)720*height));
				break;

			case 10:

				playerLabels[i].setBounds((int) (958/(double)1280*width), (int) (275/(double)720*height), (int) (200/(double)1280*width), (int) (20/(double)720*height));
				break;

			case 11:

				playerLabels[i].setBounds((int) (1031/(double)1280*width), (int) (305/(double)720*height), (int) (200/(double)1280*width), (int) (20/(double)720*height));
				break;

			case 12:

				playerLabels[i].setBounds((int) (1084/(double)1280*width), (int) (275/(double)720*height), (int) (200/(double)1280*width), (int) (20/(double)720*height));
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

}