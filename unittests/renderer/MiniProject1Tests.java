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
 * Mini-Project 1 – Super-Sampling Image Improvements.
 *
 * <p>Demonstrates two super-sampling features on a rich Temple scene:</p>
 * <ol>
 *   <li><b>Anti-Aliasing</b> – smooths jagged edges on curved/diagonal surfaces.</li>
 *   <li><b>Soft Shadows</b> (bonus) – produces smooth shadow penumbras.</li>
 * </ol>
 */
class MiniProject1Tests {

    MiniProject1Tests() {}

    // ========================= Sample-count constants =========================

    /** Demo: 9x9 = 81 rays. */
    private static final int SAMPLES_DEMO = 9;

    /** AA-only final: 17x17 = 289 rays/pixel. Use only when SS is OFF. */
    private static final int AA_FINAL = 17;

    /** SS-only final: 9x9 = 81 shadow rays/light. */
    private static final int SS_FINAL = 9;

    /**
     * Combined AA+SS: 3x3 each.
     * Kept low because costs multiply: AA² × SS² × lights.
     */
    private static final int COMBINED_SAMPLES = 3;

    // ========================= Texture helpers =========================

    /** Base folder for texture images (relative to project root). */
    private static final String TEX_DIR = "images/textures/";

    /**
     * Loads a texture from the textures folder.
     * Returns {@code null} (no texture, use emission) if the file doesn't exist
     * — keeps tests runnable even before assets are downloaded.
     *
     * @param filename e.g. "earth.jpg"
     * @return a Texture, or null
     */
    private static Texture tex(String filename) {
        File f = new File(TEX_DIR + filename);

        // If file is missing or corrupt, generate a procedural fallback
        if (!isValidJpeg(f)) {
            System.out.println("[INFO] Generating procedural texture for: " + filename);
            generateProceduralTexture(f.getAbsolutePath(), filename);
        }

        try {
            return new Texture(f.getAbsolutePath());
        } catch (Exception e) {
            System.out.println("[WARN] Texture load failed (using emission fallback): " + e.getMessage());
            return null;
        }
    }

    /** Returns true if the file exists and starts with the JPEG magic bytes FF D8. */
    private static boolean isValidJpeg(File f) {
        if (!f.exists() || f.length() < 3) return false;
        try (var is = new java.io.FileInputStream(f)) {
            return is.read() == 0xFF && is.read() == 0xD8;
        } catch (Exception e) {
            return false;
        }
    }

    /** Generates a procedural texture image for known planet/scene names. */
    private static void generateProceduralTexture(String absPath, String filename) {
        String lower = filename.toLowerCase();
        if (lower.contains("galaxy") || lower.contains("sky") || lower.contains("space"))
            primitives.TextureGenerator.generateGalaxy(absPath);
        else if (lower.contains("saturn"))
            primitives.TextureGenerator.generateSaturn(absPath);
        else if (lower.contains("mars"))
            primitives.TextureGenerator.generateMars(absPath);
        else if (lower.contains("moon") || lower.contains("lunar"))
            primitives.TextureGenerator.generateMoon(absPath);
        else if (lower.contains("jupiter"))
            primitives.TextureGenerator.generateJupiter(absPath);
        else
            primitives.TextureGenerator.generateSaturn(absPath); // generic fallback
        System.out.println("[INFO] Procedural texture saved: " + absPath);
    }

    // ========================= Scene =========================

