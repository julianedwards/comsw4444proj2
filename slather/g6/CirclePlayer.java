package slather.g6;

import java.util.Iterator;
import java.util.Set;

import slather.sim.Cell;
import slather.sim.Move;
import slather.sim.Pherome;
import slather.sim.Player;
import slather.sim.Point;

public class CirclePlayer implements Player {

	private static final int ANGLE_BITS = 5;
	private static final int ANGLE_MIN = 0;
	private static final int ANGLE_MAX = 1 << ANGLE_BITS;
	private static final int ANGLE_MASK = ANGLE_MAX - 1;

	private static final int ROLE_BITS = 1;
	private static final int ROLE_MIN = ANGLE_MAX;
	private static final int ROLE_MAX = ROLE_MIN + (1 << ROLE_BITS);
	private static final int ROLE_MASK = (ROLE_MAX - 1) & ~ANGLE_MASK;
	private static final int ROLE_SCOUT = 0 << ANGLE_BITS;
	private static final int ROLE_CIRCLE = 1 << ANGLE_BITS;
	private static final double TWOPI = 2 * Math.PI;

	private static int cell_vision = 2;
	private double t;
	private double d;
	private double dTheta;

	@Override
	public void init(double d, int t, int sidLength) {
		// TODO Auto-generated method stub
		this.t = t;
		this.d = d;
		
	}

	@Override
	public Move play(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
		// TODO Auto-generated method stub
		 if (player_cell.getDiameter() >= 2) {
             return new Move(true, (byte) -1, (byte) -1);
         }
		return playCircle(player_cell, memory, nearby_cells,
                nearby_pheromes);
	}

	public Move playCircle(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
		double theta = byte2angle(memory);
		double nextTheta = normalizeAngle(theta + dTheta, 0);
		byte nextMemory = 0;
		Point vector = null;
		// Try to go any of four normal directions.
		for (int i = 0; i < 4; ++i) {
			nextMemory = angle2byte(nextTheta, memory);
			vector = extractVectorFromAngle(nextTheta);
			if (!collides(player_cell, vector, nearby_cells, nearby_pheromes))
				return new Move(vector, nextMemory);
			nextTheta += Math.PI / 2;
		}

		// if all tries fail, just chill in place
		return new Move(new Point(0, 0), (byte) 1);
	}

	private double byte2angle(byte b) {
		// -128 <= b < 128
		// -1 <= b/128 < 1
		// -pi <= a < pi
		return normalizeAngle(TWOPI * (((double) ((b) & ANGLE_MASK)) / ANGLE_MAX), 0);
	}

	// group 2's angle2byte method
	private byte angle2byte(double a, byte b) {
		final double actualAngle = ((normalizeAngle(a, 0) / TWOPI) * ANGLE_MAX);
		final int anglePart = (int) (((int) actualAngle) & ANGLE_MASK);
		final byte memoryPart = (byte) (b & ~ANGLE_MASK);
		// System.out.println("angle2byte "+ memoryPart +","+ anglePart +","+
		// (normalizeAngle(a,0)/TWOPI) +","+ ANGLE_MAX +","+ a);
		return (byte) ((anglePart | memoryPart));
	}

	// group 2's normalizeAngle
	private double normalizeAngle(double a, double start) {
		if (a < start) {
			return normalizeAngle(a + TWOPI, start);
		} else if (a >= (start + TWOPI)) {
			return normalizeAngle(a - TWOPI, start);
		} else {
			return a;
		}
	}

	public Point extractVectorFromAngle(double angel) {
		double theta = Math.toRadians(2 * angel);
		double dx = Cell.move_dist * Math.cos(theta);
		double dy = Cell.move_dist * Math.sin(theta);
		return new Point(dx, dy);
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
}
