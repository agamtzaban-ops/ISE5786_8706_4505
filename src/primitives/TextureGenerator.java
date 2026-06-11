package primitives;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

/**
 * TextureGenerator — procedurally generates planet-surface textures.
 *
 * <p>Used when real photographic textures are unavailable.
 * Produces a JPG file that {@link Texture} can load normally.</p>
 */
public class TextureGenerator {

    // ── Saturn ────────────────────────────────────────────────────────────────

    /**
     * Generates a photorealistic Saturn-like banded texture.
     *
     * <p>Features high-contrast alternating cream/ivory and dark-caramel bands
     * with subtle storm wisps, slight polar darkening, and fine-grain detail.
     * Resolution: 2048×1024.</p>
     *
     * @param outputPath absolute path for the output JPG
     */
    public static void generateSaturn(String outputPath) {
        int W = 2048, H = 1024;
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);

        // Saturn band palette — REALISTIC: subtle differences between light/dark bands.
        // Real Saturn looks mostly golden-cream; dark bands are only ~20-25% darker.
        // Each entry: {R, G, B}
        int[][] bands = {
            {242, 220, 158},  //  0  warm gold (main belt)
            {192, 162,  95},  //  1  medium caramel
            {250, 232, 172},  //  2  pale cream
            {178, 148,  82},  //  3  warm tan
            {245, 225, 162},  //  4  golden cream
            {185, 155,  88},  //  5  caramel
            {252, 236, 178},  //  6  light ivory
            {172, 142,  76},  //  7  brown-tan
            {248, 228, 165},  //  8  ivory-gold
            {182, 152,  85},  //  9  warm brown
            {255, 238, 182},  // 10  very pale cream
            {175, 145,  78},  // 11  caramel-brown
            {246, 222, 158},  // 12  gold
            {188, 158,  90},  // 13  medium tan
        };