    private Scene buildSpaceScene() {
        Scene scene = new Scene("Solar System");

        scene.setBackground(new Color(0, 0, 0));
        // Near-zero ambient — only lit faces are bright; dark sides very dark.
        // emissionTexture surfaces (skybox) skip ambient entirely (renderer fix).
        scene.setAmbientLight(new AmbientLight(new Color(2, 2, 5), new Double3(1)));

        // ── Skybox — enormous sphere wrapping everything ───────────────────────
        // emissionTexture=true: no Phong, no ambient. Pure texture = true-black void.
        // kT=1: transparent for all shadow rays.
        Texture galaxyTex = tex("galaxy.jpg");
        scene.geometries.add(new Sphere(new Point(0, 0, -100), 2800D)
                .setEmission(Color.BLACK)
                .setMaterial(new Material().setKD(0).setKS(0).setShininess(1)
                        .setKT(1).setTexture(galaxyTex).setEmissionTexture()));

        // ── Sun — upper-left, z=+120 (in front of all planets) ────────────────
        // isLightSource() → shadow rays skip these spheres entirely.
        // glowFalloff  → limb-darkening: |N·V|^exp fades glow toward silhouette.
        // kT=1         → transparent even without the flag (belt-and-suspenders).
        final Point SUN_POS = new Point(-150, 200, 120);

        // Core: white-hot centre
        scene.geometries.add(new Sphere(SUN_POS, 100D)
                .setEmission(new Color(255, 248, 195))
                .setMaterial(new Material().setKD(0).setKS(0).setShininess(1)
                        .setKT(1).setGlowFalloff(0.22)).setLightSource());
        // Inner corona: bright yellow-orange
        scene.geometries.add(new Sphere(SUN_POS, 130D)
                .setEmission(new Color(255, 175, 30))
                .setMaterial(new Material().setKD(0).setKS(0).setShininess(1)
                        .setKT(1).setGlowFalloff(0.50)).setLightSource());
        // Mid corona: deep orange
        scene.geometries.add(new Sphere(SUN_POS, 165D)
                .setEmission(new Color(200, 68, 8))
                .setMaterial(new Material().setKD(0).setKS(0).setShininess(1)
                        .setKT(1).setGlowFalloff(0.95)).setLightSource());
        // Outer haze: faint dark red
        scene.geometries.add(new Sphere(SUN_POS, 205D)
                .setEmission(new Color(55, 14, 2))
                .setMaterial(new Material().setKD(0).setKS(0).setShininess(1)
                        .setKT(1).setGlowFalloff(1.55)).setLightSource());

        // ── Saturn — RIGHT side, large dominant subject ────────────────────────
        Texture saturnTex = tex("saturn.jpg");
        final Point SAT = new Point(120, 20, -250);
        final Vector RING_AXIS = new Vector(0.12, 1, 0.08).normalize();
        // Body
        scene.geometries.add(new Sphere(SAT, 110D)
                .setEmission(new Color(6, 5, 2))
                .setMaterial(new Material().setKD(0.72).setKS(0.38).setShininess(45)
                        .setTexture(saturnTex)));
        // Ring system — 5 layers mimicking real Saturn rings.
        // Emission is near-zero (rings don't glow); color comes from reflected sunlight
        // via a warm-gold tinted kD albedo.  kS is low — rings are dusty, not mirror-like.
        // C ring (innermost faint — dark brownish)
        scene.geometries.add(new Cylinder(
                new Ray(new Point(SAT.getX(), SAT.getY() - 9, SAT.getZ()), RING_AXIS), 128, 2)
                .setEmission(new Color(5, 4, 2))
                .setMaterial(new Material().setKD(new Double3(0.18, 0.14, 0.09))
                        .setKS(0.10).setShininess(6).setKT(0.72)));
        // B ring (brightest — sandy golden)
        scene.geometries.add(new Cylinder(
                new Ray(new Point(SAT.getX(), SAT.getY() - 10, SAT.getZ()), RING_AXIS), 152, 3)
                .setEmission(new Color(10, 9, 5))
                .setMaterial(new Material().setKD(new Double3(0.48, 0.42, 0.28))
                        .setKS(0.18).setShininess(14).setKT(0.04)));
        // Cassini division (dark gap between B and A)
        scene.geometries.add(new Cylinder(
                new Ray(new Point(SAT.getX(), SAT.getY() - 11, SAT.getZ()), RING_AXIS), 162, 2)
                .setEmission(new Color(3, 2, 1))
                .setMaterial(new Material().setKD(new Double3(0.06, 0.05, 0.03))
                        .setKS(0.05).setShininess(4).setKT(0.94)));
        // A ring (outer semi-transparent — medium warm tan)
        scene.geometries.add(new Cylinder(
                new Ray(new Point(SAT.getX(), SAT.getY() - 12, SAT.getZ()), RING_AXIS), 182, 2.5)
                .setEmission(new Color(7, 6, 4))
                .setMaterial(new Material().setKD(new Double3(0.38, 0.33, 0.22))
                        .setKS(0.15).setShininess(10).setKT(0.26)));
        // F ring (thin faint outermost — pale golden-cream)
        scene.geometries.add(new Cylinder(
                new Ray(new Point(SAT.getX(), SAT.getY() - 13, SAT.getZ()), RING_AXIS), 196, 1.5)
                .setEmission(new Color(10, 9, 7))
                .setMaterial(new Material().setKD(new Double3(0.55, 0.50, 0.38))
                        .setKS(0.20).setShininess(18).setKT(0.82)));

        // ── Jupiter — far right background, partially behind Saturn ───────────
        Texture jupiterTex = tex("jupiter.jpg");
        scene.geometries.add(new Sphere(new Point(370, 60, -620), 88D)
                .setEmission(new Color(10, 6, 3))
                .setMaterial(new Material().setKD(0.68).setKS(0.30).setShininess(28)
                        .setTexture(jupiterTex)));

        // ── Earth — center-left, medium distance ─────────────────────────────
        Texture earthTex = tex("earth.jpg");
        scene.geometries.add(new Sphere(new Point(-110, -30, -320), 75D)
                .setEmission(new Color(2, 5, 14))
                .setMaterial(new Material().setKD(0.65).setKS(0.55).setShininess(90).setKR(0.05)
                        .setTexture(earthTex)));

        // ── Moon — orbiting Earth ─────────────────────────────────────────────
        Texture moonTex = tex("moon.jpg");
        scene.geometries.add(new Sphere(new Point(-18, 52, -255), 18D)
                .setEmission(new Color(5, 5, 5))
                .setMaterial(new Material().setKD(0.78).setKS(0.12).setShininess(6)
                        .setTexture(moonTex)));

        // ── Mars — upper-right, medium distance ───────────────────────────────
        Texture marsTex = tex("mars.jpg");
        scene.geometries.add(new Sphere(new Point(268, 155, -440), 42D)
                .setEmission(new Color(7, 2, 1))
                .setMaterial(new Material().setKD(0.78).setKS(0.20).setShininess(15)
                        .setTexture(marsTex)));

        // ── Neptune-like ice giant — lower center ─────────────────────────────
        // Blue kD albedo: lit side reflects warm sunlight filtered through deep-blue
        // reflectance, producing vivid blue instead of gray when directly illuminated.
        scene.geometries.add(new Sphere(new Point(18, -155, -370), 30D)
                .setEmission(new Color(5, 18, 65))
                .setMaterial(new Material().setKD(new Double3(0.06, 0.20, 0.58))
                        .setKS(0.50).setShininess(110).setKR(0.10)));

        // ── Lights ────────────────────────────────────────────────────────────

        // Main sun light — at exact sun centre (isLightSource skips all sun spheres)
        scene.lights.add(new PointLight(new Color(1100, 960, 680), SUN_POS)
                .setKl(3E-7).setKq(2E-10).setSize(30));

        // Cool blue fill — very faint, simulates inter-stellar bounce
        scene.lights.add(new DirectionalLight(
                new Color(5, 8, 24),
                new Vector(-1, 0.2, -0.3)));

        return scene;
    }

