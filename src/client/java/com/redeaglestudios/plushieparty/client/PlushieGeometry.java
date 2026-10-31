package com.redeaglestudios.plushieparty.client;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.core.Direction;

import org.joml.Vector3fc;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Bedrock -> Java geometry converter for geometry.re_plushie_party.creeper_plushie.
 *
 * Bedrock entity geometry: Y up, ground at y=0, face on -Z.
 * Java entity models:      Y down, ground at y=24, face on -Z.
 *
 * Java ABSOLUTE model coords (ground=24):
 *   cube min corner: x = -(ox + w)   y = 24 - oy - h   z = oz
 *   bone pivot:      x = -px         y = 24 - py       z = pz
 * Cubes are then stored relative to their part pivot. Rotations in radians.
 *
 * Per-face UVs (Bedrock "uv" object form) are applied by overwriting the Polygon
 * vertices of vanilla box-UV cubes. Face U/V axis conventions come from the
 * decompiled ModelPart.Cube constructor:
 *   NORTH: U:+X V:+Y   SOUTH: U:-X V:+Y   EAST: U:+Z V:+Y   WEST: U:-Z V:+Y
 *   UP:    U:+X V:-Z   DOWN:  U:+X V:-Z
 * Negative Bedrock uv_size values (mirrored leg UVs) work naturally: U1 < U0.
 */
public final class PlushieGeometry {
    private PlushieGeometry() {}

    private static final float TEX_SIZE = 32.0f;
    private static final float EPS = 1.0e-4f;
    private static final Set<Direction> ALL_FACES = EnumSet.allOf(Direction.class);

    /** One converted cube: java-ABSOLUTE bounds + per-face UV rects in texels. */
    private record GeoCube(float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
                           float grow, Map<Direction, float[]> faces) {}

    private record LegDef(float px, float py, float pz, float rx, float ry, float rz, GeoCube cube) {}

    // ------------------------------------------------------------- source data
    // From creeper_plushie.geo.json (geometry.re_plushie_party.creeper_plushie, 32x32).
    // Root bone pivot [0,0,0]; body bone pivot [0,0,0]; all cubes on the body bone.

    private static GeoCube cube(float ox, float oy, float oz, float w, float h, float d, float grow,
                                Map<Direction, float[]> faces) {
        return new GeoCube(-(ox + w), 24f - oy - h, oz,
                           -(ox + w) + w, 24f - oy, oz + d, grow, faces);
    }

    private static Map<Direction, float[]> uv(float[] north, float[] south, float[] east,
                                              float[] west, float[] up, float[] down) {
        Map<Direction, float[]> m = new HashMap<>();
        if (north != null) m.put(Direction.NORTH, north);
        if (south != null) m.put(Direction.SOUTH, south);
        if (east != null) m.put(Direction.EAST, east);
        if (west != null) m.put(Direction.WEST, west);
        if (up != null) m.put(Direction.UP, up);
        if (down != null) m.put(Direction.DOWN, down);
        return m;
    }

    /** {u, v, uSize, vSize} from the Bedrock per-face UV entry. */
    private static float[] f(float u, float v, float us, float vs) {
        return new float[] {u, v, us, vs};
    }