        for (int y = 0; y < H; y++) {
            double ny = y / (double) H;     // 0..1 top→bottom

            // Polar darkening — poles are slightly cooler/darker (more brownish)
            double polarFade = 1.0 - 0.22 * Math.pow(Math.abs(ny - 0.5) * 2, 2.0);

            // Band lookup with variable-width wobble (irregular Saturn-like bands)
            // Using a non-uniform sine mix so bands have varying widths
            double bandCount = bands.length * 3.2;
            double t = ny * bandCount
                     + 0.10 * Math.sin(ny * 14.0)    // broad primary wave
                     + 0.055 * Math.sin(ny * 31.0)   // secondary ripple
                     + 0.025 * Math.sin(ny * 67.0);  // fine detail

            int b0  = (int) t % bands.length;
            int b1  = (b0 + 1) % bands.length;
            double frac = t - Math.floor(t);
            // Smooth-step with wide blending zone (bands fade into each other gently)
            frac = frac * frac * (3.0 - 2.0 * frac);
            // Second smooth-step pass for extra softness
            frac = frac * frac * (3.0 - 2.0 * frac);

            int baseR = (int) (bands[b0][0] + frac * (bands[b1][0] - bands[b0][0]));
            int baseG = (int) (bands[b0][1] + frac * (bands[b1][1] - bands[b0][1]));
            int baseB = (int) (bands[b0][2] + frac * (bands[b1][2] - bands[b0][2]));

            // Apply polar fade
            baseR = (int)(baseR * polarFade);
            baseG = (int)(baseG * polarFade * 0.97);
            baseB = (int)(baseB * polarFade * 0.94);

            for (int x = 0; x < W; x++) {
                double nx = x / (double) W;

                // Very subtle longitudinal turbulence (fine texture grain)
                double turb = 3.5 * Math.sin(x * 0.041 + y * 0.029)
                            + 2.5 * Math.sin(x * 0.087 - y * 0.053)
                            + 1.5 * Math.sin(x * 0.019 + y * 0.11)
                            + 1.0 * Math.sin(x * 0.157 - y * 0.079);

                // Subtle storm wisps (narrow latitude bands)
                double stormA = 0.0, stormB = 0.0;
                double dA = Math.abs(ny - 0.37) * 5.0;
                double dB = Math.abs(ny - 0.63) * 5.0;
                if (dA < 1.0) stormA = (1.0 - dA) * 8.0 * Math.max(0, Math.sin((nx - 0.22) * 11.0));
                if (dB < 1.0) stormB = (1.0 - dB) * 7.0 * Math.max(0, Math.sin((nx - 0.68) * 9.0));

                int r = clamp(baseR + (int)(turb + stormA + stormB));
                int g = clamp(baseG + (int)(turb * 0.82 + stormA * 0.85 + stormB * 0.80));
                int b = clamp(baseB + (int)(turb * 0.58 + stormA * 0.62 + stormB * 0.58));

                img.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
        save(img, outputPath);
    }

    // ── Mars ─────────────────────────────────────────────────────────────────

    /**
     * Generates a Mars-like rusty red texture with polar ice caps and
     * varied terrain (dark basalt lowlands, bright highland dust).
     *
     * @param outputPath absolute path for the output JPG
     */
    public static void generateMars(String outputPath) {
        int W = 2048, H = 1024;
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < H; y++) {
            double ny = y / (double) H;  // 0=top, 1=bottom

            // Polar ice caps (white at both poles)
            double polarIce = Math.max(0, Math.pow(Math.abs(ny - 0.5) * 2, 6.0) - 0.6);
            polarIce = Math.min(1.0, polarIce * 3.5);

            for (int x = 0; x < W; x++) {
                // Terrain noise (multi-octave)
                double n =  16.0 * Math.sin(x * 0.021 + y * 0.038)
                          + 10.0 * Math.sin(x * 0.053 - y * 0.027)
                          +  6.0 * Math.sin(x * 0.089 + y * 0.071)
                          +  3.5 * Math.sin(x * 0.13  - y * 0.10)
                          +  2.0 * Math.sin(x * 0.21  + y * 0.17)
                          +  1.0 * Math.sin(x * 0.37  - y * 0.29);

                // Base rust red, modulated by terrain height
                int r = clamp(148 + (int)(n * 1.0));
                int g = clamp( 55 + (int)(n * 0.45));
                int b = clamp( 22 + (int)(n * 0.2));

                // Dark volcanic lowlands (Valles Marineris-like)
                double lowland = 0.5 + 0.5 * Math.sin(x * 0.008 + 1.2) * Math.sin(y * 0.025);
                if (lowland < 0.3) {
                    r = clamp(r - 22);
                    g = clamp(g - 10);
                    b = clamp(b - 5);
                }

                // Polar ice overlay
                r = clamp((int)(r * (1 - polarIce) + 230 * polarIce));
                g = clamp((int)(g * (1 - polarIce) + 235 * polarIce));
                b = clamp((int)(b * (1 - polarIce) + 245 * polarIce));

                img.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
        save(img, outputPath);
    }

    // ── Moon ─────────────────────────────────────────────────────────────────

    /**
     * Generates a lunar surface texture with craters, highlands (light gray),
     * and maria (dark basalt plains).
     *
     * @param outputPath absolute path for the output JPG
     */
    public static void generateMoon(String outputPath) {
        int W = 2048, H = 1024;
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);

        // Base terrain: medium-light gray with coarse noise (visible at small scales)
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                double n = 14.0 * Math.sin(x * 0.037 + y * 0.054)
                         +  9.0 * Math.sin(x * 0.071 - y * 0.043)
                         +  6.0 * Math.sin(x * 0.11  + y * 0.083)
                         +  4.0 * Math.sin(x * 0.17  - y * 0.13)
                         +  2.5 * Math.sin(x * 0.27  + y * 0.21);
                int v = clamp(152 + (int) n);
                // Slight warm tint (lunar regolith is brownish-gray)
                img.setRGB(x, y, (clamp(v + 3) << 16) | (v << 8) | clamp(v - 6));
            }
        }

