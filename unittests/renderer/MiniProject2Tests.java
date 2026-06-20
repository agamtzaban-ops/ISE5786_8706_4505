package renderer;

import geometries.impl.*;
import lighting.*;
import org.junit.jupiter.api.Test;
import primitives.*;
import scene.Scene;

/**
 * Mini-Project 2 — Performance Acceleration (Bounding Volume Hierarchy).
 *
 * <p>Demonstrates two independent performance improvements:</p>
 * <ol>
 *   <li><b>Multi-threading</b> — raw threads / parallel stream via {@code setMultithreading}.</li>
 *   <li><b>BVH acceleration</b> — median-split hierarchy via {@code scene.geometries.buildBVH()}.</li>
 * </ol>
 *
 * <p><b>Scene — "Savanna Sunset":</b> a low-poly African landscape rendered at golden hour.
 * A 28×28 tessellated terrain (1568 triangles) is the main BVH stress-test — without
 * acceleration, every ray must be checked against every triangle individually.
 * The sky is a gradient of 96 triangles, mountain silhouettes add 8 large triangles,
 * six acacia trees (cylinder trunk + 4 canopy triangles each) contribute 30 geometries,
 * and a glowing sun sphere marks the light source. An infinite backdrop plane is kept
 * out of the BVH intentionally (unbounded geometry — exercises the always-test path).
 * All four light types and all four geometry types are present; reflections and
 * transparency are active; and the Mini-Project 1 pipeline (AA + soft shadows) is
 * demonstrated at full quality in the dedicated presentation-image test.</p>
 *
 * <p>All placement is deterministic — every call to {@link #buildScene()} returns an
 * identical scene, making the four timing configurations a fair comparison.</p>
 */
class MiniProject2Tests {

    MiniProject2Tests() {}

    // ========================= Terrain geometry =========================

    /** Side length of the square terrain grid (cells per side). 28×28×2 = 1568 triangles. */
    private static final int GRID = 28;

    /** Terrain world-space extents. */
    private static final double TX0 = -260, TX1 = 260, TZ0 = -130, TZ1 = 280;

    /** Y offset of the flat ground reference (before height-map lift). */
    private static final double BASE_Y = -62;

    // ========================= Sky geometry =========================
    private static final int SKY_COLS = 10, SKY_ROWS = 14; // 10×14×2 = 280 triangles

    // ========================= Rendering quality =========================
    // Timing tests use no AA and hard shadows so the single-threaded baseline
    // finishes in seconds, not hours; the presentation image uses full quality.

    private static final int TIMING_AA = 1; // off during timing
    private static final int TIMING_SS = 1; // hard shadows during timing
    private static final int FINAL_AA  = 9; // 9×9 for the presentation image
    private static final int FINAL_SS  = 3; // 3×3 soft shadows — sweet-spot for speed vs quality

    // ========================= Helpers =========================

    private static double frac(double x) { return x - Math.floor(x); }

    /** Terrain height-map: gentle desert dunes, range ≈ ±11 units. */
    private static double terrH(double x, double z) {
        return  5 * Math.sin(x * 0.022 + z * 0.015)
              + 3 * Math.cos(x * 0.038 - z * 0.028)
              + 2 * Math.sin(x * 0.055 + z * 0.047)
              + 1 * Math.cos(x * 0.081 - z * 0.072);
    }

    /** Terrain color: sandy desert — warm tan/beige with variation. */
    private static Color terrC(double x, double z, double h) {
        double t  = Math.max(0, Math.min(1, (h + 11) / 22.0));
        double rf = frac(x * 0.073 + z * 0.097 + x * z * 3e-5);
        return new Color(
            Math.min(255, (int)(165 + t * 42 + rf * 28)),
            Math.min(255, (int)(122 + t * 28 + rf * 18)),
            Math.min(255, (int)( 70 + t * 12 + rf *  8)));
    }

    /** Sky gradient: t=0 → warm cream at horizon; t=1 → clear blue at zenith. */
    private static Color skyC(double t) {
        return new Color(
            (int)(238 * (1 - t) + 72 * t),
            (int)(210 * (1 - t) + 158 * t),
            (int)(168 * (1 - t) + 228 * t));
    }

    // ========================= Scene =========================

