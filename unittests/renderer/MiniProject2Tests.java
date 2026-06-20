package renderer;

import geometries.impl.*;
import lighting.*;
import org.junit.jupiter.api.Test;
import primitives.*;
import scene.Scene;

/**
 * Mini-Project 2 — Performance Acceleration (BVH).
 *
 * <p>Scene: "Desert Sunset" v2 — dramatic low-poly canyon at golden hour with:
 * multi-layer sun glow (KT halos + glowFalloff limb-darkening), two-layer
 * procedural mountain ridges, 2-segment curved cactus arms, 6-octave terrain.</p>
 */
class MiniProject2Tests {

    MiniProject2Tests() {}

    // ========================= Constants =========================

    private static final int    GRID   = 28;
    private static final double TX0    = -260, TX1 = 260, TZ0 = -130, TZ1 = 280;
    private static final double BASE_Y = -62;
    private static final int    SKY_COLS = 10, SKY_ROWS = 14;

    private static final int TIMING_AA = 1;
    private static final int TIMING_SS = 1;
    private static final int FINAL_AA  = 9;
    private static final int FINAL_SS  = 3;

    // Sun world position — shared by sphere, lights, and ray emitter
    private static final Point SUN_PT = new Point(188, 70, -545);

    // ========================= Helpers =========================

    private static double frac(double x) { return x - Math.floor(x); }

    /**
     * 6-octave height map: organic desert dunes, range approx +-15.2 units.
     * Two extra octaves (vs. v1) add small-scale surface ripples.
     */
    private static double terrH(double x, double z) {
        return  6.0 * Math.sin(x * 0.020 + z * 0.013)
              + 4.0 * Math.cos(x * 0.037 - z * 0.027)
              + 2.5 * Math.sin(x * 0.058 + z * 0.049)
              + 1.5 * Math.cos(x * 0.089 - z * 0.078)
              + 0.8 * Math.sin(x * 0.135 + z * 0.119)
              + 0.4 * Math.cos(x * 0.203 - z * 0.178);
    }

    /** Sunset desert palette: burnt-orange low, warm gold high, haze at distance. */
    private static Color terrC(double x, double z, double h) {
        double t    = Math.max(0, Math.min(1, (h + 15.2) / 30.4));
        double rf   = frac(x * 0.073 + z * 0.097 + x * z * 3e-5);
        double haze = Math.max(0, Math.min(1, (-z - 20) / 260.0));
        return new Color(
            Math.min(255, (int)(108 + t * 95 + rf * 25 + haze * 55)),
            Math.min(255, (int)( 52 + t * 62 + rf * 15 + haze * 40)),
            Math.min(255, (int)( 12 + t * 16 + rf *  8 + haze * 30)));
    }

    /** 3-stop sunset gradient: horizon gold-orange, mid red-orange, zenith deep purple. */
    private static Color skyC(double t) {
        if (t < 0.32) {
            double s = t / 0.32;
            return new Color(
                (int)(252*(1-s) + 228*s),
                (int)(148*(1-s) +  82*s),
                (int)( 32*(1-s) +  28*s));
        } else if (t < 0.62) {
            double s = (t - 0.32) / 0.30;
            return new Color(
                (int)(228*(1-s) + 128*s),
                (int)( 82*(1-s) +  42*s),
                (int)( 28*(1-s) +  88*s));
        } else {
            double s = (t - 0.62) / 0.38;
            return new Color(
                (int)(128*(1-s) + 48*s),
                (int)( 42*(1-s) + 20*s),
                (int)( 88*(1-s) + 98*s));
        }
    }

    /**
     * Procedural mountain ridge height at position x.
     *
     * Three-octave sum with a bell-shaped envelope that tapers to zero at
     * x = +-450. Negative values clamp to zero, naturally creating valleys
     * between peaks for a realistic jagged silhouette.
     *
     * @param x     world x coordinate along the ridge
     * @param seed  phase offset: different seeds produce different ridge shapes
     * @param scale maximum peak height before envelope attenuation
     */
    private static double ridgeH(double x, double seed, double scale) {
        double t   = (x + 450.0) / 900.0;
        double env = Math.max(0, 1.0 - Math.pow(2 * t - 1, 6));
        double n   = 0.55 * Math.sin(x * 0.025 + seed * 1.3)
                   + 0.30 * Math.sin(x * 0.055 + seed * 2.1 + 1.2)
                   + 0.15 * Math.sin(x * 0.110 + seed * 3.7 + 2.8);
        return scale * env * Math.max(0, n + 0.45);
    }

