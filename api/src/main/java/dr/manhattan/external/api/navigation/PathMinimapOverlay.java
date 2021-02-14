package dr.manhattan.external.api.navigation;

import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

import javax.inject.Inject;
import java.awt.*;

public class PathMinimapOverlay extends Overlay {
    private static final int TILE_WIDTH = 4;
    private static final int TILE_HEIGHT = 4;

    private final Client client;

    @Inject
    private PathMinimapOverlay(Client client) {
        this.client = client;
        setPosition(OverlayPosition.DYNAMIC);
        setPriority(OverlayPriority.LOW);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
    }

    public static void renderMinimapRect(Client client, Graphics2D graphics, Point center, int width, int height, Color color) {
        double angle = client.getMapAngle() * Math.PI / 1024.0d;

        graphics.setColor(color);
        graphics.rotate(angle, center.getX(), center.getY());
        graphics.fillRect(center.getX() - width / 2, center.getY() - height / 2, width, height);
        graphics.rotate(-angle, center.getX(), center.getY());
    }

    @Override
    public Dimension render(Graphics2D graphics) {


        if (Navigation.getPath() != null) {
            for (WorldPoint point : Navigation.getPath()) {
                if (point.getPlane() != client.getPlane()) {
                    continue;
                }

                drawOnMinimap(graphics, point);
            }
        }

        return null;
    }

    private void drawOnMinimap(Graphics2D graphics, WorldPoint point) {
        WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();

        if (point.distanceTo(playerLocation) >= 50) {
            return;
        }

        LocalPoint lp = LocalPoint.fromWorld(client, point);

        if (lp == null) {
            return;
        }

        Point posOnMinimap = Perspective.localToMinimap(client, lp);

        if (posOnMinimap == null) {
            return;
        }

        renderMinimapRect(client, graphics, posOnMinimap, TILE_WIDTH, TILE_HEIGHT, new Color(255, 0, 0, 255));
    }
}