    private static Scene buildScene() {
        Scene scene = new Scene("Desert Canyon");
        scene.setBackground(new Color(118, 188, 228));
        scene.setAmbientLight(new AmbientLight(new Color(88, 75, 55), new Double3(1)));

        Material flat  = new Material().setKD(0.88).setKS(0.08).setShininess(6);
        Material skyM  = new Material().setKD(0.80).setKS(0.12).setShininess(10);
        Material rockM = new Material().setKD(0.78).setKS(0.20).setShininess(22);
        Material cactM = new Material().setKD(0.82).setKS(0.18).setShininess(16);
        Material roadM = new Material().setKD(0.55).setKS(0.45).setShininess(35);

        // ── Backdrop plane (infinite → stays outside BVH) ─────────────────
        scene.geometries.add(
            new Plane(new Point(0, 0, -700), new Vector(0, 0, 1))
                .setEmission(new Color(108, 182, 222)).setMaterial(skyM));

        // ── Sky — 10×14 = 280 triangles, blue daytime gradient ────────────
        final double SX0 = -700, SX1 = 700, SZ = -600;
        final double SY0 = -22,  SY1 = 560;
        double sdx = (SX1 - SX0) / SKY_COLS;
        double sdy = (SY1 - SY0) / SKY_ROWS;
        for (int sr = 0; sr < SKY_ROWS; sr++) {
            double t0 = (double) sr      / SKY_ROWS;
            double t1 = (double)(sr + 1) / SKY_ROWS;
            double y0 = SY0 + sr * sdy, y1 = y0 + sdy;
            Color cBot = skyC(t0), cTop = skyC(t1);
            for (int sc = 0; sc < SKY_COLS; sc++) {
                double x0 = SX0 + sc * sdx, x1 = x0 + sdx;
                Point p00 = new Point(x0, y0, SZ);
                Point p10 = new Point(x1, y0, SZ);
                Point p11 = new Point(x1, y1, SZ);
                Point p01 = new Point(x0, y1, SZ);
                scene.geometries.add(new Triangle(p00, p10, p11).setEmission(cBot).setMaterial(skyM));
                scene.geometries.add(new Triangle(p00, p11, p01).setEmission(cTop).setMaterial(skyM));
            }
        }

        // ── Terrain — 28×28 = 1568 triangles (the BVH stress-test) ─────────
        double tdx = (TX1 - TX0) / GRID, tdz = (TZ1 - TZ0) / GRID;
        for (int gz = 0; gz < GRID; gz++) {
            for (int gx = 0; gx < GRID; gx++) {
                double x0 = TX0 + gx * tdx, x1 = x0 + tdx;
                double z0 = TZ0 + gz * tdz, z1 = z0 + tdz;
                double h00 = terrH(x0, z0), h10 = terrH(x1, z0);
                double h01 = terrH(x0, z1), h11 = terrH(x1, z1);
                Point p00 = new Point(x0, BASE_Y + h00, z0);
                Point p10 = new Point(x1, BASE_Y + h10, z0);
                Point p01 = new Point(x0, BASE_Y + h01, z1);
                Point p11 = new Point(x1, BASE_Y + h11, z1);
                scene.geometries.add(new Triangle(p00, p10, p11)
                    .setEmission(terrC((x0 + x1) / 2, z0, (h00 + h10 + h11) / 3.0))
                    .setMaterial(flat));
                scene.geometries.add(new Triangle(p00, p11, p01)
                    .setEmission(terrC(x0, (z0 + z1) / 2, (h00 + h01 + h11) / 3.0))
                    .setMaterial(flat));
            }
        }

        // ── Rock formations — terracotta mesa / butte silhouettes ────────────
        double[][] rocks = {
            // Left big mesa
            {-385,-62,-178, -305,148,-193, -242,138,-186,  155,62,22},
            {-385,-62,-178, -242,138,-186, -195,-62,-180,  138,50,18},
            {-305,148,-193, -288,162,-197, -242,138,-186,  172,70,27},
            // Left secondary spire
            {-268,-62,-173, -250, 98,-179, -225,-62,-175,  148,58,21},
            {-250, 98,-179, -240,114,-181, -225,-62,-175,  165,66,24},
            // Right big mesa
            { 218,-62,-168,  312,155,-184,  375,-62,-176,  152,60,20},
            { 312,155,-184,  332,172,-189,  375,-62,-176,  168,68,25},
            // Right tall spire
            { 362,-62,-162,  396,135,-172,  428,-62,-165,  162,64,23},
            { 290,-62,-165,  355,188,-183,  415,-62,-170,  140,54,19},
            // Background small formations
            {-152,-62,-208, -130, 58,-214, -108,-62,-210,  112,44,15},
            {  88,-62,-205,  120, 72,-210,  158,-62,-208,  120,48,17},
        };
        for (double[] r : rocks)
            scene.geometries.add(new Triangle(
                new Point(r[0],r[1],r[2]), new Point(r[3],r[4],r[5]), new Point(r[6],r[7],r[8]))
                .setEmission(new Color((int)r[9],(int)r[10],(int)r[11])).setMaterial(rockM));

        // ── Saguaro cacti — 6 cacti × 5 cylinders = 30 geometries ────────────
        Color cactusC = new Color(42, 128, 32);
        double[][] cactPos = {
            {-168, 65}, {-108, 98}, { 52, 40},
            { 165, 62}, { -40,-12}, { 238, 90}
        };
        for (double[] cp : cactPos) {
            double cx = cp[0], cz = cp[1];
            double cy  = BASE_Y + terrH(cx, cz);
            double tH  = 55 + frac(cx * 0.13 + cz * 0.19) * 30;
            double aL  = 18 + frac(cx * 0.27 + cz * 0.31) * 12;
            double aY1 = cy + tH * 0.45;
            double aY2 = cy + tH * 0.38;
            // Trunk
            scene.geometries.add(new Cylinder(
                new Ray(new Point(cx, cy, cz), Vector.AXIS_Y), 4.5, tH)
                .setEmission(cactusC).setMaterial(cactM));
            // Left arm outward
            Vector leftDir = new Vector(-1, 0.45, 0).normalize();
            scene.geometries.add(new Cylinder(
                new Ray(new Point(cx, aY1, cz), leftDir), 3.0, aL)
                .setEmission(cactusC).setMaterial(cactM));
            // Left arm tip upward
            Point leftTip = new Point(cx - aL * 0.91, aY1 + aL * 0.41, cz);
            scene.geometries.add(new Cylinder(
                new Ray(leftTip, Vector.AXIS_Y), 3.0, aL * 0.65)
                .setEmission(cactusC).setMaterial(cactM));
            // Right arm outward
            Vector rightDir = new Vector(1, 0.55, 0).normalize();
            scene.geometries.add(new Cylinder(
                new Ray(new Point(cx, aY2, cz), rightDir), 3.0, aL * 0.85)
                .setEmission(cactusC).setMaterial(cactM));
            // Right arm tip upward
            Point rightTip = new Point(cx + aL * 0.85 * 0.88, aY2 + aL * 0.85 * 0.47, cz);
            scene.geometries.add(new Cylinder(
                new Ray(rightTip, Vector.AXIS_Y), 3.0, aL * 0.55)
                .setEmission(cactusC).setMaterial(cactM));
        }

        // ── Road — flat asphalt strip in the immediate foreground ────────────
        Color roadC = new Color(48, 45, 40);
        Color lineC = new Color(238, 215, 48);
        double ry = BASE_Y + 0.3;
        scene.geometries.add(new Triangle(
            new Point(-170, ry, 262), new Point(170, ry, 262), new Point(170, ry, 310))
            .setEmission(roadC).setMaterial(roadM));
        scene.geometries.add(new Triangle(
            new Point(-170, ry, 262), new Point(170, ry, 310), new Point(-170, ry, 310))
            .setEmission(roadC).setMaterial(roadM));
        for (int d = 0; d < 3; d++) {
            double dz0 = 265 + d * 14, dz1 = dz0 + 8;
            scene.geometries.add(new Triangle(
                new Point(-5, ry + 0.1, dz0), new Point(5, ry + 0.1, dz0), new Point(5, ry + 0.1, dz1))
                .setEmission(lineC).setMaterial(roadM));
            scene.geometries.add(new Triangle(
                new Point(-5, ry + 0.1, dz0), new Point(5, ry + 0.1, dz1), new Point(-5, ry + 0.1, dz1))
                .setEmission(lineC).setMaterial(roadM));
        }

        // ── Lights (all four types) ───────────────────────────────────────────
        // 1. Ambient — set above.
        // 2. Directional sun: high angle, warm white-yellow.
        scene.lights.add(new DirectionalLight(
            new Color(255, 238, 185), new Vector(-0.3, -1.0, 0.2)));
        // 3. Point light simulating broad sky illumination.
        scene.lights.add(new PointLight(
            new Color(200, 185, 140), new Point(120, 420, -350))
            .setKl(0.000018).setKq(0.000000003).setSize(60));
        // 4. SpotLight highlighting the left rock cluster.
        scene.lights.add(new SpotLight(
            new Color(255, 195, 105), new Point(-380, 280, 80), new Vector(0.6, -1.0, -0.5))
            .setNarrowBeam(5).setKl(0.00025).setKq(0.0000010));
        // 5. Cool blue fill from the sky (bounce light).
        scene.lights.add(new SpotLight(
            new Color(105, 158, 205), new Point(255, 360, 200), new Vector(-0.4, -1.0, -0.5))
            .setNarrowBeam(3).setKl(0.00018).setKq(0.0000008).setSize(22));

        return scene;
    }

