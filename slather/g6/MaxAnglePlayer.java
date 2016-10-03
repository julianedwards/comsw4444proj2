package slather.g6;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import slather.sim.Cell;
import slather.sim.GridObject;
import slather.sim.Move;
import slather.sim.Pherome;
import slather.sim.Player;
import slather.sim.Point;

public class MaxAnglePlayer implements Player {

	private static int cell_vision = 2;
	private double t;
	private double d;

	private class GridObjects {
		Point vector;
		GridObject ob;

		public GridObjects(GridObject obj, Point ang) {
			vector = ang;
			ob = obj;
		}
	}

	private class CellComparator implements Comparator<GridObjects> {
		@Override
		public int compare(GridObjects a, GridObjects b) {
			double one = Math.atan2(a.vector.y, a.vector.x);
			double two = Math.atan2(b.vector.y, b.vector.x);
			if (one == two)
				return 0;
			return one < two ? -1 : 1;
		}
	}

	@Override
	public void init(double d, int t, int sideLength) {
		this.t = t;
		this.d = d;
	}

	@Override
	public Move play(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
		// reproduce whenever possible
		if (player_cell.getDiameter() >= 2) {
			return new Move(true, (byte) 0, (byte) 0);
		}
		Point vector = new Point(0,0);
		if (!nearby_cells.isEmpty()) {
			Set<Cell> cells = findCellInRange(nearby_cells, player_cell);
			if (cells.size() == 1) {
				vector = avoidCell(player_cell, cells.iterator().next());
			} else if (cells.size() >= 2) {
				// find best angle
				vector = findBestDirection(cells, player_cell);
			}
		}
		if (vector.x != 0 && vector.y != 0) {
			if (!collides(player_cell, vector, nearby_cells, nearby_pheromes)) {
				return new Move(vector, (byte) (int) ((Math.toDegrees(Math.atan2(vector.y, vector.x)) / 2)));
			}
		} else {
			// follow previous direction
			vector = extractVectorFromAngle((int) memory);
			if (!collides(player_cell, vector, nearby_cells, nearby_pheromes))
				return new Move(vector, memory);
		}
		
		for (int i = 0; i < 180; i++) {
			Random gen = new Random();
			int arg = gen.nextInt(180) + 1;
			vector = extractVectorFromAngle(arg);
			if (!collides(player_cell, vector, nearby_cells, nearby_pheromes))
				return new Move(vector, (byte) arg);
		}

		// if all tries fail, just chill in place
		return new Move(new Point(0, 0), (byte) 0);
	}

	private Point findBestDirection(Set<Cell> cells, Cell player_cell) {
		// TODO Auto-generated method stub
		Cell[] sortedCells = sortCell(cells, player_cell);
		int largestAngle = Integer.MIN_VALUE;
		Cell current = sortedCells[0];
		int directionAngle = extractAngleFromVector(current.getPosition(), player_cell);

		for (int i = 1; i < sortedCells.length && sortedCells[i] != null; i++) {
			int currentAngle = Math.abs(extractAngleFromVector(sortedCells[i].getPosition(), player_cell)
					- extractAngleFromVector(current.getPosition(), player_cell));
			if (currentAngle > largestAngle) {
				largestAngle = currentAngle;
				directionAngle = extractAngleFromVector(sortedCells[i].getPosition(), player_cell) - largestAngle / 2;
			}
			current = sortedCells[i];
		}
		if (sortedCells[sortedCells.length - 1] != null) {
			int currentAngle = Math
					.abs(extractAngleFromVector(sortedCells[sortedCells.length - 1].getPosition(), player_cell)
							- extractAngleFromVector(sortedCells[0].getPosition(), player_cell));
			if (currentAngle > largestAngle) {
				largestAngle = currentAngle;
				directionAngle = extractAngleFromVector(sortedCells[0].getPosition(), player_cell) - largestAngle / 2;
			}
		}

		return extractVectorFromAngle(directionAngle);
	}

