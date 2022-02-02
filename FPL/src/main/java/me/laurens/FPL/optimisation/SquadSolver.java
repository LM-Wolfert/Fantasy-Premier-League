package me.laurens.FPL.optimisation;

import java.util.ArrayList;

import com.google.ortools.Loader;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;

public class SquadSolver {

	private ArrayList<PlayerValue> players;
	private ArrayList<Integer> teams;
	
	private MPSolver solver;
	private MPVariable x;
	
	public int[] squad;
	private int counter;

	public SquadSolver(ArrayList<PlayerValue> players, ArrayList<Integer> teams) {
		this.players = players;
		this.teams = teams;
	}
	
	public void solve() {
		
		final MPSolver.ResultStatus resultStatus = solver.solve();
		
		if (resultStatus == MPSolver.ResultStatus.OPTIMAL) {
			//System.out.println("Solution:");
			
			squad = new int[15];
			counter = 0;
			
			for (MPVariable v : solver.variables()) {
				
				if (v.solutionValue() == 1) {
					
					squad[counter] = Integer.parseInt(v.name());
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
		
		//Make constraints for every team.
		for (int t : teams) {
			
			solver.makeConstraint(0, 3, String.valueOf(t));			
			
		}
		
		//Make constraints for each position.
		solver.makeConstraint(0, 2, "pos1");
		solver.makeConstraint(0, 5, "pos2");
		solver.makeConstraint(0, 5, "pos3");
		solver.makeConstraint(0, 3, "pos4");
		
		//Make cost constraint.
		cost = solver.makeConstraint(0, 1000, "cost");
		
		for (PlayerValue p : players) {
			
			x = solver.makeIntVar(0, 1, String.valueOf(p.id));
			
			cost.setCoefficient(x, p.cost);
			
			for (MPConstraint c : solver.constraints()) {
				
				if (c.name().equals("cost")) {continue;}
				
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
			
			objective.setCoefficient(x, p.composite);
			
		}
		
		objective.setMaximization();
		
	}

}
