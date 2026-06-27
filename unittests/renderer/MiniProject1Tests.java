package renderer;

import geometries.impl.*;
import lighting.AmbientLight;
import lighting.DirectionalLight;
import lighting.PointLight;
import lighting.SpotLight;
import org.junit.jupiter.api.Test;
import primitives.*;
import scene.Scene;

import java.io.File;

/**
 * Mini-Project 1 — Super-Sampling Image Improvements.
 *
 * <p>Demonstrates two super-sampling features on a Solar System scene:</p>
 * <ol>
 * <li><b>Anti-Aliasing (AA)</b> — fires multiple rays per pixel in a grid
 * pattern and averages the colors, smoothing jagged edges on curved surfaces.</li>
 * <li><b>Soft Shadows (SS)</b> — fires multiple shadow rays toward different
 * points on the light's area disk, producing smooth shadow penumbras.</li>
 * </ol>
 *
 * <p><b>Scene description:</b> The Solar System poster layout — the Sun fills
 * the bottom of the frame and all planets are arranged in a mathematical arc above it,
 * ordered by distance from the Sun. All planets use real texture maps loaded from
 * {@code images/textures/}; procedural fallbacks are generated automatically if a
 * texture file is missing.</p>
 */
class MiniProject1Tests {

    MiniProject1Tests() {
    }

    // ========================= Sample-count constants =========================

    /**
     * Demo-quality sample grid: 9x9 = 81 rays. Visible improvement, fast render.
     */
    private static final int SAMPLES_DEMO = 9;
    /**
     * Final AA-only sample grid: 17x17 = 289 rays.
     */
    private static final int AA_FINAL = 17;
    /**
     * Final SS-only sample grid: 9x9 = 81 shadow rays per light.
     */
    private static final int SS_FINAL = 9;
    /**
     * Combined AA+SS sample grid: 3x3 = 9 rays each.
     */
    private static final int COMBINED_SAMPLES = 3;

    // ========================= Texture helpers =========================

    private static final String TEX_DIR = "images/textures/";