    // ========================= Camera =========================

    private static Camera.Builder buildCameraBuilder(Scene scene, SimpleRayTracer tracer) {
        return Camera.getBuilder()
                .setRayTracer(scene, tracer)
                .setLocation(new Point(0, 42, 310))
                .setDirection(new Point(-15, 5, -100), Vector.AXIS_Y)
                .setVpDistance(350)
                .setVpSize(490, 308)    // 16:10 landscape aspect
                .setResolution(600, 375)
                .setDebugPrint(5);
    }

    // ========================= Measurement helper =========================

    private static long runMeasurement(boolean useBVH, int threads, String label, String file) {
        Scene scene = buildScene();
        if (useBVH) scene.geometries.buildBVH();

        SimpleRayTracer tracer = new SimpleRayTracer(scene)
                .setSoftShadowSamples(TIMING_SS)
                .setSamplingPattern(Blackboard.SamplingPattern.JITTERED);

        Camera camera = buildCameraBuilder(scene, tracer)
                .setAntiAliasingSamples(TIMING_AA)
                .setMultithreading(threads)
                .build();

        System.out.println("=== " + label + " (BVH=" + useBVH + ", threads=" + threads + ") ===");
        long start = System.currentTimeMillis();
        camera.renderImage().writeToImage(file);
        long elapsed = System.currentTimeMillis() - start;
        System.out.printf("%s: %,d ms%n", label, elapsed);
        return elapsed;
    }