    // ========================= Scene =========================

    private static Scene buildScene() {
        Scene scene = new Scene("Desert Sunset");
        scene.setBackground(new Color(52, 22, 68));
        scene.setAmbientLight(new AmbientLight(new Color(42, 28, 18), new Double3(1)));

        Material flat     = new Material().setKD(0.88).setKS(0.10).setShininess(8);
        Material skyM     = new Material().setKD(0.78).setKS(0.08).setShininess(5);
        Material rockM    = new Material().setKD(0.72).setKS(0.25).setShininess(30);
        Material cactM    = new Material().setKD(0.80).setKS(0.20).setShininess(22);
        Material boulderM = new Material().setKD(0.72).setKS(0.28).setShininess(25);

        // ── Backdrop plane (warm horizon glow) ────────────────────────────
        scene.geometries.add(new Plane(new Point(0, 0, -700), new Vector(0, 0, 1))
            .setEmission(new Color(212, 122, 35)).setMaterial(skyM));

        // ── Sun — 4 concentric spheres producing a physical bloom / corona ─
        //
        // Each outer shell is transparent (KT) so camera rays pass inward.
        // glowFalloff creates limb-darkening: bright at centre (N·V=1),
        // fades to black at silhouette (N·V=0) so the glow blends smoothly
        // into the surrounding sky instead of showing a hard sphere edge.
        //
        //  Layer         radius   emission             glowFalloff   KT
        //  Outer corona    125   (230,  80, 12)           1.10      0.985
        //  Mid glow         90   (255, 130, 30)           0.65      0.940
        //  Inner halo       60   (255, 195, 65)           0.35      0.850
        //  Core             36   (255, 252, 210)          0.18        0   opaque
        scene.geometries.add(new Sphere(SUN_PT, 125)
            .setEmission(new Color(230, 80, 12))
            .setMaterial(new Material().setKD(0).setKS(0).setShininess(0)
                .setKT(0.985).setGlowFalloff(1.10))
            .setLightSource());
        scene.geometries.add(new Sphere(SUN_PT, 90)
            .setEmission(new Color(255, 130, 30))
            .setMaterial(new Material().setKD(0).setKS(0).setShininess(0)
                .setKT(0.940).setGlowFalloff(0.65))
            .setLightSource());
        scene.geometries.add(new Sphere(SUN_PT, 60)
            .setEmission(new Color(255, 195, 65))
            .setMaterial(new Material().setKD(0).setKS(0).setShininess(0)
                .setKT(0.850).setGlowFalloff(0.35))
            .setLightSource());
        scene.geometries.add(new Sphere(SUN_PT, 36)
            .setEmission(new Color(255, 252, 210))
            .setMaterial(new Material().setKD(0).setKS(0).setShininess(0)
                .setGlowFalloff(0.18))
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

        // ── Sun light rays — 9 fan triangles at z=-590 ───────────────────
        {
            final double lrX = 188, lrY = 70, lrZ = -590;
            Point origin = new Point(lrX, lrY, lrZ);
            for (int r = 0; r < 9; r++) {
                double ang = Math.toRadians(-105 + r * 26);
                double len = 580, hw = 14;
                double dx = Math.cos(ang), dy = Math.sin(ang);
                double px = lrX + len*dx, py = lrY + len*dy;
                double bx = -Math.sin(ang)*hw, by = Math.cos(ang)*hw;
                Color rayC = (r%2==0) ? new Color(255,210,85) : new Color(205,135,42);
                scene.geometries.add(new Triangle(
                    origin, new Point(px+bx,py+by,lrZ), new Point(px-bx,py-by,lrZ))
                    .setEmission(rayC).setMaterial(skyM));
            }
        }

        // ── Terrain — 1568 triangles (BVH stress-test) ────────────────────
        double tdx = (TX1-TX0)/GRID, tdz = (TZ1-TZ0)/GRID;
        for (int gz = 0; gz < GRID; gz++) {
            for (int gx = 0; gx < GRID; gx++) {
                double x0=TX0+gx*tdx, x1=x0+tdx, z0=TZ0+gz*tdz, z1=z0+tdz;
                double h00=terrH(x0,z0), h10=terrH(x1,z0),
                       h01=terrH(x0,z1), h11=terrH(x1,z1);
                Point p00=new Point(x0,BASE_Y+h00,z0), p10=new Point(x1,BASE_Y+h10,z0);
                Point p01=new Point(x0,BASE_Y+h01,z1), p11=new Point(x1,BASE_Y+h11,z1);
                scene.geometries.add(new Triangle(p00,p10,p11)
                    .setEmission(terrC((x0+x1)/2,z0,(h00+h10+h11)/3.0)).setMaterial(flat));
                scene.geometries.add(new Triangle(p00,p11,p01)
                    .setEmission(terrC(x0,(z0+z1)/2,(h00+h01+h11)/3.0)).setMaterial(flat));
            }
        }

        // ── Large flat-top mesa (left side, 12 faceted triangles) ─────────
        {
            final double mzF=-182, mzB=-220, myB=-62, myT=168;
            Point bFL=new Point(-400,myB,mzF), bFM=new Point(-295,myB,mzF), bFR=new Point(-168,myB,mzF);
            Point tFL=new Point(-378,myT,   mzF), tFM=new Point(-285,myT+14,mzF), tFR=new Point(-188,myT-8,mzF);
            Point bBL=new Point(-400,myB,mzB),                                    bBR=new Point(-168,myB,mzB);
            Point tBL=new Point(-378,myT,   mzB), tBM=new Point(-285,myT+14,mzB), tBR=new Point(-188,myT-8,mzB);
            // Front face: left=deep shadow, right=sun-lit
            scene.geometries.add(new Triangle(bFL,bFM,tFL).setEmission(new Color( 98,42,15)).setMaterial(rockM));
            scene.geometries.add(new Triangle(bFM,tFM,tFL).setEmission(new Color(118,54,20)).setMaterial(rockM));
            scene.geometries.add(new Triangle(bFM,bFR,tFM).setEmission(new Color(168,82,32)).setMaterial(rockM));
            scene.geometries.add(new Triangle(bFR,tFR,tFM).setEmission(new Color(198,105,42)).setMaterial(rockM));
            // Top face: lit warm gold
            scene.geometries.add(new Triangle(tFL,tFM,tBL).setEmission(new Color(188,120,52)).setMaterial(rockM));
            scene.geometries.add(new Triangle(tFM,tBM,tBL).setEmission(new Color(195,128,58)).setMaterial(rockM));
            scene.geometries.add(new Triangle(tFM,tFR,tBR).setEmission(new Color(182,115,50)).setMaterial(rockM));
            scene.geometries.add(new Triangle(tFM,tBR,tBM).setEmission(new Color(188,120,52)).setMaterial(rockM));
            // Left side: deep shadow
            scene.geometries.add(new Triangle(bFL,tFL,bBL).setEmission(new Color(68,30,12)).setMaterial(rockM));
            scene.geometries.add(new Triangle(tFL,tBL,bBL).setEmission(new Color(75,34,13)).setMaterial(rockM));
            // Right side: partial sunset light
            scene.geometries.add(new Triangle(bFR,bBR,tFR).setEmission(new Color(148,70,28)).setMaterial(rockM));
            scene.geometries.add(new Triangle(bBR,tBR,tFR).setEmission(new Color(138,65,25)).setMaterial(rockM));
        }

        // ── Far mountain ridge (z=-212, hazy dark silhouette) ────────────
        // ridgeH(seed=0.8) gives a different peak pattern than seed=1.0,
        // so the two ridges have naturally offset mountain positions.
        {
            final double mzFar = -212, mbase = BASE_Y;
            final int N = 20;
            final double startX = -450, step = 900.0 / (N-1);
            double[] ht = new double[N];
            for (int i = 0; i < N; i++) ht[i] = ridgeH(startX + i*step, 0.8, 115);
            for (int i = 0; i < N-1; i++) {
                double x0 = startX + i*step, x1 = x0 + step;
                double h0 = ht[i], h1 = ht[i+1];
                if (h0 < 1 && h1 < 1) continue;
                double litF = Math.max(0, Math.min(1, (x0 + 225) / 450.0));
                Color c = new Color(
                    (int)(52 + litF*28), (int)(22 + litF*14), (int)(12 + litF*8));
                // Guard each triangle: skip if the peak vertex coincides with a base vertex
                if (h1 > 1.0)
                    scene.geometries.add(new Triangle(
                        new Point(x0,mbase,mzFar), new Point(x1,mbase,mzFar),
                        new Point(x1,mbase+h1,mzFar)).setEmission(c).setMaterial(rockM));
                if (h0 > 1.0)
                    scene.geometries.add(new Triangle(
                        new Point(x0,mbase,mzFar), new Point(x1,mbase+h1,mzFar),
                        new Point(x0,mbase+h0,mzFar)).setEmission(c).setMaterial(rockM));
            }
        }

        // ── Main mountain ridge (z=-190, complex jagged ridgeline) ─────────
        {
            final double mzMain = -190, mbase = BASE_Y;
            final int N = 22;
            final double startX = -440, step = 880.0 / (N-1);
            double[] ht = new double[N];
            for (int i = 0; i < N; i++) ht[i] = ridgeH(startX + i*step, 1.0, 155);
            for (int i = 0; i < N-1; i++) {
                double x0 = startX + i*step, x1 = x0 + step;
                double h0 = ht[i], h1 = ht[i+1];
                if (h0 < 1 && h1 < 1) continue;
                double avgH = (h0 + h1) / 2.0;
                double litF = Math.max(0, Math.min(1, (x0 + 222) / 444.0));
                Color c = new Color(
                    (int)(68 + litF*50 + avgH*0.06),
                    (int)(30 + litF*26 + avgH*0.03),
                    (int)(14 + litF*12 + avgH*0.01));
                if (h1 > 1.0)
                    scene.geometries.add(new Triangle(
                        new Point(x0,mbase,mzMain), new Point(x1,mbase,mzMain),
                        new Point(x1,mbase+h1,mzMain)).setEmission(c).setMaterial(rockM));
                if (h0 > 1.0)
                    scene.geometries.add(new Triangle(
                        new Point(x0,mbase,mzMain), new Point(x1,mbase+h1,mzMain),
                        new Point(x0,mbase+h0,mzMain)).setEmission(c).setMaterial(rockM));
            }
        }

        // ── Boulders — 4 spheres scattered on terrain ─────────────────────
        Color boulderC = new Color(118, 62, 22);
        double[][] boulderDef = {{-85,52,8.5},{125,25,6.5},{-35,118,11},{182,78,7.5}};
        for (double[] b : boulderDef) {
            double bx=b[0], bz=b[1], br=b[2];
            scene.geometries.add(new Sphere(
                new Point(bx, BASE_Y+terrH(bx,bz)+br-2.5, bz), br)
                .setEmission(boulderC).setMaterial(boulderM));
        }

        // ── Saguaro cacti — 6 x (trunk + 2x2-segment curved arms) ────────
        //
        // Each arm has two cylinder segments forming an elbow:
        //   shoulder: mostly horizontal (~24 deg upward)
        //   forearm:  mostly vertical (~65 deg upward)
        // This produces the classic cactus S-curve shape vs. the 90-degree
        // right-angle of a single-segment arm.
        Color cactusC = new Color(38, 110, 28);
        double[][] cactPos = {
            {-168,65},{-108,98},{52,40},{165,62},{-40,-12},{238,90}
        };
        for (double[] cp : cactPos) {
            final double cx=cp[0], cz=cp[1];
            final double cy = BASE_Y + terrH(cx, cz);
            final double tH = 55 + frac(cx*0.13+cz*0.19)*30;

            // Trunk
            scene.geometries.add(new Cylinder(
                new Ray(new Point(cx, cy, cz), Vector.AXIS_Y), 4.5, tH)
                .setEmission(cactusC).setMaterial(cactM));

            // Left arm
            {
                final double laY  = cy + tH * 0.50;
                final double laL1 = 14 + frac(cx*0.31 + cz*0.19) * 8;  // shoulder
                final double laL2 = 13 + frac(cx*0.19 + cz*0.27) * 7;  // forearm
                // Shoulder dir normalized: (-0.90, 0.42, 0) -> (-0.9060, 0.4229, 0)
                final double la1x = -0.9060, la1y = 0.4229;
                final double elX = cx + la1x*laL1, elY = laY + la1y*laL1;
                scene.geometries.add(new Cylinder(
                    new Ray(new Point(cx, laY, cz), new Vector(-0.90, 0.42, 0).normalize()),
                    2.8, laL1).setEmission(cactusC).setMaterial(cactM));
                // Forearm dir normalized: (-0.40, 0.92, 0) -> mostly vertical
                scene.geometries.add(new Cylinder(
                    new Ray(new Point(elX, elY, cz), new Vector(-0.40, 0.92, 0).normalize()),
                    2.6, laL2).setEmission(cactusC).setMaterial(cactM));
            }

            // Right arm
            {
                final double raY  = cy + tH * 0.42;
                final double raL1 = 12 + frac(cx*0.27 + cz*0.35) * 8;
                final double raL2 = 11 + frac(cx*0.35 + cz*0.21) * 6;
                // Shoulder dir normalized: (0.88, 0.47, 0) -> (0.8829, 0.4694, 0)
                final double ra1x = 0.8829, ra1y = 0.4694;
                final double elX = cx + ra1x*raL1, elY = raY + ra1y*raL1;
                scene.geometries.add(new Cylinder(
                    new Ray(new Point(cx, raY, cz), new Vector(0.88, 0.47, 0).normalize()),
                    2.8, raL1).setEmission(cactusC).setMaterial(cactM));
                // Forearm dir: (0.38, 0.93, 0) -> mostly vertical
                scene.geometries.add(new Cylinder(
                    new Ray(new Point(elX, elY, cz), new Vector(0.38, 0.93, 0).normalize()),
                    2.6, raL2).setEmission(cactusC).setMaterial(cactM));
            }
        }

        // ── Lights ────────────────────────────────────────────────────────
        // 1. Ambient: set above.
        // 2. DirectionalLight: low-angle sunset sweeping from the right.
        scene.lights.add(new DirectionalLight(
            new Color(255, 165, 55), new Vector(-0.8, -0.38, 0.4)));
        // 3. PointLight at the sun sphere, warm with soft-shadow radius.
        scene.lights.add(new PointLight(
            new Color(255, 198, 88), SUN_PT)
            .setKl(0.000025).setKq(0.000000006).setSize(46));
        // 4. Cool purple fill from sky zenith.
        scene.lights.add(new SpotLight(
            new Color(88, 52, 128), new Point(-100, 320, -100), new Vector(0.2, -1.0, -0.3))
            .setNarrowBeam(3).setKl(0.00018).setKq(0.0000010));
        // 5. Warm bounce from sunlit ground.
        scene.lights.add(new SpotLight(
            new Color(205, 98, 30), new Point(350, 180, 150), new Vector(-0.75, -0.85, -0.5))
            .setNarrowBeam(4).setKl(0.00015).setKq(0.0000008).setSize(20));

        return scene;
    }

    // ========================= Camera builder =========================

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

    // ========================= Mandatory test configurations =========================

    /** Config 1 - baseline: no acceleration, single thread. */
    @Test
    void measurement_NoAcceleration_NoMultithreading() {
        runMeasurement(false, 0,
            "Config 1: Acceleration OFF, Multithreading OFF", "mp2_accelOFF_mtOFF");
    }

    /** Config 2 - no acceleration, multi-threading enabled. */
    @Test
    void measurement_NoAcceleration_WithMultithreading() {
        runMeasurement(false, -2,
            "Config 2: Acceleration OFF, Multithreading ON",  "mp2_accelOFF_mtON");
    }

    /** Config 3 - BVH enabled, single thread. */
    @Test
    void measurement_WithBVH_NoMultithreading() {
        runMeasurement(true, 0,
            "Config 3: Acceleration ON,  Multithreading OFF", "mp2_accelON_mtOFF");
    }

    /** Config 4 - BVH + multi-threading: the fastest configuration. */
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
