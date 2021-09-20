package s0559587;

import static org.lwjgl.opengl.GL11.*;

import java.awt.Polygon;
import java.util.ArrayList;
import java.util.Collections;

import org.lwjgl.util.vector.Vector2f;

import lenz.htw.ai4g.ai.AI;
import lenz.htw.ai4g.ai.DriverAction;
import lenz.htw.ai4g.ai.Info;
import lenz.htw.ai4g.track.Track;

public class MeineSuperAI2 extends AI {

	float tolerance = 0.06f;
	float posVelocity;
	float rotVelocity;

	// Align
	float brakeAngle = 1f;
	float myDesiredAngularVelocity;
	float myDesiredTime = 0.1f;
	float targetOrient;
	float neededRotation;

	// Arrive
	int zielRadius = 7;
	float wunschgeschwindigkeit;
	int abbremsradius = 50;
	float distance;
	int wunschzeit = 1;

	// CheckPointGatheredBehavior
	boolean didHeJustStart = true;
	boolean checkPointDone = false;
	int checkPointsGathered = 0;
	int downtime = 0;

	private Vector2f carPos;
	private Vector2f targetPos;

	private boolean standing = false;
	private int standingtimer = 0;
	// Graph G;
	static final int CELL_SIZE = 15; // eine Zelle 10x10 betrachtet man als ein Knoten d.h bei unserer Strecke von
	int cellWidth = info.getTrack().getWidth() / CELL_SIZE;
	int cellHight = info.getTrack().getHeight() / CELL_SIZE;
	Track track = info.getTrack();
	Polygon[] obstacles = track.getObstacles();
//	ArrayList<Node> nodeArray = new ArrayList<Node>(cellWidth * cellHight);
	Node[][] nodeArray = new Node[cellWidth][cellHight];
	float carToTargetDistance = 0;
	// 1000x1000 = 100zutrrewztuztt
	// -> vorteil Kantenlänge immer 10 wenn von mitte zu mitte

	int astarCounter = 0;

	public MeineSuperAI2(Info info) {
		super(info);
		enlistForTournament(559587, 558281);

		// hier irgendwas -> Graph erstellen
		// G = tueWasSinnvollesMit(info.getTrack().getObstacles());
		// weg = G.wegeSuche(von,nach);

		// Variante 1:

		// info.getTrack().width()/CELL_SIZE
		// info.getTrack().height()/CELL_SIZE -> Ergebnis = min. Anzahl im Array

		// frei, also scneidet ein Hinderniss die Zelle?
		// info.getTrack().getObstacles()[0].intersects(x,y, CELL_SIZE, CELL_SIZE)

		// boolean Array füllen mit den Zellen die frei sind -> bei doDebugStuff prüfbar
		// mit GL_Quads
		// boolean freeSpace[x][y] -> for X, for Y -> if(freeSpace[x][y]) ->
		// glColor(1,0,0) else glColor(1,1,0) -> glBegin(GL_QUADS) ->
		// glVertex2f(info.getX(), infogetY()

		prepareNodesInitial(info);
	}