    /**
     * Loads a texture from the textures folder by filename.
     * Generates a procedural fallback if the file is missing or invalid.
     */
    private static Texture tex(String filename) {
        File f = new File(TEX_DIR + filename);
        if (!isValidJpeg(f)) generateProceduralTexture(f.getAbsolutePath(), filename);
        try {
            return new Texture(f.getAbsolutePath());
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isValidJpeg(File f) {
        if (!f.exists() || f.length() < 3) return false;
        try (var is = new java.io.FileInputStream(f)) {
            return is.read() == 0xFF && is.read() == 0xD8;
        } catch (Exception e) {
            return false;
        }
    }

    private static void generateProceduralTexture(String absPath, String filename) {
        String lower = filename.toLowerCase();
        if (lower.contains("galaxy") || lower.contains("space"))
            primitives.TextureGenerator.generateGalaxy(absPath);
        else if (lower.contains("sun"))
            primitives.TextureGenerator.generateSun(absPath);
        else if (lower.contains("mercury"))
            primitives.TextureGenerator.generateMercury(absPath);
        else if (lower.contains("venus"))
            primitives.TextureGenerator.generateVenus(absPath);
        else if (lower.contains("saturn"))
            primitives.TextureGenerator.generateSaturn(absPath);
        else if (lower.contains("mars"))
            primitives.TextureGenerator.generateMars(absPath);
        else if (lower.contains("moon"))
            primitives.TextureGenerator.generateMoon(absPath);
        else if (lower.contains("jupiter"))
            primitives.TextureGenerator.generateJupiter(absPath);
        else if (lower.contains("uranus"))
            primitives.TextureGenerator.generateUranus(absPath);
        else if (lower.contains("neptune"))
            primitives.TextureGenerator.generateNeptune(absPath);
        else
            primitives.TextureGenerator.generateSaturn(absPath);
    }

    // ========================= Math Helpers =========================

    /**
     * Calculates a position along a mathematical arc around a given center point.
     * This is used to arrange the planets in a perfect curve around the Sun.
     *
     * @param center       The center point (e.g., the Sun's center)
     * @param radius       The distance from the center
     * @param angleDegrees The angle in degrees
     * @param zDepth       The Z coordinate to place the object at
     * @return A calculated Point on the arc
     */
    private Point getArcPoint(Point center, double radius, double angleDegrees, double zDepth) {
        double radRad = Math.toRadians(angleDegrees);
        double x = center.getX() + radius * Math.cos(radRad);
        double y = center.getY() + radius * Math.sin(radRad);
        return new Point(x, y, zDepth);
    }

    // ========================= Scene Setup =========================

    /**
     * Builds the Solar System scene used by all render tests.
     */
    private Scene buildSpaceScene() {
        Scene scene = new Scene("Solar System");
        scene.setBackground(new Color(0, 0, 0));

        // Ambient light kept near zero for a dark, cinematic space atmosphere
        scene.setAmbientLight(new AmbientLight(new Color(2, 2, 2), new Double3(1)));

        // ── Skybox ────────────────────────────────────────────────────────────
        Texture galaxyTex = tex("galaxy.jpg");
        scene.geometries.add(new Sphere(new Point(0, 0, -100), 2800D)
                .setEmission(Color.BLACK)
                .setMaterial(new Material().setKD(0).setKS(0).setShininess(1)
                        .setKT(1).setTexture(galaxyTex).setEmissionTexture()));

        // ── The Sun ───────────────────────────────────────────────────────────
        final Point SUN_POS = new Point(0, -700, -350);

        Texture sunTex = tex("sun.jpg");
        scene.geometries.add(new Sphere(SUN_POS, 600D)
                .setEmission(Color.BLACK)
                .setMaterial(new Material().setKD(0.5).setKS(0.1).setShininess(10)
                        .setKT(0)
                        .setTexture(sunTex)
                        .setEmissionTexture())
                .setLightSource());

        // ── Planets arranged in a mathematical arc around the Sun ─────────────
        final double ORBIT_RADIUS = 820D;

        // Mercury (Angle: 115)
        Point pMercury = getArcPoint(SUN_POS, ORBIT_RADIUS, 115, -340);
        Texture mercuryTex = tex("mercury.jpg");
        scene.geometries.add(new Sphere(pMercury, 12D)
                .setEmission(new Color(90, 80, 75))
                .setMaterial(new Material().setKD(0.60).setKS(0.15).setShininess(10)
                        .setTexture(mercuryTex)));

        // Venus (Angle: 108)
        Point pVenus = getArcPoint(SUN_POS, ORBIT_RADIUS, 108, -350);
        Texture venusTex = tex("venus.jpg");
        scene.geometries.add(new Sphere(pVenus, 18D)
                .setEmission(new Color(180, 150, 80))
                .setMaterial(new Material().setKD(0.55).setKS(0.20).setShininess(20)
                        .setTexture(venusTex)));

        // Earth (Angle: 101)
        Point pEarth = getArcPoint(SUN_POS, ORBIT_RADIUS, 101, -360);
        Texture earthTex = tex("earth.jpg");
        scene.geometries.add(new Sphere(pEarth, 22D)
                .setEmission(new Color(10, 40, 120))
                .setMaterial(new Material().setKD(new Double3(0.10, 0.35, 0.80))
                        .setKS(0.60).setShininess(150).setKR(0.05)
                        .setTexture(earthTex)));

        // Moon (orbiting Earth)
        Point pMoon = new Point(pEarth.getX() + 32, pEarth.getY() + 18, pEarth.getZ() + 20);
        Texture moonTex = tex("moon.jpg");
        scene.geometries.add(new Sphere(pMoon, 7D)
                .setEmission(new Color(55, 55, 55))
                .setMaterial(new Material().setKD(0.70).setKS(0.10).setShininess(5)
                        .setTexture(moonTex)));

        // Mars (Angle: 95)
        Point pMars = getArcPoint(SUN_POS, ORBIT_RADIUS, 95, -370);
        Texture marsTex = tex("mars.jpg");
        scene.geometries.add(new Sphere(pMars, 17D)
                .setEmission(new Color(140, 50, 20))
                .setMaterial(new Material().setKD(new Double3(0.70, 0.25, 0.10))
                        .setKS(0.20).setShininess(15).setTexture(marsTex)));

        // Jupiter (Angle: 87)
        Point pJupiter = getArcPoint(SUN_POS, ORBIT_RADIUS, 87, -380);
        Texture jupiterTex = tex("jupiter.jpg");
        scene.geometries.add(new Sphere(pJupiter, 55D)
                .setEmission(new Color(90, 55, 25))
                .setMaterial(new Material().setKD(0.68).setKS(0.30).setShininess(28)
                        .setTexture(jupiterTex)));

        // Saturn (Angle: 78)
        Point pSaturn = getArcPoint(SUN_POS, ORBIT_RADIUS, 78, -390);
        Texture saturnTex = tex("saturn.jpg");
        final Vector RING_AXIS = new Vector(0.05, 0.866, 0.5).normalize();
        scene.geometries.add(new Sphere(pSaturn, 42D)
                .setEmission(new Color(6, 5, 2))
                .setMaterial(new Material().setKD(0.72).setKS(0.38).setShininess(45)
                        .setTexture(saturnTex)));

        // Saturn's rings (4 concentric cylinders)
        scene.geometries.add(new Cylinder(
                new Ray(new Point(pSaturn.getX(), pSaturn.getY() - 4, pSaturn.getZ()), RING_AXIS), 52, 1)
                .setEmission(new Color(5, 4, 2))
                .setMaterial(new Material().setKD(new Double3(0.18, 0.14, 0.09)).setKS(0.10).setShininess(6).setKT(0.72)));
        scene.geometries.add(new Cylinder(
                new Ray(new Point(pSaturn.getX(), pSaturn.getY() - 5, pSaturn.getZ()), RING_AXIS), 62, 2)
                .setEmission(new Color(10, 9, 5))
                .setMaterial(new Material().setKD(new Double3(0.48, 0.42, 0.28)).setKS(0.18).setShininess(14).setKT(0.04)));
        scene.geometries.add(new Cylinder(
                new Ray(new Point(pSaturn.getX(), pSaturn.getY() - 6, pSaturn.getZ()), RING_AXIS), 68, 1.5)
                .setEmission(new Color(7, 6, 4))
                .setMaterial(new Material().setKD(new Double3(0.38, 0.33, 0.22)).setKS(0.15).setShininess(10).setKT(0.26)));
        scene.geometries.add(new Cylinder(
                new Ray(new Point(pSaturn.getX(), pSaturn.getY() - 7, pSaturn.getZ()), RING_AXIS), 74, 1)
                .setEmission(new Color(10, 9, 7))
                .setMaterial(new Material().setKD(new Double3(0.55, 0.50, 0.38)).setKS(0.20).setShininess(18).setKT(0.82)));

        // Uranus (Angle: 71)
        Point pUranus = getArcPoint(SUN_POS, ORBIT_RADIUS, 71, -380);
        Texture uranusTex = tex("uranus.jpg");
        scene.geometries.add(new Sphere(pUranus, 28D)
                .setEmission(new Color(10, 60, 80))
                .setMaterial(new Material().setKD(new Double3(0.10, 0.55, 0.65))
                        .setKS(0.45).setShininess(80).setKR(0.08)
                        .setTexture(uranusTex)));

        // Neptune (Angle: 65)
        Point pNeptune = getArcPoint(SUN_POS, ORBIT_RADIUS, 65, -370);
        Texture neptuneTex = tex("neptune.jpg");
        scene.geometries.add(new Sphere(pNeptune, 25D)
                .setEmission(new Color(8, 25, 120))
                .setMaterial(new Material().setKD(new Double3(0.06, 0.20, 0.75))
                        .setKS(0.55).setShininess(120).setKR(0.10)
                        .setTexture(neptuneTex)));

        // ── Lights ────────────────────────────────────────────────────────────

        // 1. Strong sun point light — casts dramatic shadows across all planets
        scene.lights.add(new PointLight(new Color(180, 148, 95), SUN_POS)
                .setKl(2E-7).setKq(1E-10).setSize(120));

        // 2. Weak fill light — preserves shadow sides, satisfies the 3-light requirement
        scene.lights.add(new DirectionalLight(
                new Color(5, 5, 8), new Vector(1, -0.3, -0.5)));

        // 3. Subtle rim light from behind — adds delicate highlights to planet edges
        scene.lights.add(new SpotLight(new Color(15, 20, 40), new Point(400, 100, 0), new Vector(-1, -0.2, -0.8))
                .setKl(1E-5).setKq(1E-7));

        return scene;
    }

    /**
     * Standard resolution for comparison renders (fast).
     */
    private static final int STD_RES = 800;
    /**
     * High resolution for the final render — sufficient to zoom in on the Moon.
     */
    private static final int HIGH_RES = 8000;

    /**
     * Camera builder at standard resolution for quick comparison tests.
     */
    private Camera.Builder buildCameraBuilder(Scene scene, SimpleRayTracer rayTracer) {
        return Camera.getBuilder()
                .setRayTracer(scene, rayTracer)
                .setLocation(new Point(0, 0, 500))
                .setDirection(new Point(0, 0, -300), Vector.AXIS_Y)
                .setVpDistance(420)
                .setVpSize(380, 380)
                .setResolution(STD_RES, STD_RES)
                .setMultithreading(-2)
                .setDebugPrint(5);
    }

    /**
     * Camera builder at high resolution for the final quality render.
     */
    private Camera.Builder buildHiResCameraBuilder(Scene scene, SimpleRayTracer rayTracer) {
        return Camera.getBuilder()
                .setRayTracer(scene, rayTracer)
                .setLocation(new Point(0, 0, 500))
                .setDirection(new Point(0, 0, -300), Vector.AXIS_Y)
                .setVpDistance(420)
                .setVpSize(380, 380)
                .setResolution(HIGH_RES, HIGH_RES)
                .setMultithreading(-2)
                .setDebugPrint(1);
    }

    // ========================= Anti-Aliasing Tests =========================

    @Test
    void testTempleNoAntiAliasing() {
        Scene scene = buildSpaceScene();
        SimpleRayTracer rayTracer = new SimpleRayTracer(scene);
        long start = System.currentTimeMillis();
        buildCameraBuilder(scene, rayTracer)
                .setAntiAliasingSamples(1).build().renderImage().writeToImage("space_no_AA");
        System.out.printf("Render time (no AA): %,d ms%n", System.currentTimeMillis() - start);
    }

    @Test
    void testTempleWithAntiAliasingDemo() {
        Scene scene = buildSpaceScene();
        SimpleRayTracer rayTracer = new SimpleRayTracer(scene);
        long start = System.currentTimeMillis();
        buildCameraBuilder(scene, rayTracer)
                .setAntiAliasingSamples(SAMPLES_DEMO).build().renderImage()
                .writeToImage("space_with_AA_demo");
        System.out.printf("Render time (AA demo %dx%d): %,d ms%n",
                SAMPLES_DEMO, SAMPLES_DEMO, System.currentTimeMillis() - start);
    }

    @Test
    void testTempleWithAntiAliasingFinal() {
        Scene scene = buildSpaceScene();
        SimpleRayTracer rayTracer = new SimpleRayTracer(scene);
        long start = System.currentTimeMillis();
        // High-resolution render: zoom into the Moon area to verify AA smoothness
        buildHiResCameraBuilder(scene, rayTracer)
                .setAntiAliasingSamples(AA_FINAL).build().renderImage()
                .writeToImage("space_with_AA_final");
        System.out.printf("Render time (AA final %dx%d @ %dx%d): %,d ms%n",
                AA_FINAL, AA_FINAL, HIGH_RES, HIGH_RES, System.currentTimeMillis() - start);
    }

    // ========================= Soft Shadows Tests =========================

    @Test
    void testTempleNoSoftShadows() {
        Scene scene = buildSpaceScene();
        SimpleRayTracer rayTracer = new SimpleRayTracer(scene).setSoftShadowSamples(1);
        long start = System.currentTimeMillis();
        buildCameraBuilder(scene, rayTracer)
                .setAntiAliasingSamples(1).build().renderImage().writeToImage("space_no_SS");
        System.out.printf("Render time (no SS): %,d ms%n", System.currentTimeMillis() - start);
    }

    @Test
    void testTempleWithSoftShadowsDemo() {
        Scene scene = buildSpaceScene();
        SimpleRayTracer rayTracer = new SimpleRayTracer(scene).setSoftShadowSamples(SS_FINAL);
        long start = System.currentTimeMillis();
        buildCameraBuilder(scene, rayTracer)
                .setAntiAliasingSamples(1).build().renderImage()
                .writeToImage("space_with_SS_demo");
        System.out.printf("Render time (SS demo %dx%d): %,d ms%n",
                SS_FINAL, SS_FINAL, System.currentTimeMillis() - start);
    }

    // ========================= Jittered Sampling Tests (Bonus) =========================

    /**
     * BONUS — Jittered vs Grid sampling pattern comparison.
     */
    @Test
    void testTempleJitteredVsGrid() {
        // [1/2] Grid pattern
        {
            Scene scene = buildSpaceScene();
            SimpleRayTracer rayTracer = new SimpleRayTracer(scene)
                    .setSoftShadowSamples(SS_FINAL)
                    .setSamplingPattern(Blackboard.SamplingPattern.GRID);
            System.out.println("=== [1/2] Soft Shadows with GRID pattern ===");
            long start = System.currentTimeMillis();
            buildCameraBuilder(scene, rayTracer)
                    .setAntiAliasingSamples(1).build().renderImage()
                    .writeToImage("space_SS_grid");
            System.out.printf("Render time (GRID): %,d ms%n", System.currentTimeMillis() - start);
        }

        // [2/2] Jittered pattern
        {
            Scene scene = buildSpaceScene();
            SimpleRayTracer rayTracer = new SimpleRayTracer(scene)
                    .setSoftShadowSamples(SS_FINAL)
                    .setSamplingPattern(Blackboard.SamplingPattern.JITTERED);
            System.out.println("=== [2/2] Soft Shadows with JITTERED pattern ===");
            long start = System.currentTimeMillis();
            buildCameraBuilder(scene, rayTracer)
                    .setAntiAliasingSamples(1).build().renderImage()
                    .writeToImage("space_SS_jittered");
            System.out.printf("Render time (JITTERED): %,d ms%n", System.currentTimeMillis() - start);
        }
    }

    // ========================= Combined Test =========================

    /**
     * PRIMARY DELIVERABLE — Combined Anti-Aliasing and Soft Shadows.
     * The "ON" variant renders at high resolution (3840x3840) so the Moon
     * can be examined in full detail by zooming in with an image viewer.
     */
    @Test
    void testTempleCombinedFinal() {
        // [1/2] Baseline — no improvements, standard resolution
        {
            Scene scene = buildSpaceScene();
            SimpleRayTracer rayTracer = new SimpleRayTracer(scene).setSoftShadowSamples(1);
            System.out.println("=== [1/2] Baseline — no improvements ===");
            long start = System.currentTimeMillis();
            buildCameraBuilder(scene, rayTracer)
                    .setAntiAliasingSamples(1).build().renderImage()
                    .writeToImage("space_combined_OFF");
            System.out.printf("Render time (OFF): %,d ms%n", System.currentTimeMillis() - start);
        }

        // [2/2] Full quality — AA + Soft Shadows at high resolution (3840x3840)
        {
            Scene scene = buildSpaceScene();
            SimpleRayTracer rayTracer = new SimpleRayTracer(scene)
                    .setSoftShadowSamples(COMBINED_SAMPLES);
            System.out.printf("=== [2/2] Full quality — AA %dx%d + SS %dx%d @ %dx%d ===%n",
                    COMBINED_SAMPLES, COMBINED_SAMPLES,
                    COMBINED_SAMPLES, COMBINED_SAMPLES,
                    HIGH_RES, HIGH_RES);
            long start = System.currentTimeMillis();
            buildHiResCameraBuilder(scene, rayTracer)
                    .setAntiAliasingSamples(COMBINED_SAMPLES).build().renderImage()
                    .writeToImage("space_combined_ON");
            System.out.printf("Render time (ON): %,d ms%n", System.currentTimeMillis() - start);
        }
    }
}