    // ========================= Mandatory configurations =========================

    /** Config 1 — baseline: no acceleration, single thread. */
    @Test
    void measurement_NoAcceleration_NoMultithreading() {
        runMeasurement(false, 0,
            "Config 1: Acceleration OFF, Multithreading OFF", "mp2_accelOFF_mtOFF");
    }

    /** Config 2 — no acceleration, multi-threading enabled. */
    @Test
    void measurement_NoAcceleration_WithMultithreading() {
        runMeasurement(false, -2,
            "Config 2: Acceleration OFF, Multithreading ON",  "mp2_accelOFF_mtON");
    }

    /** Config 3 — BVH enabled, single thread. */
    @Test
    void measurement_WithBVH_NoMultithreading() {
        runMeasurement(true, 0,
            "Config 3: Acceleration ON,  Multithreading OFF", "mp2_accelON_mtOFF");
    }

    /** Config 4 — BVH + multi-threading: fastest configuration. */
    @Test
    void measurement_WithBVH_WithMultithreading() {
        runMeasurement(true, -2,
            "Config 4: Acceleration ON,  Multithreading ON",  "mp2_accelON_mtON");
    }

    // ========================= Aggregate report =========================

    @Test
    void measurement_FullComparisonReport() {
        long b = runMeasurement(false, 0,  "Report 1/4: Accel OFF, MT OFF", "mp2_report_accelOFF_mtOFF");
        long m = runMeasurement(false, -2, "Report 2/4: Accel OFF, MT ON",  "mp2_report_accelOFF_mtON");
        long v = runMeasurement(true,  0,  "Report 3/4: Accel ON,  MT OFF", "mp2_report_accelON_mtOFF");
        long a = runMeasurement(true,  -2, "Report 4/4: Accel ON,  MT ON",  "mp2_report_accelON_mtON");

        System.out.println();
        System.out.println("==================== PERFORMANCE SUMMARY ====================");
        System.out.printf("Baseline (no accel, no MT):   %,9d ms  (x1.00)%n", b);
        System.out.printf("Multithreading only:          %,9d ms  (x%.2f)%n",  m, ratio(b, m));
        System.out.printf("BVH only:                     %,9d ms  (x%.2f)%n",  v, ratio(b, v));
        System.out.printf("BVH + Multithreading:         %,9d ms  (x%.2f)%n",  a, ratio(b, a));
        System.out.println("=============================================================");
    }

    private static double ratio(long base, long cur) {
        return cur == 0 ? Double.POSITIVE_INFINITY : (double) base / cur;
    }

    // ========================= Final presentation image =========================

    /**
     * Renders the submission's definitive image at full quality:
     * BVH + multi-threading + 9×9 AA + 9×9 soft shadows + 900×562 resolution.
     * Runs in minutes thanks to BVH; demonstrates the MP1 pipeline at its best.
     */
    @Test
    void renderFinalPresentationImage() {
        Scene scene = buildScene();
        scene.geometries.buildBVH();

        SimpleRayTracer tracer = new SimpleRayTracer(scene)
                .setSoftShadowSamples(FINAL_SS)
                .setSamplingPattern(Blackboard.SamplingPattern.JITTERED);

        Camera.getBuilder()
                .setRayTracer(scene, tracer)
                .setLocation(new Point(0, 32, 315))
                .setDirection(new Point(-45, -22, -100), Vector.AXIS_Y)
                .setVpDistance(350)
                .setVpSize(490, 308)
                .setResolution(750, 469)
                .setAntiAliasingSamples(FINAL_AA)
                .setMultithreading(-2)
                .setDebugPrint(1)
                .build()
                .renderImage()
                .writeToImage("mp2_final_presentation");
    }
}