    private static final List<GeoCube> BODY_CUBES = List.of(
        // CUBE0: main body 6x6x6 at y=4
        cube(-3, 4, -3, 6, 6, 6, 0,
            uv(f(0, 0, 6, 6), f(6, 0, 6, 6), f(0, 6, 6, 6), f(6, 6, 6, 6), f(0, 12, 6, 6), f(12, 6, 6, -6))),
        // CUBE1: neck cushion 4x2x4 at y=2
        cube(-2, 2, -2, 4, 2, 4, 0,
            uv(f(11, 16, 4, 2), f(0, 18, 4, 2), f(15, 17, 4, 2), f(18, 0, 4, 2), f(11, 12, 4, 4), f(15, 15, 4, -4))),
        // CUBE2: base cushion 5x2x5 at y=0, inflate 0.01
        cube(-2.5f, 0, -2.5f, 5, 2, 5, 0.01f,
            uv(f(15, 15, 5, 2), f(17, 6, 5, 2), f(6, 17, 5, 2), f(17, 8, 5, 2), f(6, 12, 5, 5), f(12, 11, 5, -5))),
        // CUBE7-13: face decal quads (0 thickness, north face only)
        cube(0.5f, 6.2f, -3.01f, 2, 2, 0, 0, uv(f(0, 20, 2, 2), null, null, null, null, null)),
        cube(0.8f, 7.1f, -3.02f, 0.8f, 0.8f, 0, 0, uv(f(0, 22, 2, 2), null, null, null, null, null)),
        cube(-2.2f, 7.1f, -3.02f, 0.8f, 0.8f, 0, 0, uv(f(0, 22, 2, 2), null, null, null, null, null)),
        cube(-2.5f, 6.2f, -3.01f, 2, 2, 0, 0, uv(f(0, 20, 2, 2), null, null, null, null, null)),
        cube(-0.5f, 5.2f, -3.01f, 1, 1, 0, 0, uv(f(0, 20, 2, 2), null, null, null, null, null)),
        cube(-1.5f, 4.7f, -3.01f, 1, 1, 0, 0, uv(f(0, 20, 2, 2), null, null, null, null, null)),
        cube(0.5f, 4.7f, -3.01f, 1, 1, 0, 0, uv(f(0, 20, 2, 2), null, null, null, null, null))
    );

    // CUBE3-6: the four splayed legs (Bedrock cube pivot + rotation -> child parts).
    private static final LegDef[] LEGS = {
        new LegDef(1.93511f, 1.2f, -3.28585f, -70, -15, 0,
            cube(0.93511f, -0.3f, -4.28585f, 2, 3, 2, 0,
                uv(f(18, 2, 2, 3), f(11, 18, 2, 3), f(4, 18, 2, 3), f(13, 18, 2, 3), f(6, 19, 2, 2), f(8, 21, 2, -2)))),
        new LegDef(-1.93511f, 1.2f, -3.28585f, -70, 15, 0,
            cube(-2.93511f, -0.3f, -4.28585f, 2, 3, 2, 0,
                uv(f(20, 2, -2, 3), f(13, 18, -2, 3), f(15, 18, -2, 3), f(6, 18, -2, 3), f(8, 19, -2, 2), f(10, 21, -2, -2)))),
        new LegDef(-1.93511f, 1.2f, 3.28585f, 70, -15, 0,
            cube(-2.93511f, -0.3f, 2.28585f, 2, 3, 2, 0,
                uv(f(11, 18, 2, 3), f(18, 2, 2, 3), f(13, 18, 2, 3), f(4, 18, 2, 3), f(8, 21, -2, -2), f(10, 19, -2, 2)))),
        new LegDef(1.93511f, 1.2f, 3.28585f, 70, 15, 0,
            cube(0.93511f, -0.3f, 2.28585f, 2, 3, 2, 0,
                uv(f(13, 18, -2, 3), f(20, 2, -2, 3), f(6, 18, -2, 3), f(15, 18, -2, 3), f(6, 21, 2, -2), f(8, 19, 2, 2))))
    };

    // ------------------------------------------------------------------ build
    /**
     * Builds the plushie model root: root -> body -> {leg0..leg3}.
     * Body pivot sits at the ground line (model y=24) like the Bedrock bone.
     */
    public static ModelPart bakeRoot() {
        Map<String, ModelPart> legs = new HashMap<>();
        int i = 0;
        for (LegDef leg : LEGS) {
            // Leg pivot, absolute java coords, then relative to the body pivot (0,24,0).
            float absPx = -leg.px();
            float absPy = 24f - leg.py();
            float absPz = leg.pz();
            ModelPart part = new ModelPart(
                List.of(buildCube(leg.cube(), absPx, absPy, absPz)),
                Map.of());
            part.loadPose(PartPose.offsetAndRotation(
                absPx - 0f, absPy - 24f, absPz - 0f,
                (float) Math.toRadians(leg.rx()),
                (float) Math.toRadians(leg.ry()),
                (float) Math.toRadians(leg.rz())));
            legs.put("leg" + (i++), part);
        }

        // Body: pivot at the ground line (24), cubes relative to it; legs as children
        // so the squash scale on the body also scales the legs (like the Bedrock bone).
        ModelPart body = new ModelPart(
            buildCubes(BODY_CUBES, 0f, 24f, 0f),
            legs);
        body.loadPose(PartPose.offsetAndRotation(0f, 24f, 0f, 0f, 0f, 0f));

        return new ModelPart(List.of(), Map.of("body", body));
    }

