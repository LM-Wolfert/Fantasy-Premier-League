package me.laurens.FPL.optimisation;

import java.util.Arrays;

import me.laurens.FPL.sql.GetPlayers;

//Deals with the selection of the team and any potential modifiers that are applied.

//Team selection will maximise the point output for that given gameweek.
//This can be done by means of expected points multiplied by change to play.
//Captain will be given to highest expected points, vice to second highest.
//Bench will be in order of expected points.
public class TeamSelection {

	private PlayerValueCurrent[] players;
	private GetPlayers getPlayers;

	private int[] team;
	private int[] bench;

	public TeamSelection(PlayerValueCurrent[] players, GetPlayers getPlayers) {
		this.players = players;
		this.getPlayers = getPlayers;
	}

	public void solve() {

		Arrays.sort(players);

		team = new int[11];
		bench = new int[4];

		int val = 0;
		//Start at positin 2 since the first is reserved for 2nd goalkeeper.
		int val2 = 1;
		//To check whether the goalkeeper has already been selected.
		boolean goalKeeper = false;

		//Iterate through squad.
		for (PlayerValueCurrent p : players) {

			//If a goalkeeper is already in the team, add them to the bench.
			if (goalKeeper && p.position == 1) {

				bench[0] = p.id;
				continue;

			}

			//If the team has less than 11 players add one.
			if (val < 11) {

				team[val] = p.id;

				//If they are a goalkeeper set the value to true.
				if (p.position == 1) {
					goalKeeper = true;
				}

				val++;
			} else {

				//else add them to the bench.
				bench[val2] = p.id;
				val2++;

			} 		
		}		
	}

	public void show() {

		System.out.println("Starting Eleven:");

		String[] teamNames = getPlayers.getNames(team);
		String[] benchNames = getPlayers.getNames(bench);

		for (int i = 0; i < 11; i++) {

			System.out.println(teamNames[i]);

		}

		System.out.println("\nBench:");

		for (int i = 0; i < 4; i++) {

			System.out.println(benchNames[i]);

		}
	}
}
