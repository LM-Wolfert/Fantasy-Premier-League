package me.laurens.FPL.optimisation;

import java.util.ArrayList;

import com.google.ortools.Loader;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;

import me.laurens.FPL.sql.SquadData;
import me.laurens.FPL.sql.UserData;

//Deals with potential transfers.
//Similar to the squadsolver method but for each transfer over 1 the cost is 3 points.
//Must find an expected points value and compare it to that of the current squad to choose whether it's worth it. 
//Thus we must not just look at optimal values, but also other ones.
//Basically select all squads with more expected points than the current one and decide which is the best option from that.
//Most important variable is expected points.
public class SquadUpdate {
	
	private ArrayList<PlayerValueCurrent> players;
	private ArrayList<Integer> teams;
	
	private SquadData squadData;
	private UserData userData;
	
	private MPSolver solver;
	private MPVariable x;
	
	public int[] squad;
	private int counter;
	
	private int transferCount;

	public SquadUpdate(ArrayList<PlayerValueCurrent> players, ArrayList<Integer> teams, SquadData squadData, UserData userData) {
		this.players = players;
		this.teams = teams;
		this.squadData = squadData;
		this.userData = userData;
		
		transferCount = 11;
	}
	
	public void setTransferCount(int x) {
		transferCount = x;
	}
	
	public void solve() {
		
		final MPSolver.ResultStatus resultStatus = solver.solve();

		if (resultStatus == MPSolver.ResultStatus.OPTIMAL) {
		
			squad = new int[15];
			counter = 0;
			
			//System.out.println(solver.objective().value());
			
			for (MPVariable v : solver.variables()) {
				
				if (v.solutionValue() == 1) {
					
					squad[counter] = Integer.parseInt(v.name());
					//System.out.println(v.name());
					counter++;
					
				}			
			}
		}		
	}

	public void setup() {
		
		Loader.loadNativeLibraries();
		solver = MPSolver.createSolver("SCIP");

		MPObjective objective = solver.objective();
		
		MPConstraint cost;
		MPConstraint transfers;
		
		//Make constraints for every team.
		for (int t : teams) {
			
			solver.makeConstraint(0, 3, String.valueOf(t));			
			
		}
		
		//Make constraints for each position.
		solver.makeConstraint(2, 2, "pos1");
		solver.makeConstraint(5, 5, "pos2");
		solver.makeConstraint(5, 5, "pos3");
		solver.makeConstraint(3, 3, "pos4");
		
		//Make cost constraint.
		cost = solver.makeConstraint(0, squadData.sellValues() + userData.getMoneyRemaining(), "cost");
		transfers = solver.makeConstraint(transferCount, transferCount, "transfers");
		
		for (PlayerValueCurrent p : players) {
			
			x = solver.makeIntVar(0, 1, String.valueOf(p.id));
			
			cost.setCoefficient(x, p.cost);
			
			for (MPConstraint c : solver.constraints()) {
				
				if (c.name().equals("cost") || c.name().equals("transfers")) {continue;}
				
				switch (c.name()) {
				
				case "pos1":
					if (p.position == 1) {
						c.setCoefficient(x, 1);
					}
					break;
				
				case "pos2":
					if (p.position == 2) {
						c.setCoefficient(x, 1);
					}
					break;
				
				case "pos3":
					if (p.position == 3) {
						c.setCoefficient(x, 1);
					}
					break;
					
				case "pos4":
					if (p.position == 4) {
						c.setCoefficient(x, 1);					
					}
					break;			
				
				default:
					if (Integer.parseInt(c.name()) == p.team) {
						c.setCoefficient(x, 1);
					}
				}
				
			}
			
			//Determines how many transfers can be made.
			if (squadData.inSquad(p.id)) {
				transfers.setCoefficient(x, 0);
			} else {
				transfers.setCoefficient(x, 1);
			}	
			
			objective.setCoefficient(x, p.composite);
		}
		
		objective.setMaximization();
		
	}

}