    private Camera.Builder buildCameraBuilder(Scene scene, SimpleRayTracer rayTracer) {
        return Camera.getBuilder()
                .setRayTracer(scene, rayTracer)
                .setLocation(new Point(0, 0, 500))
                .setDirection(new Point(0, 0, -300), Vector.AXIS_Y)
                .setVpDistance(420)
                .setVpSize(380, 380)
                .setResolution(800, 800)
                .setMultithreading(-2)
                .setDebugPrint(5);
    }

    // ========================= Anti-Aliasing Tests =========================

    @Test
    void testTempleNoAntiAliasing() {
        Scene scene = buildSpaceScene();
        SimpleRayTracer rayTracer = new SimpleRayTracer(scene);

        System.out.println("=== Rendering WITHOUT Anti-Aliasing ===");
        long start = System.currentTimeMillis();

        buildCameraBuilder(scene, rayTracer)
                .setAntiAliasingSamples(1)
                .build()
                .renderImage()
                .writeToImage("space_no_AA");

        System.out.printf("Render time (no AA): %,d ms%n",
                System.currentTimeMillis() - start);
    }

    @Test
    void testTempleWithAntiAliasingDemo() {
        Scene scene = buildSpaceScene();
        SimpleRayTracer rayTracer = new SimpleRayTracer(scene);

        System.out.printf("=== Rendering WITH Anti-Aliasing: %dx%d = %d rays/pixel ===%n",
                SAMPLES_DEMO, SAMPLES_DEMO, SAMPLES_DEMO * SAMPLES_DEMO);
        long start = System.currentTimeMillis();

        buildCameraBuilder(scene, rayTracer)
                .setAntiAliasingSamples(SAMPLES_DEMO)
                .build()
                .renderImage()
                .writeToImage("space_with_AA_demo");

        System.out.printf("Render time (AA demo): %,d ms%n",
                System.currentTimeMillis() - start);
    }

