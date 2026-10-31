import net.minecraft.client.model.geom.ModelPart;
import com.redeaglestudios.plushieparty.client.PlushieGeometry;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/**
 * Dumps the converted plushie model cubes (positions + per-face UVs) as simple
 * pipe-separated lines for offline visual verification, without the game.
 * Line format:
 * bone|name|cubeIdx|minX,minY,minZ|maxX,maxY,maxZ|faceNormal|u1,v1,u2,v2;u1,v1,...
 */
public class DumpModel {
    public static void main(String[] args) throws Exception {
        ModelPart root = PlushieGeometry.bakeRoot();
        dumpPart(root, "root", 0, 0, 0);
        System.out.println("DUMP_DONE");
    }

    private static void dumpPart(ModelPart part, String name, float px, float py, float pz) throws Exception {
        List<?> cubes = (List<?>) field(part, "cubes");
        Map<String, ModelPart> children = (Map<String, ModelPart>) field(part, "children");
        float ox = part.x, oy = part.y, oz = part.z;
        for (int i = 0; i < cubes.size(); i++) {
            Object cube = cubes.get(i);
            StringBuilder sb = new StringBuilder();
            sb.append(name).append('|').append(i).append('|');
            sb.append(getF(cube, "minX") + px + ox).append(',').append(getF(cube, "minY") + py + oy).append(',')
              .append(getF(cube, "minZ") + pz + oz).append('|');
            sb.append(getF(cube, "maxX") + px + ox).append(',').append(getF(cube, "maxY") + py + oy).append(',')
              .append(getF(cube, "maxZ") + pz + oz).append('|');
            Object[] polygons = (Object[]) field(cube, "polygons");
            for (int j = 0; j < polygons.length; j++) {
                Object poly = polygons[j];
                Object[] verts = record(poly, "vertices", Object[].class);
                Object normal = record(poly, "normal", Object.class);
                if (j > 0) sb.append(';');
                sb.append(nrm(normal)).append(':');
                for (int k = 0; k < verts.length; k++) {
                    Object v = verts[k];
                    if (k > 0) sb.append(',');
                    sb.append(fmt((float) v.getClass().getMethod("x").invoke(v) + px + ox)).append(',')
                      .append(fmt((float) v.getClass().getMethod("y").invoke(v) + py + oy)).append(',')
                      .append(fmt((float) v.getClass().getMethod("z").invoke(v) + pz + oz)).append(',')
                      .append(fmt((float) v.getClass().getMethod("u").invoke(v))).append(',')
                      .append(fmt((float) v.getClass().getMethod("v").invoke(v)));
                }
            }
            System.out.println(sb);
        }
        for (var e : children.entrySet()) {
            dumpPart(e.getValue(), e.getKey(), px + ox, py + oy, pz + oz);
        }
    }

    private static String nrm(Object n) throws Exception {
        return fmt((float) n.getClass().getMethod("x").invoke(n)) + ","
            + fmt((float) n.getClass().getMethod("y").invoke(n)) + ","
            + fmt((float) n.getClass().getMethod("z").invoke(n));
    }

    private static String fmt(float f) {
        return String.format(java.util.Locale.ROOT, "%.4f", f);
    }

    private static Object field(Object obj, String name) throws Exception {
        Field f = obj.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(obj);
    }

    private static float getF(Object obj, String name) throws Exception {
        Field f = obj.getClass().getField(name);
        return f.getFloat(obj);
    }

    @SuppressWarnings("unchecked")
    private static <T> T record(Object rec, String component, Class<T> type) throws Exception {
        for (var rc : rec.getClass().getRecordComponents()) {
            if (rc.getName().equals(component)) {
                return (T) rc.getAccessor().invoke(rec);
            }
        }
        throw new NoSuchFieldException(component);
    }
}
