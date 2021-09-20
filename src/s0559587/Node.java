package s0559587;

import java.util.ArrayList;

public class Node {

	private float x;
	private float y;
	private boolean bObstacle; // is the node and obstacle?
	private ArrayList<Node> nodeNeighbours = new ArrayList<Node>(); // connections to neighbours
	private Node parent = null; // Node connecting to this node that offers shortest parent

	private float kantenGewicht; // CellSize distance v zu neighbour
	private Float gesamtKosten;

	public Node() {

	}

	public Node(float x, float y) {
		this.x = x;
		this.y = y;
	}

	public boolean checkNodeList(Node nodeToCheck) {
		return false;
	}

	public float getY() {
		return y;
	}

	public void setY(float y) {
		this.y = y;
	}

	public float getX() {
		return x;
	}

	public void setX(float x) {
		this.x = x;
	}

	public void printStuff() {
		System.out.println("X: " + this.x + " " + "Y: " + this.y);
	}

	public boolean isbObstacle() {
		return bObstacle;
	}

	public void setbObstacle(boolean bObstacle) {
		this.bObstacle = bObstacle;
	}

	public ArrayList<Node> getNodeNeighbours() {
		return nodeNeighbours;
	}

	public void addNodeNeighbours(Node node) {
		this.nodeNeighbours.add(node);
	}

	public Node getParent() {
		return parent;
	}

	public void setParent(Node parent) {
		this.parent = parent;
	}

	public void printCoordinates() {
		System.out.println("x: " + this.getX());
		System.out.println("y: " + this.getY());
	}

	public float getGesamtKosten() {
		return gesamtKosten;
	}

	public void setGesamtKosten(float gesamtKosten) {
		this.gesamtKosten = gesamtKosten;
	}

	public float getKantenGewicht() {
		return kantenGewicht;
	}

	public void setKantenGewicht(float kantenGewicht) {
		this.kantenGewicht = kantenGewicht;
	}

	public boolean hasParent() {
		return this.parent != null;
	}

	public boolean equals1(Node nodeToCheck) {
		if (this.x == nodeToCheck.getX() && this.y == nodeToCheck.getY()) {
			return true;
		}
		return false;
	}

}
