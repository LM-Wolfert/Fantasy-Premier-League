package me.laurens.FPL.optimisation;

public class PlayerValueCurrent implements Comparable<PlayerValueCurrent> {
	
public int id;
	
	public int position;
	public int team;
	public double cost;
	
	public double composite;
	
	public PlayerValueCurrent(int id, int position, int team, double cost, double composite) {
		
		this.id = id;
		
		this.position = position;
		this.team = team;
		this.cost = cost;
		
		this.composite = composite;
		
	}

	@Override
	public int compareTo(PlayerValueCurrent p) {
		
		if (this.composite - p.composite > 0) {
			return -1;
		} else if (this.composite - p.composite < 0) {
			return 1;
		} else {
			return 0;
		}
	}
}
