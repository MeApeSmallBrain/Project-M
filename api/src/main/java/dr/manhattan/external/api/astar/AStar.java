package dr.manhattan.external.api.astar;

import dr.manhattan.external.api.M;
import dr.manhattan.external.api.objects.MObjectCache;
import dr.manhattan.external.api.objects.MObjectDefinition;
import dr.manhattan.external.api.objects.MObjects;
import dr.manhattan.external.api.player.MPlayer;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.WidgetInfo;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

@Slf4j
@Singleton
public class AStar {

    public static WorldPoint[] lastPath = null;
    private LinkedList<WorldPoint> path;
    private ANode[][] waypoints;
    private LocalPoint lastPosition, destination;
    private LinkedList<WorldPoint> emptyPath = new LinkedList<>();
    private ArrayList<ANode> open_list = new ArrayList<>(), closed_list = new ArrayList<>(), nNodes;
    private ANode closestWorldPoint = null, startway, currentNode, neighbor, node, newNode;

    public void initiateWaypoints() {
        if (lastPosition != null && lastPosition.equals(MPlayer.get().getLocalLocation()))
            return;

        waypoints = new ANode[Constants.SCENE_SIZE][Constants.SCENE_SIZE];
        lastPosition = MPlayer.get().getLocalLocation();
    }

    private void setCurrentNodeToLowestScore(){
        int lowestScoreIndex = 0;
        for (int i = 0; i < open_list.size(); i++) {
            if (open_list.get(i).fullScore < open_list.get(lowestScoreIndex).fullScore)
                lowestScoreIndex = i;
        }
        currentNode = open_list.get(lowestScoreIndex);
    }

    private void setClosestWorldPoint(LocalPoint destinationwp){
        if (closestWorldPoint == null ||
                (currentNode.getLocalPoint().distanceTo(destinationwp) <
                        closestWorldPoint.getLocalPoint().distanceTo(destinationwp))
        ) {
            closestWorldPoint = currentNode;
        }
    }
    private boolean currentIsDestination(LocalPoint destinationwp){
        return currentNode.sceneX == destinationwp.getSceneX() && currentNode.sceneY == destinationwp.getSceneY();
    }

    private boolean isNodeClosed(ANode node){
        LocalPoint playerLoc = MPlayer.get().getLocalLocation();

        if (node.sceneX == playerLoc.getSceneX() && node.sceneY == playerLoc.getSceneY())
            return true;

        AtomicBoolean isClosed = new AtomicBoolean(false);
        if(!isClosed.get())
            closed_list.forEach(clNode -> {
                if(isClosed.get()) return;
                if (node.sceneX == clNode.sceneX && node.sceneY == clNode.sceneY) {
                    //closed list contains
                    isClosed.set(true);
                    return;
                }
            });
        return isClosed.get();
    }

    private AStarPath getNodePath(ANode node){
        LinkedList<WorldPoint> ret = new LinkedList<>();
        ANode startNode = node;
        if (node.parent != null)
            node = node.parent;

        while (node != null) {
            if (node.equals(node.parent))
                break;
            ret.push(node.getWorldPoint(M.client()));
            node = node.parent;
        }
        lastPath = ret.toArray(new WorldPoint[]{});
        return new AStarPath(ret, startNode.fullScore, destination.distanceTo(LocalPoint.fromScene(startNode.sceneX, startNode.sceneY)));
    }

