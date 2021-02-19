package dr.manhattan.external.api.navigation;

import dr.manhattan.external.api.calc.MCalc;
import dr.manhattan.external.api.util.Util;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;

import java.util.*;

@Slf4j
public class Pathfinder {
    private static final WorldArea WILDERNESS_ABOVE_GROUND = new WorldArea(2944, 3523, 448, 448, 0);
    private static final WorldArea WILDERNESS_UNDERGROUND = new WorldArea(2944, 9918, 320, 442, 0);
    static boolean astar = false;
    private static boolean avoidWilderness = true;
    private final CollisionMap map;
    private final Node start;
    private final WorldPoint target;
    private final List<Node> openNodes = new LinkedList<>();
    private final HashMap<WorldPoint, Node> nodeMap = new HashMap<>();
    private final Set<WorldPoint> closedNodePositions = new HashSet<>();
    private final Map<WorldPoint, List<WorldPoint>> transports;
    int sleepMod = 0;
    private Node bestNode;
    private int noChance = 0;

    public Pathfinder(CollisionMap map, Map<WorldPoint, List<WorldPoint>> transports, WorldPoint start, WorldPoint target) {
        this.map = map;
        this.transports = transports;
        this.target = target;
        this.start = getNode(start, null);
        openNodes.add(this.start);
        bestNode = this.start;
    }

    public Pathfinder(CollisionMap map, Map<WorldPoint, List<WorldPoint>> transports, WorldPoint start, WorldPoint target, int noChance) {
        this.map = map;
        this.transports = transports;
        this.target = target;
        this.start = getNode(start, null);
        openNodes.add(this.start);
        this.noChance = noChance;
        bestNode = this.start;
    }

    public static boolean isInWilderness(WorldPoint p) {
        return WILDERNESS_ABOVE_GROUND.distanceTo(p) == 0 ||
                WILDERNESS_UNDERGROUND.distanceTo(p) == 0;
    }

    public static void setAvoidWilderness(boolean avoidWilderness) {
        Pathfinder.avoidWilderness = avoidWilderness;
    }

    public List<Node> getOpenNodes() {
        return openNodes;
    }

    public Set<WorldPoint> getClosedNodePositions() {
        return closedNodePositions;
    }

    public List<WorldPoint> find() {
        if (astar) return findStar();

        openNodes.add(start);

        int bestDistance = distancetToTarget(start);

        while (!openNodes.isEmpty()) {
            Node node = openNodes.remove(0);

            WorldPoint pos = node.position;

            if (pos.equals(target)) {
                return node.path();
            }

            int distance = distancetToTarget(node);
            if (distance < bestDistance) {
                bestNode = node;
                bestDistance = distance;
            }
            getNeighbors(node).forEach(n -> {
                if (!closedNodePositions.contains(n.position)) {
                    openNodes.add(n);
                    closedNodePositions.add(n.position);
                }
            });
            sleepMod++;
            if (sleepMod % 50 == 0)
                Util.sleep(1);
        }

        if (bestNode != null) {
            return bestNode.path();
        }

        return null;
    }

    private Node pollBestFScore() {
        int bestF = Integer.MAX_VALUE;
        Node bestNode = null;
        for (Node n : openNodes) {
            if (n.fScore < bestF) {
                bestF = n.fScore;
                bestNode = n;
            }
        }
        openNodes.remove(bestNode);

        return bestNode;
    }

    public List<WorldPoint> findStar() {
        openNodes.add(start);

        while (!openNodes.isEmpty()) {
            Node node = pollBestFScore();
            log.info("Best fscore: " + node.fScore);
            WorldPoint pos = node.position;
            if (pos.equals(target)) {
                return node.path();
            }
            if (node.fScore < bestNode.fScore) {
                bestNode = node;
            }
            for (Node n : getNeighbors(node)) {
                int tentative_gScore = node.gScore + 1;
                if (tentative_gScore < n.gScore) {
                    n.gScore = tentative_gScore;
                    n.previous = node;
                    n.fScore = n.gScore + distancetToTarget(n);
                    openNodes.add(n);
                } else if (!nodeInOpen(n) && !closedNodePositions.contains(n.position)) {
                    openNodes.add(n);
                }
            }
            closedNodePositions.add(node.position);


            Util.sleep(1);
        }

        if (bestNode != null) {
            return bestNode.path();
        }

        return null;
    }

