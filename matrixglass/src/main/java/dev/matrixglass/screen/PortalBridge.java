package dev.matrixglass.screen;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import qouteall.imm_ptl.core.portal.Portal;

/**
 * The ONLY class that touches the Immersive Portals API. Written from memory of the v5 API
 * (Portal#setOriginPos / setDestinationDimension / setDestination / setOrientationAndSize);
 * if the build complains, fix it here and nowhere else.
 */
public final class PortalBridge {
    private PortalBridge() {}

    /** The portal entity type field is named differently across versions, so look it up by name. */
    @SuppressWarnings("unchecked")
    private static EntityType<? extends Portal> portalType() {
        for (String n : new String[]{"ENTITY_TYPE", "entityType"}) {
            try { return (EntityType<? extends Portal>) Portal.class.getField(n).get(null); }
            catch (ReflectiveOperationException ignored) {}
        }
        throw new IllegalStateException("Could not find Portal entity type in this Immersive Portals version");
    }

    /** Opens a see-through window. The viewer stands on the side that axisW x axisH points toward. */
    public static void open(ServerLevel level, String tag, Vec3 origin, Vec3 axisW, Vec3 axisH,
                            double width, double height, ResourceKey<Level> destDim, Vec3 dest) {
        Portal p = portalType().create(level);
        if (p == null) throw new IllegalStateException("Portal entity could not be created");
        p.setOriginPos(origin);
        p.setDestinationDimension(destDim);
        p.setDestination(dest);
        p.setOrientationAndSize(axisW, axisH, width, height);
        p.addTag(tag);
        level.addFreshEntity(p);
    }

    /** Removes our portals in an area. tag == null removes every Matrix Glass screen there. Returns how many. */
    public static int close(ServerLevel level, AABB area, String tag) {
        var found = level.getEntitiesOfClass(Portal.class, area,
                p -> p.getTags().stream().anyMatch(t -> tag == null ? t.startsWith("mg_") : t.equals(tag)));
        found.forEach(Portal::discard);
        return found.size();
    }
}
