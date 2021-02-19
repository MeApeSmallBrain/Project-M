package dr.manhattan.external.api.navigation;

import dr.manhattan.external.api.M;
import dr.manhattan.external.api.player.MPlayer;
import dr.manhattan.external.api.util.MMenu;
import dr.manhattan.external.api.util.Util;
import net.runelite.api.Point;
import net.runelite.api.RenderOverview;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.util.ColorUtil;

import java.awt.*;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static dr.manhattan.external.api.util.Util.readAllBytes;

public class Navigation {
    public static CollisionMap map;
    public static Map<WorldPoint, List<WorldPoint>> transports = new HashMap<>();
    private static boolean running = true;
    private static WorldPoint target = null;
    private static boolean pathUpdateScheduled = false;
    private static List<WorldPoint> path = null;
    private static Pathfinder pathfinder;
    private static Object BUS_OBJ = new Object();
    private static Point lastMenuOpenedPoint;
    private static int reachedDistance = 1;
    private static int maxPathDistance = 5;

    public static Map<WorldPoint, List<WorldPoint>> getTransports() {
        return transports;
    }

    private static void decompressMap() {
        Map<SplitFlagMap.Position, byte[]> compressedRegions = new HashMap<>();

        try (ZipInputStream in = new ZipInputStream(M.class.getResourceAsStream("/collision-map"))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                String[] n = entry.getName().split("_");

                compressedRegions.put(
                        new SplitFlagMap.Position(Integer.parseInt(n[0]), Integer.parseInt(n[1])),
                        readAllBytes(in)
                );
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        map = new CollisionMap(64, compressedRegions);
    }

    private static void setTransports() {
        try {
            String s = new String(readAllBytes(M.class.getResourceAsStream("/transports.txt")), StandardCharsets.UTF_8);
            Scanner scanner = new Scanner(s);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();

                if (line.startsWith("#")) {
                    continue;
                }

                String[] l = line.split(" ");
                WorldPoint a = new WorldPoint(Integer.parseInt(l[0]), Integer.parseInt(l[1]), Integer.parseInt(l[2]));
                WorldPoint b = new WorldPoint(Integer.parseInt(l[3]), Integer.parseInt(l[4]), Integer.parseInt(l[5]));
                transports.computeIfAbsent(a, k -> new ArrayList<>()).add(b);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isPathUpdateScheduled() {
        return pathUpdateScheduled;
    }

    public static List<WorldPoint> getPath() {
        return path;
    }

    public static Pathfinder getPathfinder() {
        return pathfinder;
    }

    public static int getReachedDistance() {
        return reachedDistance;
    }

    public static void setReachedDistance(int reachedDistance) {
        Navigation.reachedDistance = reachedDistance;
    }

    public static int getMaxPathDistance() {
        return maxPathDistance;
    }

    public static void setMaxPathDistance(int maxPathDistance) {
        Navigation.maxPathDistance = maxPathDistance;
    }

    private static boolean isNearPath() {
        for (WorldPoint point : path) {
            if (MPlayer.get().getWorldLocation().distanceTo(point) < maxPathDistance) {
                return true;
            }
        }
        return false;
    }

    public void stop(EventBus eventBus) {
        map = null;
        running = false;
        eventBus.unregister(BUS_OBJ);
    }

    public void start(EventBus eventBus) {
        decompressMap();
        setTransports();

        eventBus.subscribe(GameTick.class, BUS_OBJ, this::onGameTick);
        eventBus.subscribe(MenuOpened.class, BUS_OBJ, this::onMenuOpened);
        eventBus.subscribe(MenuEntryAdded.class, BUS_OBJ, this::onMenuEntryAdded);
        eventBus.subscribe(MenuOptionClicked.class, BUS_OBJ, this::onMenuOptionClicked);

        running = true;
        new Thread(() -> {
            while (running) {
                if (pathUpdateScheduled) {
                    if (target == null) {
                        path = null;
                    } else {
                        pathfinder = new Pathfinder(map, transports, MPlayer.get().getWorldLocation(), target, 0);
                        path = pathfinder.find();
                        pathUpdateScheduled = false;
                    }
                }

                Util.sleep(300);
            }
        }, "Pathfinder thread").start();
    }

    public void onMenuOpened(MenuOpened event) {
        lastMenuOpenedPoint = M.client().getMouseCanvasPosition();
    }

    public void onMenuEntryAdded(MenuEntryAdded event) {

        final Widget map = M.client().getWidget(WidgetInfo.WORLD_MAP_VIEW);

        if (map == null) {
            return;
        }

        if (map.getBounds().contains(M.client().getMouseCanvasPosition().getX(), M.client().getMouseCanvasPosition().getY())) {
            MMenu.addEntry(event,
                    ColorUtil.wrapWithColorTag("MWalker", new Color(155, 196, 234, 255))
            );
            MMenu.addEntry(event, ColorUtil.wrapWithColorTag("Auto-Walk ", new Color(73, 250, 52, 255)));
            if (path != null)
                MMenu.addEntry(event, ColorUtil.wrapWithColorTag("Stop walking", new Color(231, 147, 147, 255)));

            String switchTo = Pathfinder.astar ? "Dijkstra" : "AStar";
            MMenu.addEntry(event, "Switch to " + ColorUtil.wrapWithColorTag(switchTo, new Color(155, 196, 234, 255)));

            MMenu.addEntry(event, "Love to " + ColorUtil.wrapWithColorTag("Runemoro", new Color(155, 196, 234, 255))
            );
        }
    }

    public void onMenuOptionClicked(MenuOptionClicked event) {

        if (event.getOption().contains("Auto-Walk")) {
            setTarget(calculateMapPoint(M.client().isMenuOpen() ? lastMenuOpenedPoint : M.client().getMouseCanvasPosition()));
        } else if (event.getOption().contains("Stop walking")) {
            setTarget(null);
        } else if (event.getOption().contains("Switch to")) {
            Pathfinder.astar = !Pathfinder.astar;
        }
    }

    private void setTarget(WorldPoint target) {
        if (target == null) {
            path = null;
            pathfinder.clearBest();
        }
        this.target = target;
        pathUpdateScheduled = true;
    }

    private WorldPoint calculateMapPoint(Point point) {
        float zoom = M.client().getRenderOverview().getWorldMapZoom();
        RenderOverview renderOverview = M.client().getRenderOverview();
        final WorldPoint mapPoint = new WorldPoint(renderOverview.getWorldMapPosition().getX(), renderOverview.getWorldMapPosition().getY(), 0);
        final Point middle = mapWorldPointToGraphicsPoint(mapPoint);

        final int dx = (int) ((point.getX() - middle.getX()) / zoom);
        final int dy = (int) ((-(point.getY() - middle.getY())) / zoom);

        return mapPoint.dx(dx).dy(dy);
    }

    private void onGameTick(GameTick event) {
        if (path != null) {
            if (!isNearPath()) {
                pathUpdateScheduled = true;
            }

            if (MPlayer.get().getWorldLocation().distanceTo(target) < reachedDistance) {
                target = null;
                path = null;
                pathfinder.clearBest();
                pathUpdateScheduled = true;
            }
        }
    }

    private Point mapWorldPointToGraphicsPoint(WorldPoint worldPoint) {
        RenderOverview ro = M.client().getRenderOverview();

        if (!ro.getWorldMapData().surfaceContainsPosition(worldPoint.getX(), worldPoint.getY())) {
            return null;
        }

        Float pixelsPerTile = ro.getWorldMapZoom();

        Widget map = M.client().getWidget(WidgetInfo.WORLD_MAP_VIEW);
        if (map != null) {
            Rectangle worldMapRect = map.getBounds();

            int widthInTiles = (int) Math.ceil(worldMapRect.getWidth() / pixelsPerTile);
            int heightInTiles = (int) Math.ceil(worldMapRect.getHeight() / pixelsPerTile);

            Point worldMapPosition = ro.getWorldMapPosition();

            //Offset in tiles from anchor sides
            int yTileMax = worldMapPosition.getY() - heightInTiles / 2;
            int yTileOffset = (yTileMax - worldPoint.getY() - 1) * -1;
            int xTileOffset = worldPoint.getX() + widthInTiles / 2 - worldMapPosition.getX();

            int xGraphDiff = ((int) (xTileOffset * pixelsPerTile));
            int yGraphDiff = (int) (yTileOffset * pixelsPerTile);

            //Center on tile.
            yGraphDiff -= pixelsPerTile - Math.ceil(pixelsPerTile / 2);
            xGraphDiff += pixelsPerTile - Math.ceil(pixelsPerTile / 2);

            yGraphDiff = worldMapRect.height - yGraphDiff;
            yGraphDiff += (int) worldMapRect.getY();
            xGraphDiff += (int) worldMapRect.getX();

            return new Point(xGraphDiff, yGraphDiff);
        }
        return null;
    }
}