	private void prepareNodesInitial(Info info) {

		int distanceFromBlock = 10;
		for (int x = 0; x < cellWidth; x++) {
			for (int y = 0; y < cellHight; y++) {
				nodeArray[x][y] = new Node();
				nodeArray[x][y].setX(x);
				nodeArray[x][y].setY(y);
			}
		}
		for (int x = 0; x < cellWidth; x++) {
			for (int y = 0; y < cellHight; y++) {
				int xCellWidth = x * CELL_SIZE;
				int yCellHeight = y * CELL_SIZE;

				nodeArray[x][y].setParent(null);
				nodeArray[x][y].setGesamtKosten(Integer.MAX_VALUE);

				int yPlus = y + 1 > cellHight - 1 ? cellHight - 1 : y + 1;
				int yMinus = y - 1 < 0 ? 0 : y - 1;
				int xPlus = x + 1 > cellWidth - 1 ? cellWidth - 1 : x + 1;
				int xMinus = x - 1 < 0 ? 0 : x - 1;

				nodeArray[x][y].addNodeNeighbours(nodeArray[x][yPlus]);
				nodeArray[x][y].addNodeNeighbours(nodeArray[xPlus][y]);
				nodeArray[x][y].addNodeNeighbours(nodeArray[x][yMinus]);
				nodeArray[x][y].addNodeNeighbours(nodeArray[xMinus][y]);
				nodeArray[x][y].addNodeNeighbours(nodeArray[xPlus][yPlus]);
				nodeArray[x][y].addNodeNeighbours(nodeArray[xMinus][yPlus]);
				nodeArray[x][y].addNodeNeighbours(nodeArray[xPlus][yMinus]);
				nodeArray[x][y].addNodeNeighbours(nodeArray[xMinus][yMinus]);
				for (int k = 0; k < obstacles.length; k++) {
					boolean north = info.getTrack().getObstacles()[k].intersects(xCellWidth,
							y * CELL_SIZE + distanceFromBlock, CELL_SIZE, CELL_SIZE);
					boolean east = info.getTrack().getObstacles()[k].intersects(xCellWidth + distanceFromBlock,
							yCellHeight, CELL_SIZE, CELL_SIZE);
					boolean south = info.getTrack().getObstacles()[k].intersects(xCellWidth,
							yCellHeight - distanceFromBlock, CELL_SIZE, CELL_SIZE);
					boolean west = info.getTrack().getObstacles()[k].intersects(xCellWidth - distanceFromBlock,
							yCellHeight, CELL_SIZE, CELL_SIZE);

					boolean northEast = info.getTrack().getObstacles()[k].intersects(xCellWidth + distanceFromBlock,
							yCellHeight + distanceFromBlock, CELL_SIZE, CELL_SIZE);
					boolean northWest = info.getTrack().getObstacles()[k].intersects(xCellWidth - distanceFromBlock,
							yCellHeight + distanceFromBlock, CELL_SIZE, CELL_SIZE);
					boolean southEast = info.getTrack().getObstacles()[k].intersects(xCellWidth + distanceFromBlock,
							yCellHeight - distanceFromBlock, CELL_SIZE, CELL_SIZE);
					boolean southWest = info.getTrack().getObstacles()[k].intersects(xCellWidth - distanceFromBlock,
							yCellHeight - distanceFromBlock, CELL_SIZE, CELL_SIZE);

					if (north || east || west || south || northEast || northWest || southWest || southEast) {
						nodeArray[x][y].setbObstacle(true);
					}
				}
			}
		}
	}

	@Override
	public String getName() {
		return "SANIC";
	}

	// Weg weg;
	// int lastCheckpointX, lastCheckpointY;

	@Override
	public DriverAction update(boolean wasResetAfterCollision) {

		if (wasResetAfterCollision) {
			astar();
			followPath();
		}
		if (didHeJustStart) {
			if (firstAstar) {
				astar();
				firstAstar = false;
				didHeJustStart = false;
				info.getCurrentCheckpoint().getLocation();
			}

		}

		if (finishedPath) {
			astar();
			finishedPath = false;
		}
//		if (astarCounter == 300) {
//			System.out.println("astar");
//			path.getPathNodes().clear();
//			astar();
//			astarCounter = 0;
//			currentPathNode = 0;
//		}

		followPath();

		return new DriverAction(posVelocity, rotVelocity);

	}

	Vector2f lastCheckpoint = new Vector2f(info.getX(), info.getY());
	boolean firstAstar = true;
	boolean recalcAstar = false;

	int counter = 0;
	static final int FRAMES = 30;

	@Override
	public String getTextureResourceName() {
		return "/s0559587/car.png";
	}

	boolean rechnen = false;

	@Override
	public void doDebugStuff() {
		// boolean Array füllen mit den Zellen die frei sind -> bei doDebugStuff prüfbar
		// boolean freeSpace[x][y] -> for X, for Y -> if(freeSpace[x][y]) ->
		// glColor(1,0,0) else glColor(1,1,0) -> glBegin(GL_QUADS) ->
		astarCounter++;
		int x = 0;
		int y = 0;
		for (x = 0; x < cellWidth; x++) {
			for (y = 0; y < cellHight; y++) {

				if (nodeArray[x][y].isbObstacle()) {
					glColor3f(1, 0, 0);
				} else if (!nodeArray[x][y].isbObstacle()) {
					glColor3f(1, 1, 0);
				}

				glBegin(GL_QUADS);
				glVertex2f(x * CELL_SIZE, y * CELL_SIZE);
				glVertex2f(x * CELL_SIZE + CELL_SIZE, y * CELL_SIZE);
				glVertex2f(x * CELL_SIZE, y * CELL_SIZE + CELL_SIZE);
				glVertex2f(x * CELL_SIZE + CELL_SIZE, y * CELL_SIZE + CELL_SIZE);
				glEnd();
			}

		}

		for (Node node : parentList) {
			float lerp = (1) * (1 - node.getGesamtKosten() / 1000);
			glColor3f(0, lerp, 1 - lerp);

			glBegin(GL_QUADS);
			glVertex2f(node.getX() * CELL_SIZE, node.getY() * CELL_SIZE);
			glVertex2f(node.getX() * CELL_SIZE + CELL_SIZE, node.getY() * CELL_SIZE);
			glVertex2f(node.getX() * CELL_SIZE, node.getY() * CELL_SIZE + CELL_SIZE);
			glVertex2f(node.getX() * CELL_SIZE + CELL_SIZE, node.getY() * CELL_SIZE + CELL_SIZE);
			glEnd();
		}

		glColor3f(1, 1, 1);
		glBegin(GL_LINES);
		glVertex2d(info.getX(), info.getY());
		glVertex2d(info.getCurrentCheckpoint().getX(), info.getCurrentCheckpoint().getY());
		glEnd();

		if (counter == FRAMES) {

			counter = 0;
		}

		counter++;
	}

