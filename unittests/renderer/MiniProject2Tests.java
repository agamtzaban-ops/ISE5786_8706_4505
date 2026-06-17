package renderer;

import geometries.impl.*;
import lighting.*;
import org.junit.jupiter.api.Test;
import primitives.*;
import scene.Scene;

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
 * <p><b>Scene description — "Asteroid Belt Observatory":</b> a glowing
 * central core, a ring of reflective "planet" spheres, a dense asteroid
 * belt of small spheres (the main BVH stress-test — hundreds of small,
 * spatially spread-out objects), a ring of glassy crystal shards
 * (triangles, exercising transparency), six reflective structural pillars
 * (cylinders), and a dark backdrop plane (intentionally left unbounded —
 * exercises the "always test directly" path for infinite geometries even
 * when BVH is active). Lit by five sources covering all four supported
 * light types: ambient, directional (sunlight), two point lights (core
 * glow + rim light, both with soft-shadow size), and a spot light
 * (dramatic highlight on the belt).</p>
 *
 * <p>The scene is built from purely deterministic formulas (a golden-angle
 * spiral and a low-discrepancy fractional sequence) rather than
 * {@code Math.random()}, so every call to {@link #buildBvhDemoScene()}
 * produces an <em>identical</em> scene — required so that the four
 * measurement configurations below are a fair, apples-to-apples comparison.</p>
 */
class MiniProject2Tests {

    MiniProject2Tests() {}

    // ========================= Scene-size constants =========================
    // Tunable knobs, not hard-coded magic numbers scattered through the code.
    // Increase NUM_ASTEROIDS / resolution if the baseline (Config 1) render
    // finishes in well under ~20 seconds on your machine — a too-fast
    // baseline makes the speedup ratio unreliable to measure.

    /** Number of small spheres in the asteroid belt — the main BVH stress-test. */
    private static final int NUM_ASTEROIDS = 400;
    /** Number of glassy triangular crystal shards (transparency exercise). */
    private static final int NUM_CRYSTALS = 150;
    /** Number of structural cylinder pillars. */
    private static final int NUM_PILLARS = 6;

    private static final double BELT_INNER_RADIUS = 70;
    private static final double BELT_OUTER_RADIUS = 160;
    private static final double BELT_THICKNESS = 40;
    private static final double ASTEROID_MIN_RADIUS = 1.0;
    private static final double ASTEROID_MAX_RADIUS = 3.0;

    private static final double CRYSTAL_INNER_RADIUS = 30;
    private static final double CRYSTAL_OUTER_RADIUS = 55;
    private static final double CRYSTAL_SIZE = 4.0;

    private static final double PILLAR_RING_RADIUS = 220;
    private static final double PILLAR_HEIGHT = 120;
    private static final double PILLAR_RADIUS = 5;

    /** Golden angle (radians) — produces an even, non-repeating spiral distribution. */
    private static final double GOLDEN_ANGLE = Math.toRadians(137.50776);
    /** Fractional part of 1/phi — used as a deterministic low-discrepancy sequence step. */
    private static final double GOLDEN_FRAC_STEP = 0.6180339887;

    // ========================= MP1 feature quality (kept active here) =========================

    /** Anti-Aliasing grid size: 3x3 = 9 rays/pixel — enough to visibly smooth edges. */
    private static final int AA_SAMPLES = 3;
    /** Soft Shadow grid size: 3x3 = 9 shadow rays per light. */
    private static final int SS_SAMPLES = 3;

    // ========================= Deterministic placement helper =========================

    /** Returns the fractional part of {@code x} — used to build a repeatable pseudo-random sequence. */
    private static double frac(double x) {
        return x - Math.floor(x);
    }

    // ========================= Scene Setup =========================

    /**
     * Builds the "Asteroid Belt Observatory" scene used by all four
     * measurement tests below. Pure function of nothing but the constants
     * above — no external state, no randomness — so every call returns an
     * identical scene, which is required for a fair BVH/MT comparison.
     *
     * @return a freshly built scene
     */
    private static Scene buildBvhDemoScene() {
        Scene scene = new Scene("Asteroid Belt Observatory");
        scene.setBackground(new Color(2, 2, 5));
        scene.setAmbientLight(new AmbientLight(new Color(8, 8, 12), new Double3(1)));

        // ── Backdrop plane — intentionally infinite/unbounded. Even when BVH
        //    is enabled this geometry cannot be placed in the tree and must
        //    always be tested directly (see Geometries.buildBVH()). ─────────
        scene.geometries.add(new Plane(new Point(0, 0, -300), new Vector(0, 0, 1))
                .setEmission(new Color(3, 3, 8))
                .setMaterial(new Material().setKD(0.4).setKS(0.1).setShininess(10)));

        // ── Glowing core — also marked as a light source so shadow rays
        //    ignore it (it should never shadow itself). ─────────────────────
        scene.geometries.add(new Sphere(new Point(0, 0, 0), 22)
                .setEmission(new Color(220, 160, 60))
                .setMaterial(new Material().setKD(0.5).setKS(0.4).setShininess(60).setKR(0.1))
                .setLightSource());

        // ── Ring of six reflective "planet" spheres around the core ───────
        for (int i = 0; i < 6; i++) {
            double angle = i * (2 * Math.PI / 6);
            double x = 40 * Math.cos(angle);
            double z = 40 * Math.sin(angle);
            scene.geometries.add(new Sphere(new Point(x, 0, z), 8)
                    .setEmission(new Color(60 + i * 20, 90, 160 - i * 15))
                    .setMaterial(new Material().setKD(0.4).setKS(0.5).setShininess(100).setKR(0.3)));
        }

        // ── Asteroid belt — the main BVH stress-test: hundreds of small,
        //    spatially scattered spheres. Placed on a golden-angle spiral so
        //    they spread out evenly without ever repeating a pattern. ──────
        for (int i = 0; i < NUM_ASTEROIDS; i++) {
            double angle = i * GOLDEN_ANGLE;
            double radial = BELT_INNER_RADIUS
                    + (BELT_OUTER_RADIUS - BELT_INNER_RADIUS) * frac(i * GOLDEN_FRAC_STEP);
            double height = (frac(i * 0.37) - 0.5) * BELT_THICKNESS;
            double size = ASTEROID_MIN_RADIUS
                    + (ASTEROID_MAX_RADIUS - ASTEROID_MIN_RADIUS) * frac(i * 0.74);

            Point center = new Point(radial * Math.cos(angle), height, radial * Math.sin(angle));

            Material material = (i % 3 == 0)
                    ? new Material().setKD(0.3).setKS(0.5).setShininess(80).setKR(0.4)   // reflective
                    : new Material().setKD(0.7).setKS(0.2).setShininess(20);             // matte

            Color emission = new Color(
                    90 + 60 * frac(i * 0.13),
                    70 + 60 * frac(i * 0.29),
                    60 + 60 * frac(i * 0.51));

            scene.geometries.add(new Sphere(center, size).setEmission(emission).setMaterial(material));
        }

        // ── Crystal shards — glassy triangles, exercising transparency ────
        for (int i = 0; i < NUM_CRYSTALS; i++) {
            double angle = i * GOLDEN_ANGLE * 1.3;
            double radial = CRYSTAL_INNER_RADIUS
                    + (CRYSTAL_OUTER_RADIUS - CRYSTAL_INNER_RADIUS) * frac(i * GOLDEN_FRAC_STEP);
            double height = (frac(i * 0.21) - 0.5) * 20;

            Point center = new Point(radial * Math.cos(angle), height, radial * Math.sin(angle));

            // Build a small flat triangle facing roughly outward from the core.
            Vector vRight = new Vector(Math.cos(angle + Math.PI / 2), 0, Math.sin(angle + Math.PI / 2));
            Vector vUp = Vector.AXIS_Y;

            Point p1 = center.add(vRight.scale(CRYSTAL_SIZE));
            Point p2 = center.add(vRight.scale(-CRYSTAL_SIZE / 2)).add(vUp.scale(CRYSTAL_SIZE * 0.87));
            Point p3 = center.add(vRight.scale(-CRYSTAL_SIZE / 2)).add(vUp.scale(-CRYSTAL_SIZE * 0.87));

            scene.geometries.add(new Triangle(p1, p2, p3)
                    .setEmission(new Color(40, 80, 140))
                    .setMaterial(new Material().setKD(0.05).setKS(0.6).setShininess(150).setKT(0.6)));
        }

        // ── Structural pillars — finite cylinders, placed in a hexagon ─────
        for (int i = 0; i < NUM_PILLARS; i++) {
            double angle = i * (2 * Math.PI / NUM_PILLARS);
            double x = PILLAR_RING_RADIUS * Math.cos(angle);
            double z = PILLAR_RING_RADIUS * Math.sin(angle);
            scene.geometries.add(new Cylinder(
                    new Ray(new Point(x, -PILLAR_HEIGHT / 2, z), Vector.AXIS_Y),
                    PILLAR_RADIUS, PILLAR_HEIGHT)
                    .setEmission(new Color(50, 50, 60))
                    .setMaterial(new Material().setKD(0.3).setKS(0.5).setShininess(90).setKR(0.5)));
        }

        // ── Five light sources, covering all four supported light types ───

        // 1. Ambient — already set above.

        // 2. Directional — distant "sunlight" raking across the whole scene.
        scene.lights.add(new DirectionalLight(new Color(40, 35, 30), new Vector(-1, -0.6, -0.3)));

        // 3. Point light — the core's own glow, with soft-shadow size.
        scene.lights.add(new PointLight(new Color(255, 200, 120), new Point(0, 0, 0))
                .setKl(0.0005).setKq(0.000003).setSize(15));

        // 4. Point light — cool rim light from the opposite side, soft shadows.
        scene.lights.add(new PointLight(new Color(80, 110, 200), new Point(-180, 90, -180))
                .setKl(0.0008).setKq(0.000005).setSize(10));

        // 5. Spot light — dramatic, focused highlight on the asteroid belt.
        scene.lights.add(new SpotLight(new Color(255, 255, 255),
                new Point(0, 160, 0), new Vector(0.3, -1, 0.3))
                .setNarrowBeam(4).setKl(0.0003).setKq(0.000002).setSize(8));

        return scene;
    }

    // ========================= Camera Setup =========================

    /**
     * Creates a {@link Camera.Builder} pre-configured for the asteroid belt scene.
     * Resolution is moderate by default — raise it for the final submission image.
     */
    private static Camera.Builder buildCameraBuilder(Scene scene, SimpleRayTracer rayTracer) {
        return Camera.getBuilder()
                .setRayTracer(scene, rayTracer)
                .setLocation(new Point(0, 180, 420))
                .setDirection(new Point(0, 0, 0), Vector.AXIS_Y)
                .setVpDistance(350)
                .setVpSize(280, 280)
                .setResolution(500, 500)
                .setDebugPrint(5);
    }

    // ========================= Measurement Helper =========================

    /**
     * Builds a fresh scene, optionally enables BVH, configures multi-threading,
     * renders, writes the image, and returns the elapsed render time in
     * milliseconds. Used by all four mandatory measurement tests below, plus
     * the optional aggregate report, so the timing/printing logic is written
     * exactly once (DRY).
     *
     * @param useBVH  whether to call {@code scene.geometries.buildBVH()} before rendering
     * @param threads multithreading parameter, passed directly to {@code Camera.Builder.setMultithreading}
     * @param label   human-readable description printed to the console
     * @param fileName output image file name (without extension)
     * @return elapsed render time in milliseconds
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
    // Per the assignment: each configuration is its own test method, with a
    // name that clearly states what is active and what is disabled.

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
     * prints a single comparison table — useful for quickly building the
     * MEASUREMENTS report. Not a replacement for the four separate tests
     * above (which remain the graded, individually named configurations).
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