    private boolean nodeInOpen(Node node) {
        for (Node n : openNodes) {
            if (n.position.equals(node.position)) return true;
        }
        return false;
    }

    public int distancetToTarget(Node n) {
        return Math.abs(target.getX() - n.position.getX()) + Math.abs(target.getY() - n.position.getY());
    }

    private boolean niceNeigbor(WorldPoint pos) {
        if (avoidWilderness && isInWilderness(pos)) return false;
        return MCalc.nextInt(0, 100) > noChance;
    }

    private Collection<Node> getNeighbors(Node node) {
        WorldPoint pos = node.position;
        List<Node> nodes = new ArrayList<>();

        if (map.w(pos.getX(), pos.getY(), pos.getPlane()) && niceNeigbor(pos)) {
            nodes.add(getNode(new WorldPoint(pos.getX() - 1, pos.getY(), pos.getPlane()), node));
        }

        if (map.e(pos.getX(), pos.getY(), pos.getPlane()) && niceNeigbor(pos)) {
            nodes.add(getNode(new WorldPoint(pos.getX() + 1, pos.getY(), pos.getPlane()), node));
        }

        if (map.s(pos.getX(), pos.getY(), pos.getPlane()) && niceNeigbor(pos)) {
            nodes.add(getNode(new WorldPoint(pos.getX(), pos.getY() - 1, pos.getPlane()), node));
        }

        if (map.n(pos.getX(), pos.getY(), pos.getPlane()) && niceNeigbor(pos)) {
            nodes.add(getNode(new WorldPoint(pos.getX(), pos.getY() + 1, pos.getPlane()), node));
        }

        if (map.sw(pos.getX(), pos.getY(), pos.getPlane()) && niceNeigbor(pos)) {
            nodes.add(getNode(new WorldPoint(pos.getX() - 1, pos.getY() - 1, pos.getPlane()), node));
        }

        if (map.se(pos.getX(), pos.getY(), pos.getPlane()) && niceNeigbor(pos)) {
            nodes.add(getNode(new WorldPoint(pos.getX() + 1, pos.getY() - 1, pos.getPlane()), node));
        }

        if (map.nw(pos.getX(), pos.getY(), pos.getPlane()) && niceNeigbor(pos)) {
            nodes.add(getNode(new WorldPoint(pos.getX() - 1, pos.getY() + 1, pos.getPlane()), node));
        }

        if (map.ne(pos.getX(), pos.getY(), pos.getPlane()) && niceNeigbor(pos)) {
            nodes.add(getNode(new WorldPoint(pos.getX() + 1, pos.getY() + 1, pos.getPlane()), node));
        }

        return nodes;
    }

    private Node getNode(WorldPoint wp, Node previousNode) {
        Node n = nodeMap.get(wp);
        if (n == null) {
            n = new Node(wp, previousNode);
            n.gScore = previousNode == null ? 0 : previousNode.gScore + 1;
            n.fScore = n.gScore + distancetToTarget(n);
        }
        return n;
    }


    public List<WorldPoint> currentBest() {
        return bestNode == null ? null : bestNode.path();
    }

    public void clearBest() {
        bestNode = null;
    }

    public static class Node {
        static int fakeScore = 0;
        final WorldPoint position;
        Node previous;
        int gScore = Integer.MAX_VALUE;
        int fScore = Integer.MAX_VALUE;

        public Node(WorldPoint position, Node previous) {
            this.position = position;
            this.previous = previous;
        }

        public List<WorldPoint> path() {
            List<WorldPoint> path = new LinkedList<>();
            Node node = this;

            while (node != null) {
                path.add(0, node.position);
                node = node.previous;
            }

            return new ArrayList<>(path);
        }
    }

}