	private void isStanding(boolean standing, int standingTimer) {
		if (info.getVelocity().length() < 1.5f) {
			standingTimer++;
		} else {
			standingTimer = 0;
		}
		if (standingTimer == 120)
			standing = true;
		if (standing)
			posVelocity = info.getMaxVelocity();
	}

	private float calcDistance(Node node1, Node node2) {
		return (float) (Math.sqrt(CELL_SIZE * Math.pow(node1.getX() - node2.getX(), 2)
				+ CELL_SIZE * Math.pow(node1.getY() - node2.getY(), 2)));
	}

	private float calcHeur(Node carMatrixVector, Node targetMatrixVector) {

		return (float) CELL_SIZE * (Math.abs(carMatrixVector.getX() - targetMatrixVector.getX())
				+ Math.abs(carMatrixVector.getY() - targetMatrixVector.getY()));
	}

	ArrayList<Node> visited = new ArrayList<Node>();

	private boolean nodeIsInList(ArrayList<Node> list, Node nodeToCheck) {
		for (Node node : list) {
			if (node.getX() == nodeToCheck.getX() && node.getY() == nodeToCheck.getY()) {
				return true;
			}
		}
		return false;
	}

	ArrayList<Node> parentList = new ArrayList<Node>();

	private void prepareNodesForAstar() {
		for (Node nodes : visited) {
			nodes.setParent(null);
			nodes.setGesamtKosten(Integer.MAX_VALUE);
		}
	}

	private ArrayList<Node> astar() {
		// reset all
		prepareNodesForAstar();
		path.getPathNodes().clear();
		currentPathNode = 0;
		ArrayList<Node> open = new ArrayList<Node>();
		visited = new ArrayList<Node>();
		Node startNode = nodeArray[(int) (info.getX() / CELL_SIZE)][(int) (info.getY() / CELL_SIZE)];
		Node targetNode = nodeArray[(int) (info.getCurrentCheckpoint().getX() / CELL_SIZE)][(int) (info
				.getCurrentCheckpoint().getY() / CELL_SIZE)];

//		startNode.f = (calcDistance(startNode, targetNode));
//		open.add(startNode);

//		while (!open.isEmpty()) {
//			Collections.sort(open, (i, j) -> {
//				if (i.f > j.f) {
//					return 1;
//				} else if (i.f < j.f) {
//					return -1;
//				} else {
//					return 0;
//				}
//			});
//
//			Node currentNode = (open.get(0));
//			open.remove(open.get(0));
//			if (currentNode.equals1(targetNode)) {
//				break;
//			}
//
//			for (Node neighbour : currentNode.getNodeNeighbours()) {
//				currentNode.g = calcDistance(currentNode, neighbour);
//				if (neighbour.isbObstacle() || nodeIsInList(visited, neighbour)) {
//					continue;
//				}
//
//				if (!nodeIsInList(open, neighbour)) {
//					if (!nodeIsInList(open, neighbour)) {
//						neighbour.f = neighbour.h + currentNode.g;
//						open.add(neighbour);
//
//					}
//					if (neighbour.f <= currentNode.f) {
//						neighbour.f = neighbour.h + currentNode.g;
//						neighbour.setParent(currentNode);
//					}
//				}
//			}
//			visited.add(currentNode);
//		}
		startNode.setGesamtKosten(calcDistance(startNode, targetNode));
		startNode.setKantenGewicht(0);
		open.add(startNode);
		while (!open.isEmpty()) {
			Collections.sort(open, (i, j) -> {
				if (i.getGesamtKosten() > j.getGesamtKosten()) {
					return 1;
				} else if (i.getGesamtKosten() < j.getGesamtKosten()) {
					return -1;
				} else {
					return 0;
				}
			});
			Node cur = open.get(0);
			open.remove(cur);
			visited.add(cur);
			if (cur.equals1(targetNode)) {
				break;
			}
			for (Node neighbour : cur.getNodeNeighbours()) {
				if (neighbour.isbObstacle() || nodeIsInList(visited, neighbour)) {
					continue;
				}
				if (!nodeIsInList(visited, neighbour)) {
					if (!nodeIsInList(open, neighbour)) {
						neighbour.setGesamtKosten(Integer.MAX_VALUE);
						open.add(neighbour);
					}
					if (nodeIsInList(open, neighbour)
							&& cur.getGesamtKosten() + calcDistance(cur, neighbour) < neighbour.getGesamtKosten()) {
						neighbour.setGesamtKosten(cur.getGesamtKosten() + calcDistance(cur, neighbour));
						neighbour.setParent(cur);
						if (neighbour.equals1(targetNode)) {
							break;
						}
					}
				}
			}
		}
		Node tar = targetNode;
		Node start = startNode;
		parentList = new ArrayList<Node>();
		while (tar.getParent() != start.getParent()) {
			parentList.add(tar);
			tar = tar.getParent();

		}
		Collections.reverse(parentList);
		parentList.forEach(node -> path.addNodeToPath(
				new Vector2f(node.getX() * CELL_SIZE + CELL_SIZE / 2, node.getY() * CELL_SIZE + CELL_SIZE / 2)));
		path.addNodeToPath(
				new Vector2f((float) info.getCurrentCheckpoint().getX(), (float) info.getCurrentCheckpoint().getY()));
		return visited;

	}

