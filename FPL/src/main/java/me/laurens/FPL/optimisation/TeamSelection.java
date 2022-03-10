package me.laurens.FPL.optimisation;

import java.util.Arrays;

import me.laurens.FPL.sql.FPLSQL;

//Deals with the selection of the team and any potential modifiers that are applied.

//Team selection will maximise the point output for that given gameweek.
//This can be done by means of expected points multiplied by change to play.
//Captain will be given to highest expected points, vice to second highest.
//Bench will be in order of expected points.
public class TeamSelection {

	private PlayerValueCurrent[] players;
	private FPLSQL fplSQL;

	private int[] team;
	private int[] bench;

	public TeamSelection(PlayerValueCurrent[] players, FPLSQL fplSQL) {
		this.players = players;
		this.fplSQL = fplSQL;
	}

	public void solve() {

		Arrays.sort(players);

		team = new int[11];
		bench = new int[4];

		//Count values
		int teamCount = 0;
		int benchCount = 1;

		//Need at least 1;
		boolean goalkeeper = false;
		boolean forward = false;

		//Need at least 3;
		int defender = 0;

		//Other players allowed.
		int other = 6;

		//Iterate through squad.
		for (PlayerValueCurrent p : players) {

			//If player is goalkeeper and we have no goalkeepers add them to the team.
			if (p.position == 1 && goalkeeper == false) {
				goalkeeper = true;
				team[teamCount] = p.id;
				teamCount++;
			}

			//If player is forward and we have no forwards add them to the team.
			else if (p.position == 4 && forward == false) {
				forward = true;
				team[teamCount] = p.id;
				teamCount++;
			}

			//If player is defender and we have less than 3 defenders add them to the team.
			else if (p.position == 2 && defender < 3) {
				defender++;
				team[teamCount] = p.id;
				teamCount++;
			}

			//If player is a goalkeeper, and we already have a goalkeeper, add them to the bench.
			else if (p.position == 1 && goalkeeper) {
				bench[0] = p.id;
			}

			//If other players are still allowed add them to the team.
			else if (other > 0) {
				other--;
				team[teamCount] = p.id;
				teamCount++;	
			}

			//Anyone left at the end is added to the bench.
			else {
				bench[benchCount] = p.id;
				benchCount++;	
			}			
		}		
	}

	public void show() {

		System.out.println("Starting Eleven:");

		String[] teamNames = fplSQL.getNames(team);
		String[] benchNames = fplSQL.getNames(bench);

		for (int i = 0; i < 11; i++) {

			for (int j = 0; j < 15; j++) {

				if (players[j].id == team[i]) {

					System.out.println(teamNames[i] + ", expected points: " + players[j].composite);

				}

			}
		}

		System.out.println("\nBench:");

		for (int i = 0; i < 4; i++) {

			for (int j = 0; j < 15; j++) {

				if (players[j].id == bench[i]) {

					System.out.println(benchNames[i] + ", expected points: " + players[j].composite);

				}

			}

		}
	}
	
	public String[][] getExpectedPoints() {
		
		String[] teamNames = fplSQL.getNames(team);
		String[] benchNames = fplSQL.getNames(bench);
		
		String[][] expectedPoints = new String[15][2];
		
		int val = 0;

		for (int i = 0; i < 11; i++) {

			for (int j = 0; j < 15; j++) {

				if (players[j].id == team[i]) {
					
					expectedPoints[val][0] = teamNames[i];
					expectedPoints[val][1] = String.format("%.2f", players[j].composite);

					val++;

				}

			}
		}

		for (int i = 0; i < 4; i++) {

			for (int j = 0; j < 15; j++) {

				if (players[j].id == bench[i]) {

					expectedPoints[val][0] = benchNames[i];
					expectedPoints[val][1] = String.format("%.2f", players[j].composite);

					val++;

				}

			}

		}
		
		return expectedPoints;		
		
	}

	public double expectedPoints(double modifier) {

		double sum = 0;

		for (int i = 0; i < 11; i++) {

			sum += getComposite(team[i]);

		}

		return (sum+modifier);
	}

	private double getComposite(int id) {

		for (int i = 0; i < 15; i++) {

			if (players[i].id == id) {

				return players[i].composite;

			}

		}

		return 0;	

	}
}
