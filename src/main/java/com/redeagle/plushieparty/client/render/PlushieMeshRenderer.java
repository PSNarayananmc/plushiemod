package com.redeagle.plushieparty.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.redeagle.plushieparty.client.model.PlushieGeometry;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Emits the plushie doll mesh (all 14 cubes of the original geometry) into a
 * {@link VertexConsumer}.
 *
 * <p>The data lives in "Java model space" (y down, ground plane at
 * {@link PlushieGeometry#GROUND_Y}), identical to how vanilla
 * {@code ModelPart} meshes are authored; callers wrap it with the standard
 * {@code scale(-1,-1,1)} + {@code translate(0, -1.501, 0)} entity pose chain,
 * or with an equivalent flip for item display.</p>
 *
 * <p>Face corner-to-UV mapping follows the "viewed from outside" convention
 * of Blockbench per-face UVs, so negative UV sizes render as mirrored faces,
 * exactly like the original Bedrock model.</p>
 */
public final class PlushieMeshRenderer {

        /**
         * Emits the doll.
         *
         * @param outerPose  pose already positioned at the doll's feet origin
         * @param bodyScaleX/Y/Z scale of the body bone (the click "squish"), 1 = rest
         */
        public static void emit(PoseStack.Pose outerPose, VertexConsumer buffer, int light, int overlay,
                        float bodyScaleX, float bodyScaleY, float bodyScaleZ) {
                PoseStack stack = new PoseStack();
                stack.last().set(outerPose);

                // "body" bone: pivot (0, 24, 0) in Java model units = ground level.
                stack.pushPose();
                stack.translate(0.0F, PlushieGeometry.GROUND_Y / 16.0F, 0.0F);
                if (bodyScaleX != 1.0F || bodyScaleY != 1.0F || bodyScaleZ != 1.0F) {
                        stack.scale(bodyScaleX, bodyScaleY, bodyScaleZ);
                }
                PoseStack.Pose bodyPose = stack.last();

                for (PlushieGeometry.Cube cube : PlushieGeometry.BODY_CUBES) {
                        // Body cubes are stored in absolute model coords; shift into body-local space.
                        emitCube(bodyPose, buffer, light, overlay, cube, 0.0F, -PlushieGeometry.GROUND_Y, 0.0F);
                }

                for (PlushieGeometry.Part leg : PlushieGeometry.LEG_PARTS) {
                        stack.pushPose();
                        stack.translate(leg.px() / 16.0F, (leg.py() - PlushieGeometry.GROUND_Y) / 16.0F, leg.pz() / 16.0F);
                        stack.mulPose(new Quaternionf().rotationZYX(
                                        (float) Math.toRadians(leg.rz()),
                                        (float) Math.toRadians(leg.ry()),
                                        (float) Math.toRadians(leg.rx())));
                        PoseStack.Pose legPose = stack.last();
                        for (PlushieGeometry.Cube cube : leg.cubes()) {
                                // Leg cubes are already stored relative to their part pivot.
                                emitCube(legPose, buffer, light, overlay, cube, 0.0F, 0.0F, 0.0F);
                        }
                        stack.popPose();
                }

                stack.popPose();
        }

        private static void emitCube(PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay,
                        PlushieGeometry.Cube cube, float offsetX, float offsetY, float offsetZ) {
                float x0 = (cube.minX() + offsetX) / 16.0F;
                float y0 = (cube.minY() + offsetY) / 16.0F;
                float z0 = (cube.minZ() + offsetZ) / 16.0F;
                float x1 = (cube.maxX() + offsetX) / 16.0F;
                float y1 = (cube.maxY() + offsetY) / 16.0F;
                float z1 = (cube.maxZ() + offsetZ) / 16.0F;
                boolean zeroDepth = cube.minZ() == cube.maxZ();

                // Corner order per face: TL, TR, BR, BL as seen from outside the face.
                // Java model space is y-down, so the visual "top" of a side face is minY.
                if (cube.north() != null) {
                        quad(pose, buffer, light, overlay, cube.north(), 0.0F, 0.0F, -1.0F,
                                        x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0);
                }
                if (zeroDepth) {
                        // The face decals are single quads (Bedrock zero-thickness cubes
                        // only define their north face; the cutout render type shows the
                        // texture on both sides).
                        return;
                }
                if (cube.south() != null) {
                        quad(pose, buffer, light, overlay, cube.south(), 0.0F, 0.0F, 1.0F,
                                        x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
                }
                if (cube.west() != null) {
                        quad(pose, buffer, light, overlay, cube.west(), -1.0F, 0.0F, 0.0F,
                                        x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
                }
                if (cube.east() != null) {
                        quad(pose, buffer, light, overlay, cube.east(), 1.0F, 0.0F, 0.0F,
                                        x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1);
                }
                if (cube.up() != null) {
                        quad(pose, buffer, light, overlay, cube.up(), 0.0F, 1.0F, 0.0F,
                                        x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1);
                }
                if (cube.down() != null) {
                        quad(pose, buffer, light, overlay, cube.down(), 0.0F, -1.0F, 0.0F,
                                        x1, y0, z0, x0, y0, z0, x0, y0, z1, x1, y0, z1);
                }
        }

        /** TL, TR, BR, BL corners map to (u0,v0), (u1,v0), (u1,v1), (u0,v1). */
        private static void quad(PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay,
                        PlushieGeometry.FaceUV uv, float nx, float ny, float nz,
                        float ax, float ay, float az, float bx, float by, float bz,
                        float cx, float cy, float cz, float dx, float dy, float dz) {
                vertex(pose, buffer, light, overlay, ax, ay, az, uv.u0(), uv.v0(), nx, ny, nz);
                vertex(pose, buffer, light, overlay, bx, by, bz, uv.u1(), uv.v0(), nx, ny, nz);
                vertex(pose, buffer, light, overlay, cx, cy, cz, uv.u1(), uv.v1(), nx, ny, nz);
                vertex(pose, buffer, light, overlay, dx, dy, dz, uv.u0(), uv.v1(), nx, ny, nz);
        }

        private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay,
                        float x, float y, float z, float u, float v, float nx, float ny, float nz) {
                Vector3f position = pose.pose().transformPosition(x, y, z, new Vector3f());
                Vector3f normal = pose.transformNormal(nx, ny, nz, new Vector3f());
                buffer.addVertex(position.x(), position.y(), position.z())
                                .setColor(0xFFFFFFFF)
                                .setUv(u, v)
                                .setOverlay(overlay)
                                .setLight(light)
                                .setNormal(normal.x(), normal.y(), normal.z());
        }

        private PlushieMeshRenderer() {
        }
}