    public AStarPath getPath(LocalPoint startwp, LocalPoint destinationwp) {
        return getPath(startwp, destinationwp, null);
    }
    public AStarPath getPath(LocalPoint destinationwp) {
        return getPath(MPlayer.get().getLocalLocation(), destinationwp, null);
    }
    public AStarPath getPath(Locatable destinationwp) {
        return getPath(MPlayer.get().getLocalLocation(), destinationwp.getLocalLocation(), null);
    }
    public AStarPath getPath(LocalPoint startwp, LocalPoint destinationwp, Predicate<LocalPoint> filter) {
        if (startwp == null || destinationwp == null) {
            lastPath = emptyPath.toArray(new WorldPoint[]{});
            return new AStarPath(emptyPath, Integer.MAX_VALUE, Integer.MAX_VALUE);
        }

        initiateWaypoints();

        if (!getWaypointWithCoord(destinationwp.getSceneX(), destinationwp.getSceneY()).isWalkable())
            return new AStarPath(emptyPath, Integer.MAX_VALUE, Integer.MAX_VALUE);

        open_list.clear();
        closed_list.clear();
        closestWorldPoint = null;
        startway = getWaypointWithCoord(startwp.getSceneX(), startwp.getSceneY());
        open_list.add(startway);
        destination = destinationwp;

        while (open_list.size() > 0) {

            setCurrentNodeToLowestScore();
            setClosestWorldPoint(destinationwp);


            if (currentIsDestination(destinationwp)) {
                //reached destination
                return getNodePath(currentNode);
            }

            //normal case
            open_list.remove(currentNode);
            closed_list.add(currentNode);

            //get valid neighbours;
            Scene scene = M.client().getScene();
            Tile[][][] tiles = scene.getTiles();
            int z = M.client().getPlane();
            if(filter == null) filter = new Predicate<LocalPoint>() {
                @Override
                public boolean test(LocalPoint localPoint) {
                    return true;
                }
            };
            nNodes = getConnectedWaypoints(currentNode, destinationwp, filter.and(localPoint ->{
                Tile tile = tiles[z][localPoint.getSceneX()][localPoint.getSceneY()];
                if (tile == null) {
                    return true;
                }
                GameObject[] gameObjects = tile.getGameObjects();
                for(GameObject go: gameObjects){
                    if(go == null) continue;
                    ObjectDefinition def = MObjectDefinition.getDef(go.getId());
                    if(def == null) return true;
                    for(String action: def.getActions()){
                        if(action == null) continue;
                        if(action.toLowerCase().contains("open")){

                            return false;
                        }
                    }
                }
                return true;
            }));

            nNodes.forEach(nNode -> {

                if(isNodeClosed(nNode)) return;

                int gScore = currentNode.score + 1;
                boolean gScoreIsBest = false;
                boolean isOpen = open_list.contains(nNode);

                if (!isOpen) {
                    gScoreIsBest = true;
                    nNode.heuristic = getHeuristic(nNode, destinationwp);

                } else if (gScore < nNode.score) {
                    gScoreIsBest = true;
                }
                if (gScoreIsBest) {
                    nNode.parent = currentNode;
                    nNode.score = gScore;
                    nNode.fullScore = nNode.score + nNode.heuristic;
                }
                if(!isOpen) open_list.add(nNode);

            });
        }

        if (closestWorldPoint != null) {
            return getNodePath(closestWorldPoint);
        }

        lastPath = emptyPath.toArray(new WorldPoint[]{});
        return new AStarPath(emptyPath, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    int getHeuristic(ANode a, LocalPoint b) {
        int xd, yd;
        xd = b.getSceneX() - a.sceneX;
        yd = b.getSceneY() - a.sceneY;

        // Manhattan distance
        //return Math.abs(xd)+Math.abs(yd);

        // Chebyshev distance
        return Math.max(Math.abs(xd), Math.abs(yd));

        //Gets the euclidian distance
        //return (int) Math.sqrt(xd * xd + yd * yd);
    }

    private ANode getWaypointWithCoord(int x, int y) {
        x = Math.max(0, x);
        y = Math.max(0, y);

        if (waypoints[x][y] != null)
            return waypoints[x][y];

        waypoints[x][y] = new ANode(x, y);
        return waypoints[x][y];
    }

    private ANode n;

    private ArrayList<ANode> getConnectedWaypoints(ANode awp, LocalPoint dest, Predicate<LocalPoint> filter) {
        ArrayList<ANode> neighbours = new ArrayList<>();
        int x = awp.sceneX;
        int y = awp.sceneY;

        n = getWaypointWithCoord(x - 1, y);
        if (!awp.blocked(M.client(), -1, 0) && (filter == null || filter.test(n.getLocalPoint())))
            neighbours.add(n);

        n = getWaypointWithCoord(x + 1, y);
        if (!awp.blocked(M.client(), 1, 0) && (filter == null || filter.test(n.getLocalPoint())))
            neighbours.add(n);

        n = getWaypointWithCoord(x, y - 1);
        if (!awp.blocked(M.client(), 0, -1) && (filter == null || filter.test(n.getLocalPoint())))
            neighbours.add(n);

        n = getWaypointWithCoord(x, y + 1);
        if (!awp.blocked(M.client(), 0, 1) && (filter == null || filter.test(n.getLocalPoint())))
            neighbours.add(n);

        return neighbours;
    }

}