	private Point findBestDirection(Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes, Cell player_cell) {
		double largestAngle = 0;
		int largestAngleIndex = -1;
		List<GridObjects> neighbors = new ArrayList<GridObjects>();
		for (GridObject cell : nearby_cells) {
			neighbors.add(new GridObjects(cell, getNearbyPostion(player_cell.getPosition(), cell.getPosition())));
		}
		for (GridObject pherome : nearby_pheromes) {
			if (pherome.player != player_cell.player) {
				neighbors.add(
						new GridObjects(pherome, getNearbyPostion(player_cell.getPosition(), pherome.getPosition())));
			}
		}
		neighbors.sort(new CellComparator());
		if (neighbors.size() > 1) {
			
			for (int i = 1; i < neighbors.size(); ++i) {
				double current = Math.atan2(neighbors.get(i).vector.y, neighbors.get(i).vector.x);
				double previous = Math.atan2(neighbors.get(i-1).vector.y, neighbors.get(i-1).vector.x);
				if (current - previous > largestAngle) {
					largestAngle = current - previous;
					largestAngleIndex = i;
				}
			}
			double firstAngle = Math.atan2(neighbors.get(0).vector.y, neighbors.get(0).vector.x);
			double lastAngle = Math.atan2(neighbors.get(neighbors.size() - 1).vector.y,
					neighbors.get(neighbors.size() - 1).vector.x);
			if (largestAngle < firstAngle + 2 * Math.PI - lastAngle) {
				largestAngle = firstAngle + 2 * Math.PI - lastAngle;
				largestAngleIndex = 0;
			}
			int i = largestAngleIndex - 1 < 0 ? neighbors.size() - 1 : largestAngleIndex - 1;
			Point vector = neighbors.get(i).vector;
			double x = vector.x * Math.cos(largestAngle / 2) - vector.y * Math.sin(largestAngle / 2);
			double y = vector.y * Math.cos(largestAngle / 2) + vector.x * Math.sin(largestAngle / 2);
			return new Point(x, y);
		} else if (neighbors.size() == 1) {
			return new Point(-neighbors.get(0).vector.x, -neighbors.get(0).vector.y);
		}
		return new Point(0, 0);
	}

	// sort cells by its angle with play cell
	private Cell[] sortCell(Set<Cell> cells, Cell player_cell) {
		Cell[] orderedCells = new Cell[cells.size()];
		Map<Integer, Cell> cellAngleMap = new HashMap<Integer, Cell>();
		List<Integer> angleSet = new ArrayList<Integer>();
		for (Cell cell : cells) {
			int angle = extractAngleFromVector(cell.getPosition(), player_cell);
			if (!cellAngleMap.containsKey(angle)) {
				cellAngleMap.put(angle, cell);
				angleSet.add(angle);
			}
		}
		Collections.sort(angleSet);
		int i = 0;
		for (int key : angleSet) {
			orderedCells[i++] = cellAngleMap.get(key);
		}

		return orderedCells;
	}

	private Set<Cell> findCellInRange(Set<Cell> cells, Cell player_cell) {
		Set<Cell> cell_List = new HashSet<>();
		for (Cell cell : cells) {
			if (player_cell.distance(cell) <= cell_vision) {
				cell_List.add(cell);
			}
		}
		return cell_List;
	}

	private Point avoidCell(Cell pl_cell, Cell one) {
		int enemy_dir = extractAngleFromVector(one.getPosition(), pl_cell);
		enemy_dir *= 2; // back to 360 degrees for easier mental arithmetic
		int my_cell_dir = (enemy_dir + 180) % 360; // opposite direction of
													// enemy
		my_cell_dir /= 2;
		return this.extractVectorFromAngle(my_cell_dir);
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

	public Point extractVectorFromAngle(double angel) {
		double theta = Math.toRadians(2 * angel);
		double dx = Cell.move_dist * Math.cos(theta);
		double dy = Cell.move_dist * Math.sin(theta);
		return new Point(dx, dy);
	}

	private double getDistance(Point first, Point second) {
		double dist_square = (first.x - second.x) * (first.x - second.x) + (first.y - second.y) * (first.y - second.y);
		double dist = Math.sqrt(dist_square);
		return dist;
	}

	private int extractAngleFromVector(Point arg, Cell player_cell) {
		double x = player_cell.getPosition().x;
		double y = player_cell.getPosition().y;

		if (x == arg.x) { // cell is either directly above or below ours
			if (y > arg.y) { // go up
				return 45; // 90/2
			} else { // otherwise go down
				return 135; // 270/2
			}
		}

		double dx = arg.x - x;
		double dy = arg.y - y;
		double angle = Math.atan(dy / dx);
		if (arg.x < x)
			angle += Math.PI;
		if (angle < 0)
			angle += 2 * Math.PI;
		return (int) (Math.toDegrees(angle) / 2);
	}

	private Point getNearbyPostion(Point one, Point two) {
		double x = two.x;
		double y = two.y;
		double dist = 50;
		Point res = null;
		for (int X = -1; X <= 1; X++) {
			for (int Y = -1; Y <= 1; Y++) {
				x = two.x + X * 50;
				y = two.y + Y * 50;
				double d = getDistance(one, new Point(x, y));
				if (dist > d) {
					dist = d;
					res = new Point(x - one.x, y - one.y);
				}
			}
		}
		double l = Math.hypot(res.x, res.y);
		return new Point(x / l, y / l);
	}
}
