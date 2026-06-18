package renderer;

import geometries.impl.*;
import java.io.IOException;
import lighting.*;
import org.junit.jupiter.api.Test;
import primitives.*;
import scene.Scene;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Mini-Project 2 — Performance Acceleration (Bounding Volume Hierarchy).
 *
 * <p>Demonstrates two independent performance improvements on top of the
 * Mini-Project 1 pipeline (Anti-Aliasing + Soft Shadows, kept active here at
 * demo quality so the MP1 feature is visibly still working):</p>
 * <ol>
 *   <li><b>Multi-threading</b> — already implemented in {@link Camera}
 *       (raw threads / parallel stream), selected via {@code setMultithreading}.</li>
 *   <li><b>BVH acceleration</b> — implemented in {@code geometries.impl.BVHNode},
 *       activated per-scene via {@code scene.geometries.buildBVH()}.</li>
 * </ol>
 *
 * <p><b>Scene description — "Low-Poly Savanna Sunset":</b> a faceted sky and
 * a faceted ground, each built from a dense grid of small flat-shaded
 * triangles (the classic "low-poly art" look — every triangle keeps a
 * single constant normal, and a low, raking directional light makes each
 * facet catch a slightly different brightness). A glowing sun sits on the
 * horizon, also acting as the main light source; silhouette acacia trees
 * stand against the bright sky; a handful of small glassy/reflective
 * "rocks" sparkle in the foreground. This dense grid of triangles is also
 * exactly what the BVH needs to prove itself — thousands of small,
 * spatially-clustered primitives are the textbook case for bounding-volume
 * culling.</p>
 *
 * <p>The whole scene is built from deterministic formulas (no
 * {@code Math.random()}), so every call to {@link #buildBvhDemoScene()}
 * produces an <em>identical</em> scene — required so the four measurement
 * configurations below are a fair, apples-to-apples comparison.</p>
 */
class MiniProject2Tests {

    MiniProject2Tests() {}

    // ========================= Sky (faceted backdrop) =========================

    private static final int SKY_COLS = 26;
    private static final int SKY_ROWS = 14;
    private static final double SKY_X_MIN = -320, SKY_X_MAX = 320;
    private static final double SKY_BOTTOM_Y = -60, SKY_TOP_Y = 230;
    private static final double SKY_BASE_Z = -380;
    /** How far each sky vertex is randomly pushed forward/back — breaks the wall into facets. */
    private static final double SKY_DEPTH_JITTER = 14;

    // ========================= Ground (faceted terrain) =========================

    private static final int GROUND_COLS = 26;
    private static final int GROUND_ROWS = 16;
    private static final double GROUND_NEAR_Z = 380, GROUND_FAR_Z = -380;
    private static final double GROUND_BASE_Y = -62;
    /** How far each ground vertex is randomly pushed up/down — breaks the floor into facets. */
    private static final double GROUND_HEIGHT_JITTER = 7;

    // ========================= Palette (warm sunset gradient) =========================

    private static final double[] HORIZON_COLOR     = {255, 150, 60};  // warm orange, low in the sky
    private static final double[] DUSK_COLOR         = {35, 28, 80};   // deep dusk purple-blue, high up
    private static final double[] GROUND_NEAR_COLOR  = {60, 28, 22};   // dark warm brown, close to camera
    private static final double[] GROUND_FAR_COLOR   = {235, 120, 50}; // glowing orange at the horizon

    // ========================= Scene dressing =========================

    private static final int NUM_TREES = 6;
    private static final int NUM_ACCENT_ROCKS = 8;

    // ========================= MP1 feature quality (kept active here) =========================

    /** Anti-Aliasing grid size: 3x3 = 9 rays/pixel — enough to visibly smooth edges. */
    private static final int AA_SAMPLES = 3;
    /** Soft Shadow grid size: 3x3 = 9 shadow rays per light. */
    private static final int SS_SAMPLES = 3;

    // ========================= Small math helpers =========================

    /** Fractional part of {@code x} — builds a repeatable, evenly-spread pseudo-random sequence. */
    private static double frac(double x) {
        return x - Math.floor(x);
    }

    /** Clamps {@code t} into [0,1]. */
    private static double clamp01(double t) {
        return Math.max(0, Math.min(1, t));
    }

    /** Linearly interpolates between two RGB triplets at parameter {@code t} (clamped to [0,1]). */
    private static Color lerpColor(double r0, double g0, double b0,
                                   double r1, double g1, double b1, double t) {
        double tc = clamp01(t);
        return new Color(r0 + (r1 - r0) * tc, g0 + (g1 - g0) * tc, b0 + (b1 - b0) * tc);
    }

    // ========================= Faceted surface builder =========================

    /**
     * Builds a faceted (flat-shaded, low-poly) surface from a grid of
     * vertices: each grid cell becomes two triangles. {@code vertexFn} maps
     * grid indices (col, row) to a 3D point (typically with some per-vertex
     * jitter to break the surface into visible facets), and {@code colorFn}
     * maps a triangle's centroid to its emission color (typically a
     * position-based gradient).
     *
     * @param scene    the scene to add the surface to
     * @param cols     number of grid cells along the first axis
     * @param rows     number of grid cells along the second axis
     * @param vertexFn maps (col, row) in [0..cols]x[0..rows] to a 3D point
     * @param colorFn  maps a triangle centroid to its emission color
     * @param material shared material for every triangle of the surface
     */
    private static void addFacetedSurface(Scene scene, int cols, int rows,
                                          BiFunction<Integer, Integer, Point> vertexFn,
                                          Function<Point, Color> colorFn,
                                          Material material) {
        Point[][] grid = new Point[rows + 1][cols + 1];
        for (int r = 0; r <= rows; r++)
            for (int c = 0; c <= cols; c++)
                grid[r][c] = vertexFn.apply(c, r);

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Point p00 = grid[r][c];
                Point p01 = grid[r][c + 1];
                Point p10 = grid[r + 1][c];
                Point p11 = grid[r + 1][c + 1];

                // Each triangle keeps one constant normal (no smoothing), and
                // adjacent triangles sit at slightly different angles because
                // of the per-vertex jitter — that mismatch in normals is
                // exactly what produces the faceted "low-poly" shading look.
                addFacetedTriangle(scene, p00, p01, p10, colorFn, material);
                addFacetedTriangle(scene, p01, p11, p10, colorFn, material);
            }
        }
    }

    private static void addFacetedTriangle(Scene scene, Point a, Point b, Point c,
                                           Function<Point, Color> colorFn, Material material) {
        Point centroid = new Point(
                (a.getX() + b.getX() + c.getX()) / 3,
                (a.getY() + b.getY() + c.getY()) / 3,
                (a.getZ() + b.getZ() + c.getZ()) / 3);
        scene.geometries.add(new Triangle(a, b, c)
                .setEmission(colorFn.apply(centroid))
                .setMaterial(material));
    }

    // ========================= Scene Setup =========================

    /**
     * Builds the "Low-Poly Savanna Sunset" scene used by all four
     * measurement tests below. Pure function of nothing but the constants
     * above — no external state, no randomness — so every call returns an
     * identical scene, required for a fair BVH/MT comparison.
     *
     * @return a freshly built scene
     */
    private static Scene buildBvhDemoScene() {
        Scene scene = new Scene("Low-Poly Savanna Sunset");
        scene.setBackground(new Color(20, 14, 30));
        scene.setAmbientLight(new AmbientLight(new Color(18, 12, 16), new Double3(1)));

        Material facetMaterial = new Material().setKD(0.75).setKS(0.15).setShininess(25);

        // ── Sky: a wide faceted wall of triangles, colored by height
        //    (dusk purple high up, warm horizon glow down low). ─────────────
        addFacetedSurface(scene, SKY_COLS, SKY_ROWS,
                (c, r) -> {
                    double x = SKY_X_MIN + (SKY_X_MAX - SKY_X_MIN) * c / (double) SKY_COLS;
                    double y = SKY_BOTTOM_Y + (SKY_TOP_Y - SKY_BOTTOM_Y) * r / (double) SKY_ROWS;
                    double jitterZ = (2 * frac(c * 0.37 + r * 0.61) - 1) * SKY_DEPTH_JITTER;
                    return new Point(x, y, SKY_BASE_Z + jitterZ);
                },
                point -> {
                    double t = clamp01((point.getY() - SKY_BOTTOM_Y) / (SKY_TOP_Y - SKY_BOTTOM_Y));
                    double brightness = 0.9 + 0.2 * frac(point.getX() * 0.053 + point.getZ() * 0.029);
                    return lerpColor(HORIZON_COLOR[0], HORIZON_COLOR[1], HORIZON_COLOR[2],
                            DUSK_COLOR[0], DUSK_COLOR[1], DUSK_COLOR[2], t).scale(brightness);
                },
                facetMaterial);

        // ── Ground: a wide faceted terrain, colored by depth (dark near the
        //    camera, glowing orange toward the horizon where it meets the sky). ─
        addFacetedSurface(scene, GROUND_COLS, GROUND_ROWS,
                (c, r) -> {
                    double x = SKY_X_MIN + (SKY_X_MAX - SKY_X_MIN) * c / (double) GROUND_COLS;
                    double z = GROUND_NEAR_Z + (GROUND_FAR_Z - GROUND_NEAR_Z) * r / (double) GROUND_ROWS;
                    double jitterY = (2 * frac(c * 0.53 + r * 0.19) - 1) * GROUND_HEIGHT_JITTER;
                    return new Point(x, GROUND_BASE_Y + jitterY, z);
                },
                point -> {
                    double t = clamp01((GROUND_NEAR_Z - point.getZ()) / (GROUND_NEAR_Z - GROUND_FAR_Z));
                    double brightness = 0.9 + 0.2 * frac(point.getX() * 0.071 + point.getZ() * 0.043);
                    return lerpColor(GROUND_NEAR_COLOR[0], GROUND_NEAR_COLOR[1], GROUND_NEAR_COLOR[2],
                            GROUND_FAR_COLOR[0], GROUND_FAR_COLOR[1], GROUND_FAR_COLOR[2], t).scale(brightness);
                },
                facetMaterial);

        // ── The setting sun — also the scene's main light source. ───────────
        Point sunPosition = new Point(0, -25, SKY_BASE_Z + 15);
        scene.geometries.add(new Sphere(sunPosition, 38)
                .setEmission(new Color(255, 225, 150))
                .setMaterial(new Material().setKD(0.3).setKS(0.2).setShininess(20).setKR(0.05))
                .setLightSource());

        // ── Silhouette acacia trees scattered near the horizon. ─────────────
        for (int i = 0; i < NUM_TREES; i++) {
            double x = -260 + i * 100 + 30 * (frac(i * 0.81) - 0.5);
            double z = -180 - 60 * frac(i * 0.47);
            addAcaciaTree(scene, new Point(x, GROUND_BASE_Y, z), 1.0 + 0.4 * frac(i * 0.29));
        }

        // ── Foreground accent rocks — small reflective/glassy spheres near the
        //    camera, formally exercising reflection and transparency, and
        //    doubling as glints of light on the savanna floor. ───────────────
        for (int i = 0; i < NUM_ACCENT_ROCKS; i++) {
            double x = -180 + i * 50 + 20 * (frac(i * 0.62) - 0.5);
            double z = 250 + 80 * frac(i * 0.37);
            double size = 5 + 6 * frac(i * 0.91);
            Material rockMaterial = (i % 2 == 0)
                    ? new Material().setKD(0.1).setKS(0.6).setShininess(120).setKR(0.5)    // polished
                    : new Material().setKD(0.05).setKS(0.5).setShininess(150).setKT(0.55); // glassy
            scene.geometries.add(new Sphere(new Point(x, GROUND_BASE_Y + size * 0.6, z), size)
                    .setEmission(new Color(180, 90, 40))
                    .setMaterial(rockMaterial));
        }

        // ── Five light sources, covering all four supported types ───────────

        // 1. Ambient — set above.

        // 2. Directional — the low, raking sunset light responsible for the
        //    strong per-facet highlight/shadow contrast across the terrain.
        scene.lights.add(new DirectionalLight(new Color(180, 90, 40), new Vector(0.25, -0.2, 1)));

        // 3. Point light — the sun itself; large soft-shadow size (big solar disc).
        scene.lights.add(new PointLight(new Color(255, 200, 130), sunPosition)
                .setKl(0.0004).setKq(0.000002).setSize(25));

        // 4. Point light — cool fill light from the opposite side (classic
        //    painter's trick: warm sun vs. cool shadow fill).
        scene.lights.add(new PointLight(new Color(50, 70, 120), new Point(220, 120, 150))
                .setKl(0.0007).setKq(0.000004).setSize(12));

        // 5. Spot light — a focused "god ray" breaking through, for drama.
        scene.lights.add(new SpotLight(new Color(255, 230, 180), new Point(40, 200, 0), new Vector(-0.2, -1, -0.3))
                .setNarrowBeam(5).setKl(0.0003).setKq(0.000002).setSize(10));

        return scene;
    }

    /**
     * Adds a simple silhouette acacia tree: a thin trunk and two overlapping
     * flat canopy triangles, all in near-black tones so they read as
     * silhouettes against the bright sky.
     */
    private static void addAcaciaTree(Scene scene, Point base, double scale) {
        Material silhouette = new Material().setKD(0.5).setKS(0.05).setShininess(5);
        Color trunkColor = new Color(18, 12, 8);
        Color canopyColor = new Color(22, 16, 10);

        double trunkWidth = 1.5 * scale;
        double trunkHeight = 14 * scale;

        scene.geometries.add(new Triangle(
                base.add(new Vector(-trunkWidth, 0.01, 0)),
                base.add(new Vector(trunkWidth, 0.01, 0)),
                base.add(new Vector(0, trunkHeight, 0)))
                .setEmission(trunkColor).setMaterial(silhouette));

        Point crown = base.add(new Vector(0, trunkHeight, 0));
        double canopyWidth = 16 * scale;
        double canopyHeight = 9 * scale;

        scene.geometries.add(new Triangle(
                crown.add(new Vector(-canopyWidth, canopyHeight * 0.3, -1)),
                crown.add(new Vector(canopyWidth, canopyHeight * 0.3, -1)),
                crown.add(new Vector(0, canopyHeight, -1)))
                .setEmission(canopyColor).setMaterial(silhouette));

        scene.geometries.add(new Triangle(
                crown.add(new Vector(-canopyWidth * 1.3, canopyHeight * 0.5, 1)),
                crown.add(new Vector(canopyWidth * 1.3, canopyHeight * 0.5, 1)),
                crown.add(new Vector(0, canopyHeight * 0.75, 1)))
                .setEmission(canopyColor).setMaterial(silhouette));
    }

    // ========================= Camera Setup =========================

    /**
     * Creates a {@link Camera.Builder} pre-configured for the savanna scene,
     * framed wide (16:9-ish) to suit a landscape composition.
     */
    private static Camera.Builder buildCameraBuilder(Scene scene, SimpleRayTracer rayTracer) {
        return Camera.getBuilder()
                .setRayTracer(scene, rayTracer)
                .setLocation(new Point(0, 45, 480))
                .setDirection(new Point(0, 25, -140), Vector.AXIS_Y)
                .setVpDistance(380)
                .setVpSize(380, 214)
                .setResolution(640, 360)
                .setDebugPrint(5);
    }

    // ========================= Measurement Helper =========================

    /**
     * Builds a fresh scene, optionally enables BVH, configures multi-threading,
     * renders, writes the image, and returns the elapsed render time in
     * milliseconds. Used by all four mandatory measurement tests below, plus
     * the optional aggregate report, so the timing/printing logic is written
     * exactly once (DRY).
     */
    private static long runMeasurement(boolean useBVH, int threads, String label, String fileName) {
        Scene scene = buildBvhDemoScene();
        if (useBVH) scene.geometries.buildBVH();

        SimpleRayTracer rayTracer = new SimpleRayTracer(scene)
                .setSoftShadowSamples(SS_SAMPLES)
                .setSamplingPattern(Blackboard.SamplingPattern.JITTERED);

        Camera camera = buildCameraBuilder(scene, rayTracer)
                .setAntiAliasingSamples(AA_SAMPLES)
                .setMultithreading(threads)
                .build();

        System.out.println("=== " + label + " (BVH=" + useBVH + ", threads=" + threads + ") ===");
        long start = System.currentTimeMillis();
        camera.renderImage().writeToImage(fileName);
        long elapsed = System.currentTimeMillis() - start;
        System.out.printf("%s: %,d ms%n", label, elapsed);
        return elapsed;
    }

    // ========================= Mandatory Measurement Configurations =========================

    /** Configuration 1 — baseline: BVH disabled, multithreading disabled. */
    @Test
    void measurement_NoAcceleration_NoMultithreading() {
        runMeasurement(false, 0, "Config 1: Acceleration OFF, Multithreading OFF", "mp2_accelOFF_mtOFF");
    }

    /** Configuration 2 — BVH disabled, multithreading enabled (auto core count). */
    @Test
    void measurement_NoAcceleration_WithMultithreading() {
        runMeasurement(false, -2, "Config 2: Acceleration OFF, Multithreading ON", "mp2_accelOFF_mtON");
    }

    /** Configuration 3 — BVH enabled, multithreading disabled. */
    @Test
    void measurement_WithBVH_NoMultithreading() {
        runMeasurement(true, 0, "Config 3: Acceleration ON, Multithreading OFF", "mp2_accelON_mtOFF");
    }

    /** Configuration 4 — BVH enabled, multithreading enabled (auto core count). */
    @Test
    void measurement_WithBVH_WithMultithreading() {
        runMeasurement(true, -2, "Config 4: Acceleration ON, Multithreading ON", "mp2_accelON_mtON");
    }

    // ========================= Optional Aggregate Report =========================

    /**
     * Convenience test that runs all four configurations back-to-back and
     * prints a single comparison table.
     */
    @Test
    void measurement_FullComparisonReport() {
        long baseline = runMeasurement(false, 0, "Report 1/4: Accel OFF, MT OFF", "mp2_report_accelOFF_mtOFF");
        long mtOnly = runMeasurement(false, -2, "Report 2/4: Accel OFF, MT ON", "mp2_report_accelOFF_mtON");
        long bvhOnly = runMeasurement(true, 0, "Report 3/4: Accel ON, MT OFF", "mp2_report_accelON_mtOFF");
        long both = runMeasurement(true, -2, "Report 4/4: Accel ON, MT ON", "mp2_report_accelON_mtON");

        System.out.println();
        System.out.println("==================== PERFORMANCE SUMMARY ====================");
        System.out.printf("Baseline (no accel, no MT):   %,9d ms   (x1.00)%n", baseline);
        System.out.printf("Multithreading only:          %,9d ms   (x%.2f)%n", mtOnly, ratio(baseline, mtOnly));
        System.out.printf("BVH only:                     %,9d ms   (x%.2f)%n", bvhOnly, ratio(baseline, bvhOnly));
        System.out.printf("BVH + Multithreading:         %,9d ms   (x%.2f)%n", both, ratio(baseline, both));
        System.out.println("===============================================================");
    }

    /** Speedup ratio of {@code baseline} relative to {@code current}, guarding against division by zero. */
    private static double ratio(long baseline, long current) {
        return current == 0 ? Double.POSITIVE_INFINITY : (double) baseline / current;
    }

    }











