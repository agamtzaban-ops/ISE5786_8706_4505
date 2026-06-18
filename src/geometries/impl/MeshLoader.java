package geometries.impl;

import geometries.api.Intersectable;
import primitives.Color;
import primitives.Material;
import primitives.Point;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * MeshLoader - loads a triangulated 3D mesh from a Wavefront OBJ file and
 * converts it into a flat list of {@link Triangle} geometries.
 *
 * <p>Supported OBJ features:</p>
 * <ul>
 *   <li>{@code v x y z} — vertex positions.</li>
 *   <li>{@code f i1 i2 i3 ...} — faces, referencing 1-based vertex indices.
 *       Faces with more than 3 vertices are triangulated with a simple fan
 *       from the first vertex — correct for the convex faces (triangles
 *       and quads) that the vast majority of exported models use.</li>
 *   <li>Index tokens of the form {@code v/vt/vn} or {@code v//vn} — the
 *       texture/normal indices are simply ignored: this project's
 *       {@code Triangle} always computes its own flat face normal from the
 *       three vertex positions (see {@code Polygon}).</li>
 * </ul>
 *
 * <p><b>NOT</b> supported: per-vertex normals/UVs from the file (ignored, as
 * above), per-face materials from a companion {@code .mtl} file (every
 * triangle in one {@link #load} call gets the single emission color and
 * material passed in), and negative (relative) vertex indices.</p>
 *
 * <p><b>Axis convention:</b> many modeling tools — 3ds Max among them —
 * export OBJ files with the file's Z axis as "up", while this engine's
 * world (Camera, Cylinder axes, etc.) treats Y as "up". The {@code
 * zUpSource} parameter, when {@code true}, remaps the file's Z axis to this
 * engine's Y axis (and the file's Y axis to this engine's Z/depth axis)
 * before scaling, rotating, or translating.</p>
 *
 * <p><b>Design note — why this returns a flat {@code List<Intersectable>}
 * and not a nested {@code Geometries}:</b> if a loaded mesh were wrapped in
 * its own {@code Geometries} and added as a single child of the scene, the
 * BVH built at the scene level would treat the entire mesh as one opaque,
 * un-subdivided leaf — every ray that touches the mesh's bounding box would
 * still brute-force all of its triangles. Returning a flat list lets the
 * caller add the triangles directly to {@code scene.geometries}, so the
 * scene-level BVH subdivides *inside* the mesh too, exactly like it does for
 * every other geometry.</p>
 */
public final class MeshLoader {

    private MeshLoader() {} // utility class — not instantiable

    /**
     * Loads an OBJ mesh file assuming the file already uses a Y-up
     * convention (no axis remap). Convenience overload of
     * {@link #load(String, Color, Material, Point, double, double, boolean)}.
     */
    public static List<Intersectable> load(String filePath, Color emission, Material material,
                                           Point position, double scale, double rotationYDegrees) {
        return load(filePath, emission, material, position, scale, rotationYDegrees, false);
    }

    /**
     * Loads an OBJ mesh file and returns it as a flat list of {@link Triangle}
     * objects, transformed (axis remap, scale, rotate around Y, translate)
     * to place it in the scene.
     *
     * @param filePath         path to the {@code .obj} file
     * @param emission         flat emission color applied to every triangle —
     *                         the per-facet brightness variation in the final
     *                         render comes entirely from real per-face
     *                         lighting (different face normals), not from
     *                         varying this color
     * @param material         material applied to every triangle
     * @param position         world-space position of the mesh's local origin
     * @param scale            uniform scale factor applied before placement
     * @param rotationYDegrees rotation around the Y axis, in degrees, applied
     *                         after the axis remap and scale, before translation
     * @param zUpSource        true if the source file uses Z as "up" (common
     *                         for 3ds Max exports) and needs remapping to this
     *                         engine's Y-up world; false if the file is already Y-up
     * @return a flat list of Triangle geometries, ready to be added directly
     *         to {@code scene.geometries}
     * @throws RuntimeException if the file cannot be read or parsed
     */
    public static List<Intersectable> load(String filePath, Color emission, Material material,
                                           Point position, double scale, double rotationYDegrees,
                                           boolean zUpSource) {
        List<Point> rawVertices = new ArrayList<>();
        List<Intersectable> result = new ArrayList<>();

        double radians = Math.toRadians(rotationYDegrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("v ")) {
                    rawVertices.add(parseVertex(line, scale, cos, sin, position, zUpSource));
                } else if (line.startsWith("f ")) {
                    addFace(result, line, rawVertices, emission, material);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("MeshLoader: cannot read OBJ file: " + filePath, e);
        }

        return result;
    }

    /**
     * Parses a {@code v x y z} line into a world-space Point, applying the
     * axis remap (if requested), scale, Y-axis rotation, and translation —
     * in that order.
     */
    private static Point parseVertex(String line, double scale, double cos, double sin,
                                     Point position, boolean zUpSource) {
        String[] tokens = line.split("\\s+");
        double fileX = Double.parseDouble(tokens[1]);
        double fileY = Double.parseDouble(tokens[2]);
        double fileZ = Double.parseDouble(tokens[3]);

        // Axis remap: if the source file is Z-up, its vertical axis (Z)
        // becomes our vertical axis (Y), and its depth axis (Y) becomes
        // our depth axis (Z). If the file is already Y-up, no remap needed.
        double upAxisValue = zUpSource ? fileZ : fileY;
        double depthAxisValue = zUpSource ? fileY : fileZ;

        double x = fileX * scale;
        double y = upAxisValue * scale;
        double z = depthAxisValue * scale;

        // Rotate around the Y axis: x' = x*cos + z*sin ; z' = -x*sin + z*cos
        double rotatedX = x * cos + z * sin;
        double rotatedZ = -x * sin + z * cos;

        return new Point(position.getX() + rotatedX,
                position.getY() + y,
                position.getZ() + rotatedZ);
    }

    /**
     * Parses an {@code f ...} line into one or more Triangle objects (fan
     * triangulation for faces with more than 3 vertices) and appends them
     * to {@code result}.
     */
    private static void addFace(List<Intersectable> result, String line, List<Point> rawVertices,
                                Color emission, Material material) {
        String[] tokens = line.split("\\s+");
        int faceVertexCount = tokens.length - 1;
        if (faceVertexCount < 3) return; // malformed face line, skip

        int[] indices = new int[faceVertexCount];
        for (int i = 0; i < faceVertexCount; i++)
            indices[i] = parseVertexIndex(tokens[i + 1]);

        for (int i = 1; i < faceVertexCount - 1; i++) {
            Point p0 = rawVertices.get(indices[0]);
            Point p1 = rawVertices.get(indices[i]);
            Point p2 = rawVertices.get(indices[i + 1]);
            try {
                result.add(new Triangle(p0, p1, p2)
                        .setEmission(emission)
                        .setMaterial(material));
            } catch (IllegalArgumentException degenerate) {
                // A handful of exported meshes contain zero-area/collinear
                // faces — skip them rather than failing the whole load.
            }
        }
    }

    /**
     * Extracts the vertex index from a face token, which may be a plain
     * integer or include texture/normal indices ({@code "12/4/7"} or
     * {@code "12//7"}). Texture/normal indices are ignored — see the
     * class-level Javadoc.
     */
    private static int parseVertexIndex(String token) {
        String vertexPart = token.split("/")[0];
        return Integer.parseInt(vertexPart) - 1; // OBJ indices are 1-based
    }
}