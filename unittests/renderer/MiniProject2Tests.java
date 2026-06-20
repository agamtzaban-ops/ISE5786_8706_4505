package renderer;

import geometries.impl.*;
import lighting.*;
import org.junit.jupiter.api.Test;
import primitives.*;
import scene.Scene;

/**
 * Mini-Project 2 — Performance Acceleration (Bounding Volume Hierarchy).
 *
 * <p>Scene: "Desert Sunset" — a dramatic low-poly canyon at golden hour.
 * A 28×28 tessellated terrain (1568 triangles) is the BVH stress-test.
 * Additional geometry: a large flat-top mesa (12 triangles), background peaks,
 * boulders (spheres), saguaro cacti (cylinders), light rays, and a sun sphere.</p>
 */
class MiniProject2Tests {

    MiniProject2Tests() {}

    // ========================= Constants =========================

    private static final int    GRID  = 28;
    private static final double TX0   = -260, TX1 = 260, TZ0 = -130, TZ1 = 280;
    private static final double BASE_Y = -62;
    private static final int    SKY_COLS = 10, SKY_ROWS = 14;

    private static final int TIMING_AA = 1;
    private static final int TIMING_SS = 1;
    private static final int FINAL_AA  = 9;
    private static final int FINAL_SS  = 3;

    // ========================= Helpers =========================

    private static double frac(double x) { return x - Math.floor(x); }

    /** Gentle desert dunes, range ≈ ±11 units. */
    private static double terrH(double x, double z) {
        return  5 * Math.sin(x * 0.022 + z * 0.015)
              + 3 * Math.cos(x * 0.038 - z * 0.028)
              + 2 * Math.sin(x * 0.055 + z * 0.047)
              + 1 * Math.cos(x * 0.081 - z * 0.072);
    }

    /**
     * Sunset desert palette — dark burnt-orange in valleys, warm gold on peaks.
     * Distant terrain fades to a warm haze matching the horizon sky.
     */
    private static Color terrC(double x, double z, double h) {
        double t    = Math.max(0, Math.min(1, (h + 11) / 22.0));
        double rf   = frac(x * 0.073 + z * 0.097 + x * z * 3e-5);
        double haze = Math.max(0, Math.min(1, (-z - 20) / 260.0));
        return new Color(
            Math.min(255, (int)(112 + t * 92 + rf * 22 + haze * 55)),
            Math.min(255, (int)( 58 + t * 58 + rf * 14 + haze * 40)),
            Math.min(255, (int)( 15 + t * 14 + rf *  6 + haze * 30)));
    }

    /**
     * 3-stop sunset sky gradient:
     * t=0 (horizon) → bright gold-orange → t=0.35: red-orange → t=0.62: magenta-purple → t=1: deep purple.
     */
    private static Color skyC(double t) {
        if (t < 0.32) {
            double s = t / 0.32;
            return new Color(
                (int)(252 * (1-s) + 228 * s),
                (int)(148 * (1-s) +  82 * s),
                (int)( 32 * (1-s) +  28 * s));
        } else if (t < 0.62) {
            double s = (t - 0.32) / 0.30;
            return new Color(
                (int)(228 * (1-s) + 128 * s),
                (int)( 82 * (1-s) +  42 * s),
                (int)( 28 * (1-s) +  88 * s));
        } else {
            double s = (t - 0.62) / 0.38;
            return new Color(
                (int)(128 * (1-s) + 48 * s),
                (int)( 42 * (1-s) + 20 * s),
                (int)( 88 * (1-s) + 98 * s));
        }
    }

    // ========================= Scene =========================

