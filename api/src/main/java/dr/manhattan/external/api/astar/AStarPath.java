package dr.manhattan.external.api.astar;

import net.runelite.api.coords.WorldPoint;

import java.util.LinkedList;

public class AStarPath {
    LinkedList<WorldPoint> path;
    int cost;

    public AStarPath(LinkedList<WorldPoint> path, int cost, int distanceToDestination) {
        this.path = path;
        this.cost = cost;
        this.distanceToDestination = distanceToDestination;
    }

    int distanceToDestination;

    public LinkedList<WorldPoint> getPath() {
        return path;
    }

    public int getCost() {
        return cost;
    }

    public int getDistanceToDestination() {
        return distanceToDestination;
    }
}
