package s0559587;

import java.util.ArrayList;

import org.lwjgl.util.vector.Vector2f;

public class Path {
	private ArrayList<Vector2f> pathNodes;

	public Path() {
		this.pathNodes = new ArrayList<>();
	}

	public void addNodeToPath(Vector2f pathNode) {
		pathNodes.add(pathNode);
	}

	public ArrayList<Vector2f> getPathNodes() {
		return pathNodes;
	}
}