    private static List<ModelPart.Cube> buildCubes(List<GeoCube> cubes, float px, float py, float pz) {
        return cubes.stream().map(c -> buildCube(c, px, py, pz)).toList();
    }

    /** Builds a cube with its origin translated into the part's pivot space. */
    private static ModelPart.Cube buildCube(GeoCube c, float px, float py, float pz) {
        float minX = c.minX() - px, minY = c.minY() - py, minZ = c.minZ() - pz;
        float maxX = c.maxX() - px, maxY = c.maxY() - py, maxZ = c.maxZ() - pz;
        ModelPart.Cube cube = new ModelPart.Cube(
            0, 0,
            minX, minY, minZ,
            maxX - minX, maxY - minY, maxZ - minZ,
            c.grow(), c.grow(), c.grow(),
            false, TEX_SIZE, TEX_SIZE, ALL_FACES);

        for (int i = 0; i < cube.polygons.length; i++) {
            ModelPart.Polygon poly = cube.polygons[i];
            Direction face = directionOf(poly.normal());
            float[] rect = c.faces().get(face);
            if (rect == null) {
                continue;   // face not painted in Bedrock (degenerate or hidden)
            }
            float u0 = rect[0], v0 = rect[1], u1 = rect[0] + rect[2], v1 = rect[1] + rect[3];
            AxisMapping map = AXIS_MAPPINGS.get(face);
            ModelPart.Vertex[] vs = poly.vertices();
            for (int j = 0; j < vs.length; j++) {
                ModelPart.Vertex v = vs[j];
                boolean isRight = map.uSign() > 0
                    ? coord(v, map.uAxis()) >= axisMax(minX, minY, minZ, maxX, maxY, maxZ, c.grow(), map.uAxis()) - EPS
                    : coord(v, map.uAxis()) <= axisMin(minX, minY, minZ, maxX, maxY, maxZ, c.grow(), map.uAxis()) + EPS;
                boolean isTop = map.vSign() > 0
                    ? coord(v, map.vAxis()) <= axisMin(minX, minY, minZ, maxX, maxY, maxZ, c.grow(), map.vAxis()) + EPS
                    : coord(v, map.vAxis()) >= axisMax(minX, minY, minZ, maxX, maxY, maxZ, c.grow(), map.vAxis()) - EPS;
                float u = (isRight ? u1 : u0) / TEX_SIZE;
                float vv = (isTop ? v0 : v1) / TEX_SIZE;
                vs[j] = v.remap(u, vv);
            }
        }
        return cube;
    }

    /** UV axis conventions per face, derived from the decompiled ModelPart.Cube. */
    private record AxisMapping(int uAxis, int uSign, int vAxis, int vSign) {}

    private static final Map<Direction, AxisMapping> AXIS_MAPPINGS = Map.of(
        Direction.NORTH, new AxisMapping(0, +1, 1, +1),
        Direction.SOUTH, new AxisMapping(0, -1, 1, +1),
        Direction.EAST,  new AxisMapping(2, +1, 1, +1),
        Direction.WEST,  new AxisMapping(2, -1, 1, +1),
        Direction.UP,    new AxisMapping(0, +1, 2, -1),
        Direction.DOWN,  new AxisMapping(0, +1, 2, -1)
    );

    private static float coord(ModelPart.Vertex v, int axis) {
        return switch (axis) {
            case 0 -> v.x();
            case 1 -> v.y();
            default -> v.z();
        };
    }

    private static float axisMin(float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
                                 float grow, int axis) {
        return switch (axis) {
            case 0 -> minX - grow;
            case 1 -> minY - grow;
            default -> minZ - grow;
        };
    }

    private static float axisMax(float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
                                 float grow, int axis) {
        return switch (axis) {
            case 0 -> maxX + grow;
            case 1 -> maxY + grow;
            default -> maxZ + grow;
        };
    }

    private static Direction directionOf(Vector3fc normal) {
        if (normal.x() < -0.5f) return Direction.WEST;
        if (normal.x() > 0.5f) return Direction.EAST;
        if (normal.y() < -0.5f) return Direction.DOWN;
        if (normal.y() > 0.5f) return Direction.UP;
        if (normal.z() < -0.5f) return Direction.NORTH;
        return Direction.SOUTH;
    }
}