    @Test
    void testTempleWithAntiAliasingFinal() {
        Scene scene = buildSpaceScene();
        SimpleRayTracer rayTracer = new SimpleRayTracer(scene);

        System.out.printf("=== Rendering WITH Anti-Aliasing: %dx%d = %d rays/pixel ===%n",
                AA_FINAL, AA_FINAL, AA_FINAL * AA_FINAL);
        long start = System.currentTimeMillis();

        buildCameraBuilder(scene, rayTracer)
                .setAntiAliasingSamples(AA_FINAL)
                .build()
                .renderImage()
                .writeToImage("space_with_AA_final");

        System.out.printf("Render time (AA final): %,d ms%n",
                System.currentTimeMillis() - start);
    }

    // ========================= Soft Shadows Tests =========================

    @Test
    void testTempleNoSoftShadows() {
        Scene scene = buildSpaceScene();
        SimpleRayTracer rayTracer = new SimpleRayTracer(scene)
                .setSoftShadowSamples(1);

        System.out.println("=== Rendering WITHOUT Soft Shadows ===");
        long start = System.currentTimeMillis();

        buildCameraBuilder(scene, rayTracer)
                .setAntiAliasingSamples(1)
                .build()
                .renderImage()
                .writeToImage("space_no_SS");

        System.out.printf("Render time (no SS): %,d ms%n",
                System.currentTimeMillis() - start);
    }

    @Test
    void testTempleWithSoftShadowsDemo() {
        Scene scene = buildSpaceScene();
        SimpleRayTracer rayTracer = new SimpleRayTracer(scene)
                .setSoftShadowSamples(SS_FINAL);

        System.out.printf("=== Rendering WITH Soft Shadows: %dx%d = %d rays/light ===%n",
                SS_FINAL, SS_FINAL, SS_FINAL * SS_FINAL);
        long start = System.currentTimeMillis();

        buildCameraBuilder(scene, rayTracer)
                .setAntiAliasingSamples(1)
                .build()
                .renderImage()
                .writeToImage("space_with_SS_demo");

        System.out.printf("Render time (SS demo): %,d ms%n",
                System.currentTimeMillis() - start);
    }

    // ========================= Combined Test (PRIMARY DELIVERABLE) =========================

    /**
     * PRIMARY DELIVERABLE.
     * Renders twice: OFF (no improvements) and ON (AA 3x3 + SS 3x3).
     */
    @Test
    void testTempleCombinedFinal() {

        // [1/2] Baseline — no improvements
        {
            Scene scene = buildSpaceScene();
            SimpleRayTracer rayTracer = new SimpleRayTracer(scene)
                    .setSoftShadowSamples(1);

            System.out.println("=== [1/2] Baseline – no improvements ===");
            long start = System.currentTimeMillis();

            buildCameraBuilder(scene, rayTracer)
                    .setAntiAliasingSamples(1)
                    .build()
                    .renderImage()
                    .writeToImage("space_combined_OFF");

            System.out.printf("Render time (OFF): %,d ms%n",
                    System.currentTimeMillis() - start);
        }

        // [2/2] Full quality — AA + Soft Shadows
        {
            Scene scene = buildSpaceScene();
            SimpleRayTracer rayTracer = new SimpleRayTracer(scene)
                    .setSoftShadowSamples(COMBINED_SAMPLES);

            System.out.printf("=== [2/2] Full quality – AA + Soft Shadows ===%n");
            System.out.printf("AA grid: %dx%d = %d rays/pixel%n",
                    COMBINED_SAMPLES, COMBINED_SAMPLES, COMBINED_SAMPLES * COMBINED_SAMPLES);
            System.out.printf("SS grid: %dx%d = %d shadow rays/light%n",
                    COMBINED_SAMPLES, COMBINED_SAMPLES, COMBINED_SAMPLES * COMBINED_SAMPLES);
            long start = System.currentTimeMillis();

            buildCameraBuilder(scene, rayTracer)
                    .setAntiAliasingSamples(COMBINED_SAMPLES)
                    .build()
                    .renderImage()
                    .writeToImage("space_combined_ON");

            System.out.printf("Render time (ON):  %,d ms%n",
                    System.currentTimeMillis() - start);
        }
    }
}