	private void printCarNodeAndTargetNode(Node current, Node targetNode) {
		System.out.println();
		System.out.println();
		System.out.println();
		System.out.println();
		System.out.println();
		System.out.println();
		System.out.println();
		System.out.println();
		System.out.println();
		System.out.println();
		System.out.println();
		System.out.println();
		System.out.println();
		System.out.println();
		System.out.println();
		System.out.println("CurrentNode");
		current.printCoordinates();
		System.out.println("targetNode");
		targetNode.printCoordinates();

		System.out.println("CarPos");
		System.out.println(info.getX());
		System.out.println(info.getY());

		System.out.println("targetPos");
		System.out.println(info.getCurrentCheckpoint().getX());
		System.out.println(info.getCurrentCheckpoint().getY());
	}

	private Path path = new Path();
	private int currentPathNode;
	private ArrayList<Vector2f> pathArray;

	boolean finishedPath = false;

	private void followPath() {
		Vector2f pathTarget = new Vector2f();

		carPos = new Vector2f(info.getX(), info.getY());
		pathArray = path.getPathNodes();
		if (!pathArray.isEmpty()) {
//            pathArray = path.getPathNodes();
			pathTarget = pathArray.get(currentPathNode);
//            pathTarget = new Vector2f(550,600);

			distance = (float) Math.sqrt(Math.pow(carPos.x - pathTarget.x, 2) + Math.pow(carPos.y - pathTarget.y, 2));
			if (distance <= 27) {
				currentPathNode += 1;
				if (currentPathNode >= pathArray.size() - 1) {
					// or break at > pathArray.size
					currentPathNode = pathArray.size() - 1;
					if (distance < 6.4 && currentPathNode == pathArray.size() - 1) {
						finishedPath = true;
					}
				}

			}

		}
		seekPath(carPos, pathTarget);

	}

	private void seekPath(Vector2f carPos, Vector2f pathTarget) {

		// Richtung: Ziel - Start
		Vector2f direction = new Vector2f(pathTarget.x - carPos.x, pathTarget.y - carPos.y);
		direction.normalise(direction);

		// Orientierung zw. Auto und Ziel
		float carOrient = info.getOrientation();

		targetOrient = (float) Math.atan2(direction.y, direction.x);
		neededRotation = targetOrient - carOrient;

		// Align

		float tolerance = 0.06f;
		if (Math.abs(neededRotation) < tolerance) {
			rotVelocity = 0;
		}
		if (neededRotation < brakeAngle) {
			myDesiredAngularVelocity = (neededRotation) * info.getMaxAbsoluteAngularVelocity();
		} else {
			if (neededRotation < Math.PI) {
				myDesiredAngularVelocity = info.getMaxAbsoluteAngularVelocity();
			} else {
				myDesiredAngularVelocity = -info.getMaxAbsoluteAngularVelocity();
			}
		}

		if (neededRotation < -4.4) {
			myDesiredAngularVelocity = info.getMaxAbsoluteAngularVelocity();
		}

		rotVelocity = (myDesiredAngularVelocity - info.getAngularVelocity()) / myDesiredTime;
		if (rotVelocity > 1.5f) {
			rotVelocity = 1.5f;
		}
		posVelocity = 1.15f;
		if (currentPathNode >= pathArray.size() - 4) {
			posVelocity = 0.6f;
		}
		if (Math.abs(neededRotation) > 0.26f) {
			posVelocity = -0.8f;
		} else if (Math.abs(neededRotation) > 3f) {
			posVelocity = -1f;
		}

	}
}
