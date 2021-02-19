package dr.manhattan.external.api.navigation.astar;

import net.runelite.api.Client;
import net.runelite.api.Constants;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

public class ANode {

    public int sceneX, sceneY;
    public ANode parent = null;
    public int score, heuristic, fullScore, collisionData;

    public ANode(int x, int y) {
        this.sceneX = x;
        this.sceneY = y;
        fullScore = 0;
        score = 0;
        heuristic = 0;
    }

    public boolean blocked(Client client, int x, int y) {
        if (sceneX < 0 || sceneY < 0 ||
                sceneX >= Constants.SCENE_SIZE || sceneY >= Constants.SCENE_SIZE)
            return true;
        collisionData = client.getCollisionMaps()[client.getPlane()].getFlags()[sceneX][sceneY];
        //System.out.println(collisionData);
        return isBlockedInDirection(client, x, y);
    }

    public WorldPoint getWorldPoint(Client client) {
        return WorldPoint.fromLocal(client, getLocalPoint());
    }

    public LocalPoint getLocalPoint() {
        return LocalPoint.fromScene(sceneX, sceneY);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof ANode)) {
            return false;
        }
        ANode compare = (ANode) obj;
        return compare.sceneX == sceneX && compare.sceneY == sceneY;
    }

    public boolean isBlockedInDirection(Client client, int x, int y) {
        if (getWorldPoint(client).isInScene(client) && isWalkable()) {
            if (x > 0)
                return blockedEast();
            else if (x < 0)
                return blockedWest();
            else if (y > 0)
                return blockedNorth();
            else if (y < 0)
                return blockedSouth();
        }
        return true;
    }

    private boolean checkFlag(int flag, int checkFlag) {
        return (flag & checkFlag) == checkFlag;
    }

    private boolean blockedNorth() {
        return checkFlag(collisionData, CollisionFlags.NORTH)
                || checkFlag(collisionData, CollisionFlags.BLOCKED_NORTH_WALL);
    }

    private boolean blockedEast() {
        return checkFlag(collisionData, CollisionFlags.EAST)
                || checkFlag(collisionData, CollisionFlags.BLOCKED_EAST_WALL);
    }

    private boolean blockedSouth() {
        return checkFlag(collisionData, CollisionFlags.SOUTH)
                || checkFlag(collisionData, CollisionFlags.BLOCKED_SOUTH_WALL);
    }

    private boolean blockedWest() {
        return checkFlag(collisionData, CollisionFlags.WEST)
                || checkFlag(collisionData, CollisionFlags.BLOCKED_WEST_WALL);
    }

    public boolean isWalkable() {
        return !(checkFlag(collisionData, CollisionFlags.OCCUPIED)
                || checkFlag(collisionData, CollisionFlags.SOLID)
                || checkFlag(collisionData, CollisionFlags.BLOCKED)
                || checkFlag(collisionData, CollisionFlags.CLOSED));
    }
}

