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
    private static final int FINAL_SS  = 9;

    // ========================= Helpers =========================

    private static double frac(double x) { return x - Math.floor(x); }

    /** Terrain height-map: sum of sine waves, range ≈ ±18 units. */
    private static double terrH(double x, double z) {
        return  8 * Math.sin(x * 0.022 + z * 0.015)
              + 5 * Math.cos(x * 0.038 - z * 0.028)
              + 3 * Math.sin(x * 0.055 + z * 0.047)
              + 2 * Math.cos(x * 0.081 - z * 0.072);
    }

    /** Terrain color: warm earthy oranges-reds, brighter on hilltops. */
    private static Color terrC(double x, double z, double h) {
        double t  = Math.max(0, Math.min(1, (h + 18) / 36.0));
        double rf = frac(x * 0.073 + z * 0.097 + x * z * 3e-5);
        return new Color(
            Math.min(255, (int)(75  + t * 95 + rf * 25)),
            Math.min(255, (int)(30  + t * 40 + rf * 10)),
            Math.min(255, (int)( 5  + t * 13 + rf *  5)));
    }

    /** Sky gradient: t=0 → warm orange at horizon; t=1 → cool purple at zenith. */
    private static Color skyC(double t) {
        return new Color(
            (int)(215 * (1 - t) + 16 * t),
            (int)( 85 * (1 - t) +  6 * t),
            (int)( 12 * (1 - t) + 55 * t));
    }

    // ========================= Scene =========================

    private static Scene buildScene() {
        Scene scene = new Scene("Savanna Sunset");
        scene.setBackground(new Color(16, 7, 38));
        scene.setAmbientLight(new AmbientLight(new Color(52, 26, 10), new Double3(1)));

        Material flat  = new Material().setKD(0.92).setKS(0.05).setShininess(4);
        Material skyM  = new Material().setKD(0.85).setKS(0.10).setShininess(8);
        Material dark  = new Material().setKD(0.85).setKS(0.05).setShininess(3);
        Material trunk = new Material().setKD(0.70).setKS(0.12).setShininess(12);

        // ── Backdrop plane (infinite → stays outside BVH) ─────────────────
        scene.geometries.add(
            new Plane(new Point(0, 0, -700), new Vector(0, 0, 1))
                .setEmission(new Color(10, 5, 28)).setMaterial(skyM));

        // ── Sun — glowing emissive sphere; marked as light source so
        //    shadow rays from the terrain do not hit it ─────────────────────
        scene.geometries.add(
            new Sphere(new Point(-185, 6, -400), 92)
                .setEmission(new Color(255, 162, 40))
                .setMaterial(new Material().setKD(0.1).setKS(0))
                .setLightSource());

        // ── Sky — 8×6 grid of triangles forming a sunset gradient ─────────
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

        // ── Mountains — dark silhouette triangles in the background ─────────
        Color mtnC = new Color(26, 12, 5);
        double[][] mts = {
            {-360, -62, -185,  -90, 112, -218,  170, -62, -168},
            { -80, -62, -192,   65,  98, -222,  290, -62, -172},
            { 210, -62, -168,  390,  62, -198,  520, -62, -162},
            {-520, -62, -172, -310,  58, -198, -140, -62, -182},
        };
        for (double[] m : mts)
            scene.geometries.add(new Triangle(
                new Point(m[0], m[1], m[2]),
                new Point(m[3], m[4], m[5]),
                new Point(m[6], m[7], m[8]))
                .setEmission(mtnC).setMaterial(dark));

        // ── Acacia trees: 6 trees × (1 cylinder trunk + 4 canopy triangles) ─
        Color trunkC  = new Color(40, 19, 7);
        Color canopyC = new Color(16, 36, 9);
        double[][] treeXZ = {
            {-152, 78}, {-218, 22}, { 128, 48},
            { 185, -8}, { -58,-22}, { 235, 95}
        };
        for (double[] tp : treeXZ) {
            double tx = tp[0], tz = tp[1];
            double ty  = BASE_Y + terrH(tx, tz);
            double tH  = 50 + frac(tx * 0.17 + tz * 0.23) * 25;
            double cR  = 30 + frac(tx * 0.31 + tz * 0.41) * 18;
            double top = ty + tH;
            scene.geometries.add(
                new Cylinder(new Ray(new Point(tx, ty, tz), Vector.AXIS_Y), 3.5, tH)
                    .setEmission(trunkC).setMaterial(trunk));
            double[] ang = {0, Math.PI / 2, Math.PI, 3 * Math.PI / 2};
            for (int i = 0; i < 4; i++) {
                double a1 = ang[i], a2 = ang[(i + 1) % 4];
                Point apex = new Point(tx, top,     tz);
                Point e1   = new Point(tx + cR * Math.cos(a1), top - 10, tz + cR * Math.sin(a1));
                Point e2   = new Point(tx + cR * Math.cos(a2), top - 10, tz + cR * Math.sin(a2));
                scene.geometries.add(new Triangle(apex, e1, e2)
                    .setEmission(canopyC).setMaterial(dark));
            }
        }

        // ── Reflective water pool — flat mirror in the foreground ────────────
        Material water = new Material().setKD(0.15).setKS(0.85).setShininess(120).setKR(0.65);
        Color waterE   = new Color(8, 18, 32);
        double wy = BASE_Y + 0.5;                    // just above ground level
        scene.geometries.add(new Triangle(
            new Point(-110, wy,  90), new Point( 110, wy,  90), new Point( 110, wy, 235))
            .setEmission(waterE).setMaterial(water));
        scene.geometries.add(new Triangle(
            new Point(-110, wy,  90), new Point( 110, wy, 235), new Point(-110, wy, 235))
            .setEmission(waterE).setMaterial(water));

        // ── Lights (all four types represented) ──────────────────────────────
        // 1. Ambient — set above.
        // 2. Directional sunlight raking in from the left.
        scene.lights.add(new DirectionalLight(
            new Color(255, 138, 48), new Vector(1.1, -0.22, 0.45)));
        // 3. Point light at the sun sphere (warm glow with soft-shadow size).
        scene.lights.add(new PointLight(
            new Color(255, 188, 68), new Point(-185, 6, -400))
            .setKl(0.00005).setKq(0.00000009).setSize(48));
        // 4. Cool purple-blue fill from the zenith (sky bounce).
        scene.lights.add(new SpotLight(
            new Color(68, 42, 128), new Point(360, 290, -160), new Vector(-0.8, -1, -0.3))
            .setNarrowBeam(4).setKl(0.0002).setKq(0.0000012));
        // 5. Warm orange accent spot highlighting foreground terrain.
        scene.lights.add(new SpotLight(
            new Color(195, 98, 28), new Point(-130, 230, 200), new Vector(0.5, -1, -0.42))
            .setNarrowBeam(3).setKl(0.00014).setKq(0.0000008).setSize(22));

        return scene;
    }

    // ========================= Camera =========================

    private static Camera.Builder buildCameraBuilder(Scene scene, SimpleRayTracer tracer) {
        return Camera.getBuilder()
                .setRayTracer(scene, tracer)
                .setLocation(new Point(0, 32, 315))
                .setDirection(new Point(-45, -22, -100), Vector.AXIS_Y)
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
                .setResolution(900, 562)
                .setAntiAliasingSamples(FINAL_AA)
                .setMultithreading(-2)
                .setDebugPrint(1)
                .build()
                .renderImage()
                .writeToImage("mp2_final_presentation");
    }
}