    private static Scene buildScene() {
        Scene scene = new Scene("Desert Sunset");
        scene.setBackground(new Color(52, 22, 68));
        scene.setAmbientLight(new AmbientLight(new Color(42, 28, 18), new Double3(1)));

        Material flat    = new Material().setKD(0.88).setKS(0.10).setShininess(8);
        Material skyM    = new Material().setKD(0.78).setKS(0.08).setShininess(5);
        Material rockM   = new Material().setKD(0.72).setKS(0.25).setShininess(30);
        Material cactM   = new Material().setKD(0.80).setKS(0.20).setShininess(22);
        Material boulderM = new Material().setKD(0.72).setKS(0.28).setShininess(25);

        // ── Backdrop plane (warm horizon glow; stays outside BVH) ─────────
        scene.geometries.add(new Plane(new Point(0, 0, -700), new Vector(0, 0, 1))
            .setEmission(new Color(212, 122, 35)).setMaterial(skyM));

        // ── Sun sphere (emissive; marked so shadow rays skip it) ──────────
        scene.geometries.add(new Sphere(new Point(188, 70, -545), 46)
            .setEmission(new Color(255, 232, 148))
            .setMaterial(new Material().setKD(0.05).setKS(0))
            .setLightSource());

        // ── Sky — 280 triangles, 3-stop sunset gradient ───────────────────
        final double SX0 = -700, SX1 = 700, SZ = -600;
        final double SY0 = -88,  SY1 = 560;
        double sdx = (SX1 - SX0) / SKY_COLS, sdy = (SY1 - SY0) / SKY_ROWS;
        for (int sr = 0; sr < SKY_ROWS; sr++) {
            double t0 = (double)sr / SKY_ROWS, t1 = (double)(sr+1) / SKY_ROWS;
            double y0 = SY0 + sr * sdy, y1 = y0 + sdy;
            for (int sc = 0; sc < SKY_COLS; sc++) {
                double x0 = SX0 + sc * sdx, x1 = x0 + sdx;
                scene.geometries.add(new Triangle(
                    new Point(x0,y0,SZ), new Point(x1,y0,SZ), new Point(x1,y1,SZ))
                    .setEmission(skyC(t0)).setMaterial(skyM));
                scene.geometries.add(new Triangle(
                    new Point(x0,y0,SZ), new Point(x1,y1,SZ), new Point(x0,y1,SZ))
                    .setEmission(skyC(t1)).setMaterial(skyM));
            }
        }

        // ── Sun light rays — 9 fan triangles at z=-590 (in front of sky) ─
        {
            double lrX = 188, lrY = 70, lrZ = -590;
            Point sunPt = new Point(lrX, lrY, lrZ);
            for (int r = 0; r < 9; r++) {
                double ang = Math.toRadians(-105 + r * 26);
                double len = 560, hw = 16;
                double dx = Math.cos(ang), dy = Math.sin(ang);
                double px = lrX + len*dx, py = lrY + len*dy;
                double bx = -Math.sin(ang)*hw, by = Math.cos(ang)*hw;
                Color rayC = (r % 2 == 0)
                    ? new Color(255, 200, 82) : new Color(198, 128, 38);
                scene.geometries.add(new Triangle(
                    sunPt,
                    new Point(px+bx, py+by, lrZ),
                    new Point(px-bx, py-by, lrZ))
                    .setEmission(rayC).setMaterial(skyM));
            }
        }

        // ── Terrain — 1568 triangles (BVH stress-test) ────────────────────
        double tdx = (TX1 - TX0) / GRID, tdz = (TZ1 - TZ0) / GRID;
        for (int gz = 0; gz < GRID; gz++) {
            for (int gx = 0; gx < GRID; gx++) {
                double x0 = TX0+gx*tdx, x1 = x0+tdx;
                double z0 = TZ0+gz*tdz, z1 = z0+tdz;
                double h00=terrH(x0,z0), h10=terrH(x1,z0), h01=terrH(x0,z1), h11=terrH(x1,z1);
                Point p00=new Point(x0,BASE_Y+h00,z0), p10=new Point(x1,BASE_Y+h10,z0);
                Point p01=new Point(x0,BASE_Y+h01,z1), p11=new Point(x1,BASE_Y+h11,z1);
                scene.geometries.add(new Triangle(p00,p10,p11)
                    .setEmission(terrC((x0+x1)/2, z0, (h00+h10+h11)/3.0)).setMaterial(flat));
                scene.geometries.add(new Triangle(p00,p11,p01)
                    .setEmission(terrC(x0, (z0+z1)/2, (h00+h01+h11)/3.0)).setMaterial(flat));
            }
        }

        // ── Large flat-top mesa (12 triangles, 3D faceted) ────────────────
        {
            double mzF = -182, mzB = -220, myB = -62, myT = 168;
            Point bFL = new Point(-400, myB, mzF);
            Point bFM = new Point(-295, myB, mzF);
            Point bFR = new Point(-168, myB, mzF);
            Point tFL = new Point(-378, myT,    mzF);
            Point tFM = new Point(-285, myT+14, mzF);
            Point tFR = new Point(-188, myT- 8, mzF);
            Point bBL = new Point(-400, myB, mzB);
            Point bBR = new Point(-168, myB, mzB);
            Point tBL = new Point(-378, myT,    mzB);
            Point tBM = new Point(-285, myT+14, mzB);
            Point tBR = new Point(-188, myT- 8, mzB);
            // Front face — 4 triangles with light→shadow gradient
            scene.geometries.add(new Triangle(bFL,bFM,tFL).setEmission(new Color( 98,42,15)).setMaterial(rockM));
            scene.geometries.add(new Triangle(bFM,tFM,tFL).setEmission(new Color(118,54,20)).setMaterial(rockM));
            scene.geometries.add(new Triangle(bFM,bFR,tFM).setEmission(new Color(168,82,32)).setMaterial(rockM));
            scene.geometries.add(new Triangle(bFR,tFR,tFM).setEmission(new Color(198,105,42)).setMaterial(rockM));
            // Top face — lit from above (warm golden)
            scene.geometries.add(new Triangle(tFL,tFM,tBL).setEmission(new Color(188,120,52)).setMaterial(rockM));
            scene.geometries.add(new Triangle(tFM,tBM,tBL).setEmission(new Color(195,128,58)).setMaterial(rockM));
            scene.geometries.add(new Triangle(tFM,tFR,tBR).setEmission(new Color(182,115,50)).setMaterial(rockM));
            scene.geometries.add(new Triangle(tFM,tBR,tBM).setEmission(new Color(188,120,52)).setMaterial(rockM));
            // Left side — deep shadow
            scene.geometries.add(new Triangle(bFL,tFL,bBL).setEmission(new Color(68,30,12)).setMaterial(rockM));
            scene.geometries.add(new Triangle(tFL,tBL,bBL).setEmission(new Color(75,34,13)).setMaterial(rockM));
            // Right side — partially lit by setting sun
            scene.geometries.add(new Triangle(bFR,bBR,tFR).setEmission(new Color(148,70,28)).setMaterial(rockM));
            scene.geometries.add(new Triangle(bBR,tBR,tFR).setEmission(new Color(138,65,25)).setMaterial(rockM));
        }

        // ── Background mountain peaks ─────────────────────────────────────
        double[][] peaks = {
            {  -82,-62,-195,  42,118,-218, 172,-62,-202,  88,42,18},
            {  140,-62,-188, 245,105,-205, 328,-62,-195,  95,48,20},
            { -195,-62,-205, -95, 72,-218,  -8,-62,-208,  78,38,16},
            {  295,-62,-182, 380, 88,-198, 452,-62,-188,  92,45,19},
            { -305,-62,-198,-228, 52,-212,-145,-62,-202,  72,35,14},
        };
        for (double[] p : peaks)
            scene.geometries.add(new Triangle(
                new Point(p[0],p[1],p[2]), new Point(p[3],p[4],p[5]), new Point(p[6],p[7],p[8]))
                .setEmission(new Color((int)p[9],(int)p[10],(int)p[11])).setMaterial(rockM));

        // ── Boulders — scattered spheres add visual interest ──────────────
        Color boulderC = new Color(118, 62, 22);
        double[][] boulderDef = {{-85,52,8.5},{125,25,6.5},{-35,118,11},{182,78,7.5}};
        for (double[] b : boulderDef) {
            double bx=b[0], bz=b[1], br=b[2];
            scene.geometries.add(new Sphere(
                new Point(bx, BASE_Y+terrH(bx,bz)+br-2.5, bz), br)
                .setEmission(boulderC).setMaterial(boulderM));
        }

        // ── Saguaro cacti — 6 × 5 cylinders = 30 geometries ─────────────
        Color cactusC = new Color(38, 110, 28);
        double[][] cactPos = {
            {-168,65},{-108,98},{52,40},{165,62},{-40,-12},{238,90}
        };
        for (double[] cp : cactPos) {
            double cx=cp[0], cz=cp[1];
            double cy  = BASE_Y + terrH(cx, cz);
            double tH  = 55 + frac(cx*0.13+cz*0.19)*30;
            double aL  = 18 + frac(cx*0.27+cz*0.31)*12;
            double aY1 = cy + tH*0.45, aY2 = cy + tH*0.38;
            scene.geometries.add(new Cylinder(
                new Ray(new Point(cx,cy,cz), Vector.AXIS_Y), 4.5, tH)
                .setEmission(cactusC).setMaterial(cactM));
            Vector leftDir = new Vector(-1, 0.45, 0).normalize();
            scene.geometries.add(new Cylinder(
                new Ray(new Point(cx,aY1,cz), leftDir), 3.0, aL)
                .setEmission(cactusC).setMaterial(cactM));
            Point leftTip = new Point(cx-aL*0.91, aY1+aL*0.41, cz);
            scene.geometries.add(new Cylinder(
                new Ray(leftTip, Vector.AXIS_Y), 3.0, aL*0.65)
                .setEmission(cactusC).setMaterial(cactM));
            Vector rightDir = new Vector(1, 0.55, 0).normalize();
            scene.geometries.add(new Cylinder(
                new Ray(new Point(cx,aY2,cz), rightDir), 3.0, aL*0.85)
                .setEmission(cactusC).setMaterial(cactM));
            Point rightTip = new Point(cx+aL*0.85*0.88, aY2+aL*0.85*0.47, cz);
            scene.geometries.add(new Cylinder(
                new Ray(rightTip, Vector.AXIS_Y), 3.0, aL*0.55)
                .setEmission(cactusC).setMaterial(cactM));
        }

        // ── Lights (all four types) ───────────────────────────────────────
        // 1. Ambient — set above.
        // 2. Directional: low-angle sunset sun sweeping from right.
        scene.lights.add(new DirectionalLight(
            new Color(255, 165, 55), new Vector(-0.8, -0.38, 0.4)));
        // 3. Point light at the sun sphere (warm, soft-shadow enabled).
        scene.lights.add(new PointLight(
            new Color(255, 198, 88), new Point(188, 70, -545))
            .setKl(0.000025).setKq(0.000000006).setSize(46));
        // 4. Cool purple fill from the sky zenith.
        scene.lights.add(new SpotLight(
            new Color(88, 52, 128), new Point(-100, 320, -100), new Vector(0.2, -1.0, -0.3))
            .setNarrowBeam(3).setKl(0.00018).setKq(0.0000010));
        // 5. Warm secondary bounce from sunlit terrain.
        scene.lights.add(new SpotLight(
            new Color(205, 98, 30), new Point(350, 180, 150), new Vector(-0.75, -0.85, -0.5))
            .setNarrowBeam(4).setKl(0.00015).setKq(0.0000008).setSize(20));

        return scene;
    }

    // ========================= Camera =========================

    private static Camera.Builder buildCameraBuilder(Scene scene, SimpleRayTracer tracer) {
        return Camera.getBuilder()
                .setRayTracer(scene, tracer)
                .setLocation(new Point(0, 42, 310))
                .setDirection(new Point(-15, 5, -100), Vector.AXIS_Y)
                .setVpDistance(350)
                .setVpSize(490, 308)
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

    @Test
    void renderFinalPresentationImage() {
        Scene scene = buildScene();
        scene.geometries.buildBVH();

        SimpleRayTracer tracer = new SimpleRayTracer(scene)
                .setSoftShadowSamples(FINAL_SS)
                .setSamplingPattern(Blackboard.SamplingPattern.JITTERED);

        Camera.getBuilder()
                .setRayTracer(scene, tracer)
                .setLocation(new Point(0, 42, 310))
                .setDirection(new Point(-15, 5, -100), Vector.AXIS_Y)
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
