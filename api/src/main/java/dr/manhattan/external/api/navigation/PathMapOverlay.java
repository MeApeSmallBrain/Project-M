package dr.manhattan.external.api.navigation;

import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.worldmap.WorldMapOverlay;

import javax.inject.Inject;
import java.awt.*;
import java.awt.geom.Area;
import java.util.List;

public class PathMapOverlay extends Overlay {
    private final Client client;

    @Inject
    private WorldMapOverlay worldMapOverlay;
    private Area mapClipArea;

    @Inject
    private PathMapOverlay(Client client) {
        this.client = client;
        setPosition(OverlayPosition.DYNAMIC);
        setPriority(OverlayPriority.LOW);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
    }

    @Override
    public Dimension render(Graphics2D graphics) {

        if (client.getWidget(WidgetInfo.WORLD_MAP_VIEW) == null) {
            return null;
        }
        mapClipArea = getWorldMapClipArea(client.getWidget(WidgetInfo.WORLD_MAP_VIEW).getBounds());

        if (Navigation.getPathfinder() != null) {

            for (Pathfinder.Node n : Navigation.getPathfinder().getOpenNodes()) {
                drawOnMap(graphics, n.position, new Color(255, 255, 0, 255));
            }
        }

        if (Navigation.getPath() != null && !Navigation.isPathUpdateScheduled()) {
            for (WorldPoint point : Navigation.getPath()) {
                drawOnMap(graphics, point, new Color(0, 255, 0, 255));
            }
        } else if (Navigation.isPathUpdateScheduled() && Navigation.getPathfinder() != null) {
            List<WorldPoint> bestPath = Navigation.getPathfinder().currentBest();

            if (bestPath != null) {
                for (WorldPoint point : bestPath) {
                    drawOnMap(graphics, point, new Color(255, 0, 0, 255));
                }
            }
        }

        return null;
    }

    private void drawOnMap(Graphics2D graphics, WorldPoint point, Color color) {
        Point start = worldMapOverlay.mapWorldPointToGraphicsPoint(point);
        Point end = worldMapOverlay.mapWorldPointToGraphicsPoint(point.dx(1).dy(-1));

        if (start == null || end == null) {
            return;
        }

        if (!mapClipArea.contains(start.getX(), start.getY()) || !mapClipArea.contains(end.getX(), end.getY())) {
            return;
        }

        graphics.setColor(color);
        graphics.fillRect(start.getX(), start.getY(), end.getX() - start.getX(), end.getY() - start.getY());
    }

    private Area getWorldMapClipArea(Rectangle baseRectangle) {
        final Widget overview = client.getWidget(WidgetInfo.WORLD_MAP_OVERVIEW_MAP);
        final Widget surfaceSelector = client.getWidget(WidgetInfo.WORLD_MAP_SURFACE_SELECTOR);

        Area clipArea = new Area(baseRectangle);

        if (overview != null && !overview.isHidden()) {
            clipArea.subtract(new Area(overview.getBounds()));
        }

        if (surfaceSelector != null && !surfaceSelector.isHidden()) {
            clipArea.subtract(new Area(surfaceSelector.getBounds()));
        }

        return clipArea;
    }
}
