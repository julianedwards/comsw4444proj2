package slather.g6;

import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import slather.sim.Cell;
import slather.sim.Move;
import slather.sim.Pherome;
import slather.sim.Point;

public class Player implements slather.sim.Player {

	Random gen = new Random();

	@Override
	public void init(double d, int t) {
		// TODO Auto-generated method stub

	}

	@Override
	public Move play(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
		if (player_cell.getDiameter() >= 2) // reproduce whenever possible
			return new Move(true, (byte) -1, (byte) -1);
		if (memory > 0) { // follow previous direction unless it would cause a
							// collision
			Point vector = extractVectorFromAngle((int) memory);
			// check for collisions
			if (!collides(player_cell, vector, nearby_cells, nearby_pheromes))
				return new Move(vector, memory);
		}
		// if all tries fail, find best path and go
		return findBestPath(player_cell, memory, nearby_cells, nearby_pheromes);
	}

	public Move findBestPath(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
		if (memory > 0) {
			// there was a collision. find the best path to go
			for (int i = memory + 90; i < 360; i++) {
				// int arg = gen.nextInt(180) + 1;
				Point vector = extractVectorFromAngle(i);
				if (!collides(player_cell, vector, nearby_cells, nearby_pheromes))
					return new Move(vector, (byte) i);
			}
			for (int i = memory + 90; i > 0; i--) {
				// int arg = gen.nextInt(180) + 1;
				Point vector = extractVectorFromAngle(i);
				if (!collides(player_cell, vector, nearby_cells, nearby_pheromes))
					return new Move(vector, (byte) i);
			}
		} else {
			// there was no memory before. find the best path to go
			for (int i = 0; i < 360; i++) {
				int arg = gen.nextInt(180) + 1;
				Point vector = extractVectorFromAngle(arg);
				if (!collides(player_cell, vector, nearby_cells, nearby_pheromes))
					return new Move(vector, (byte) arg);
			}
		}

		// if all tries fails, find the farthest pherome position and go
		Point farthestPheromePoint = player_cell.getPosition();
		for (Pherome p : nearby_pheromes) {
			if (!collides(player_cell, p.getPosition(), nearby_cells, nearby_pheromes)) {
				if (player_cell.getPosition().distance(p.getPosition()) > player_cell.getPosition()
						.distance(farthestPheromePoint)) {
					farthestPheromePoint = p.getPosition();
				}
			}
		}
		return new Move(farthestPheromePoint, (byte) 0);
	}

	private boolean collides(Cell player_cell, Point vector, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
		Iterator<Cell> cell_it = nearby_cells.iterator();
		Point destination = player_cell.getPosition().move(vector);
		while (cell_it.hasNext()) {
			Cell other = cell_it.next();
			if (destination.distance(other.getPosition()) < 0.5 * player_cell.getDiameter() + 0.5 * other.getDiameter()
					+ 0.00011)
				return true;
		}
		Iterator<Pherome> pherome_it = nearby_pheromes.iterator();
		while (pherome_it.hasNext()) {
			Pherome other = pherome_it.next();
			if (other.player != player_cell.player
					&& destination.distance(other.getPosition()) < 0.5 * player_cell.getDiameter() + 0.0001)
				return true;
		}
		return false;
	}

	public Point extractVectorFromAngle(int angel) {
		double theta = Math.toRadians(2 * angel);
		double dx = Cell.move_dist * Math.cos(theta);
		double dy = Cell.move_dist * Math.sin(theta);
		return new Point(dx, dy);
	}

}