        // Mare (dark basalt) regions — large irregular dark patches (visible from far)
        Random rng = new Random(0xCA55E77E);
        int numMare = 5;
        for (int m = 0; m < numMare; m++) {
            int mx = rng.nextInt(W);
            int my = rng.nextInt(H / 2) + H / 4;
            int mr = 80 + rng.nextInt(120);   // larger for visibility at small render size
            for (int dy = -mr; dy <= mr; dy++) {
                for (int dx = (int)(-mr * 1.6); dx <= (int)(mr * 1.6); dx++) {
                    double dist = Math.sqrt((double) dx * dx / 2.56 + (double) dy * dy) / mr;
                    if (dist > 1.0) continue;
                    int px = (mx + dx + W) % W;
                    int py = my + dy;
                    if (py < 0 || py >= H) continue;
                    double fade = Math.max(0, 1.0 - dist * dist);
                    int old = img.getRGB(px, py);
                    int ov = (old >> 8) & 0xFF;
                    int nv = clamp((int)(ov * (1 - 0.44 * fade)));
                    img.setRGB(px, py, (clamp(nv + 2) << 16) | (nv << 8) | clamp(nv - 5));
                }
            }
        }

        // Craters — large ones very visible; dark interior, bright raised rim, ejecta blanket
        int numCraters = 160;
        for (int c = 0; c < numCraters; c++) {
            int cx = rng.nextInt(W);
            int cy = rng.nextInt(H);
            // Size distribution: many small, a few large (log-scale)
            double sizeRng = rng.nextDouble();
            int cr = (int)(3 + sizeRng * sizeRng * 45);
            double rimBright = 0.38 + 0.22 * rng.nextDouble();
            double floorDark = 0.46 + 0.16 * rng.nextDouble();
            for (int dy = -(cr + 3); dy <= cr + 3; dy++) {
                for (int dx = -(cr + 3); dx <= cr + 3; dx++) {
                    double dist = Math.sqrt(dx * dx + dy * dy);
                    double r = dist / cr;
                    if (r > 1.45) continue;
                    int px = (cx + dx + W) % W;
                    int py = cy + dy;
                    if (py < 0 || py >= H) continue;
                    int old = img.getRGB(px, py);
                    int ov = (old >> 8) & 0xFF;
                    int nv;
                    if (r <= 0.80) {
                        // Dark crater floor
                        nv = clamp((int)(ov * floorDark));
                    } else if (r <= 1.05) {
                        // Bright raised rim — most visible feature
                        double t = (r - 0.80) / 0.25;
                        t = t * t * (3 - 2 * t);
                        nv = clamp((int)(ov * (floorDark + t * (1.0 + rimBright - floorDark))));
                    } else {
                        // Ejecta blanket fading outward
                        double t = (r - 1.05) / 0.40;
                        t = Math.min(1.0, t);
                        t = t * t * (3 - 2 * t);
                        nv = clamp((int)(ov * (1.0 + rimBright * 0.5 * (1.0 - t))));
                    }
                    img.setRGB(px, py, (clamp(nv + 3) << 16) | (nv << 8) | clamp(nv - 6));
                }
            }
        }
        save(img, outputPath);
    }

    // ── Jupiter ───────────────────────────────────────────────────────────────

    /**
     * Generates a Jupiter-like banded texture with a Great Red Spot analog.
     * Warm orange/cream/tan bands with prominent dark belt contrasts.
     *
     * @param outputPath absolute path for the output JPG
     */
    public static void generateJupiter(String outputPath) {
        int W = 2048, H = 1024;
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);

        // Jupiter band palette: orange/cream/tan alternating
        int[][] bands = {
            {225, 190, 140},  //  0  pale tan
            {178,  98,  48},  //  1  dark burnt orange
            {240, 215, 168},  //  2  cream
            {152,  78,  32},  //  3  deep orange-brown
            {235, 202, 152},  //  4  warm tan
            {195, 125,  68},  //  5  medium orange
            {248, 224, 178},  //  6  light cream
            {162,  88,  38},  //  7  dark orange
            {230, 195, 142},  //  8  tan
            {188, 118,  58},  //  9  orange-tan
        };

        for (int y = 0; y < H; y++) {
            double ny = y / (double) H;
            // Polar blue-gray tint
            double polarBlue = Math.pow(Math.abs(ny - 0.5) * 2, 3.5) * 0.35;

            double bandCount = bands.length * 3.0;
            double t = ny * bandCount
                     + 0.06 * Math.sin(ny * 22.0)
                     + 0.04 * Math.sin(ny * 55.0);
            int b0  = (int) t % bands.length;
            int b1  = (b0 + 1) % bands.length;
            double frac = t - Math.floor(t);
            frac = frac * frac * (3.0 - 2.0 * frac);

            int baseR = (int)(bands[b0][0] + frac * (bands[b1][0] - bands[b0][0]));
            int baseG = (int)(bands[b0][1] + frac * (bands[b1][1] - bands[b0][1]));
            int baseB = (int)(bands[b0][2] + frac * (bands[b1][2] - bands[b0][2]));

            // Apply polar cooling
            baseR = clamp((int)(baseR * (1 - polarBlue * 0.3)));
            baseG = clamp((int)(baseG * (1 - polarBlue * 0.15)));
            baseB = clamp((int)(baseB + polarBlue * 40));

            for (int x = 0; x < W; x++) {
                double nx = x / (double) W;
                double turb = 6.0 * Math.sin(x * 0.035 + y * 0.025)
                            + 4.0 * Math.sin(x * 0.073 - y * 0.047)
                            + 2.5 * Math.sin(x * 0.12  + y * 0.095)
                            + 1.5 * Math.sin(x * 0.21  - y * 0.16);

                // Great Red Spot — large oval anticyclone
                double grsX = 0.38, grsY = 0.60;
                double dx = (nx - grsX) / 0.16;
                double dy = (ny - grsY) / 0.07;
                double grs = Math.exp(-(dx*dx + dy*dy));
                int grsR = (int)(220 * grs);  // deep red
                int grsG = (int)( 55 * grs);
                int grsB = (int)( 30 * grs);

                int r = clamp(baseR + (int) turb + grsR);
                int g = clamp(baseG + (int)(turb * 0.75) + grsG);
                int b = clamp(baseB + (int)(turb * 0.55) + grsB);
                img.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
        save(img, outputPath);
    }

    // ── Galaxy / Skybox ────────────────────────────────────────────────────────

    /**
     * Generates a deep-space galaxy / Milky Way skybox texture.
     *
     * <p>Features:</p>
     * <ul>
     *   <li>True-black void between stars (base = 0,0,0)</li>
     *   <li>~6 000 stars with realistic brightness distribution and color tints</li>
     *   <li>Bright stars with cross-shaped diffraction spikes</li>
     *   <li>Faint diagonal Milky Way core glow (warm amber/blue)</li>
     *   <li>Subtle nebula color wisps</li>
     * </ul>
     *
     * @param outputPath absolute path for the output JPG
     */
    public static void generateGalaxy(String outputPath) {
        int W = 2048, H = 1024;
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);

        // 1. True-black base
        for (int y = 0; y < H; y++)
            for (int x = 0; x < W; x++)
                img.setRGB(x, y, 0);

        // 2. Faint Milky Way band and nebula wisps
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                double nx = x / (double) W;
                double ny = y / (double) H;

                // Milky Way band: slightly off-center diagonal ridge
                double bandY   = 0.5 + 0.06 * Math.sin(nx * 3.7) + 0.04 * Math.sin(nx * 7.1);
                double bandDist = Math.abs(ny - bandY) / 0.14;
                double band    = Math.exp(-bandDist * bandDist * 3.0);

                // Nebula wisps (subtle color)
                double wi1 = 0.025 * (Math.sin(nx * 9.3 + ny * 5.8) + Math.sin(nx * 17.1 - ny * 11.3));
                double wi2 = 0.018 * (Math.sin(nx * 6.7 - ny * 14.2) + Math.sin(nx * 22.4 + ny * 8.1));
                double wi3 = 0.012 * Math.sin(nx * 38.0 + ny * 21.0);

                // Milky Way: warm amber core + blue fringe
                int r = clamp((int)(band * 14 + wi1 * 160));
                int g = clamp((int)(band *  9 + wi2 * 110 + wi3 * 80));
                int b = clamp((int)(band * 22 + (wi1 + wi2) * 140 + wi3 * 100));

                if (r > 0 || g > 0 || b > 0)
                    img.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }

        // 3. Stars — realistic power-law brightness distribution
        Random rng = new Random(0xF0572AB3L);
        int numStars = 6500;
        for (int i = 0; i < numStars; i++) {
            int sx = rng.nextInt(W);
            int sy = rng.nextInt(H);

            // Power-law: most stars are dim, very few are bright
            double brightness = rng.nextDouble();
            brightness = brightness * brightness * brightness;  // cube → very skewed
            int lum = clamp(28 + (int)(brightness * 228));

            // Spectral tint
            int r, g, b;
            int spectral = rng.nextInt(10);
            if (spectral == 0) {          // O/B blue-white
                r = clamp(lum - 50); g = clamp(lum - 18); b = lum;
            } else if (spectral == 1) {   // K/M orange-red giant
                r = lum; g = clamp(lum - 40); b = clamp(lum - 80);
            } else if (spectral == 2) {   // G yellow (Sun-like)
                r = lum; g = clamp(lum - 12); b = clamp(lum - 40);
            } else {                      // A/F white / blue-white
                int tiny = lum > 180 ? 8 : 3;
                r = clamp(lum - tiny / 2); g = clamp(lum - tiny / 3); b = lum;
            }

            img.setRGB(sx, sy, (r << 16) | (g << 8) | b);

            // Very bright stars: 4-point diffraction spikes + glow halo
            if (lum > 205) {
                int halo = lum / 4;
                // Cross spikes
                for (int k = 1; k <= 3; k++) {
                    int fade = halo / (k + 1);
                    if (sx - k >= 0)     setMax(img, sx - k, sy, fade, fade, fade);
                    if (sx + k < W)      setMax(img, sx + k, sy, fade, fade, fade);
                    if (sy - k >= 0)     setMax(img, sx, sy - k, fade, fade, fade);
                    if (sy + k < H)      setMax(img, sx, sy + k, fade, fade, fade);
                }
                // 1-pixel diagonal halo
                if (sx > 0 && sy > 0)         setMax(img, sx-1, sy-1, halo/3, halo/3, halo/3);
                if (sx < W-1 && sy > 0)       setMax(img, sx+1, sy-1, halo/3, halo/3, halo/3);
                if (sx > 0 && sy < H-1)       setMax(img, sx-1, sy+1, halo/3, halo/3, halo/3);
                if (sx < W-1 && sy < H-1)     setMax(img, sx+1, sy+1, halo/3, halo/3, halo/3);
            }
        }

        save(img, outputPath);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }

    /** Sets a pixel to the component-wise maximum of the current and new values. */
    private static void setMax(BufferedImage img, int x, int y, int r, int g, int b) {
        int old = img.getRGB(x, y);
        int or = (old >> 16) & 0xFF, og = (old >> 8) & 0xFF, ob = old & 0xFF;
        img.setRGB(x, y, (Math.max(or, r) << 16) | (Math.max(og, g) << 8) | Math.max(ob, b));
    }

    private static void save(BufferedImage img, String path) {
        try {
            File f = new File(path);
            f.getParentFile().mkdirs();
            if (!ImageIO.write(img, "jpg", f))
                throw new RuntimeException("TextureGenerator: no JPEG writer: " + path);
        } catch (IOException e) {
            throw new RuntimeException("TextureGenerator: cannot write: " + path, e);
        }
    }
}
