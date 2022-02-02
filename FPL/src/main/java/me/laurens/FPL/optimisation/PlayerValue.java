package me.laurens.FPL.optimisation;

public class PlayerValue {
	
	public int id;
	
	public int position;
	public int team;
	public double cost;
	
	public double composite;
	
	public PlayerValue(int id, int position, int team, double cost, double ict_comp, double form_comp, double history_comp, double points_comp, double minutes_comp) {
		
		this.id = id;
		
		this.position = position;
		this.team = team;
		this.cost = cost;
		
		this.composite = ict_comp + form_comp + history_comp + points_comp + minutes_comp;
		
	}

}
