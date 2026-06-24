package renderer;

import geometries.impl.*;
import lighting.*;
import org.junit.jupiter.api.Test;
import primitives.*;
import scene.Scene;

/**
 * Mini-Project 2 -- Performance Acceleration (BVH).
 *
 * <p>Scene: "Desert Sunset" v2 -- dramatic low-poly canyon at golden hour with:
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

    // Sun world position -- shared by sphere, lights, and ray emitter
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

    /**
     * Sunset desert palette: burnt-orange low, warm gold high, atmospheric haze at distance.
     * Uses a sin-based hash to decorrelate adjacent triangles that share the same row,
     * ensuring each triangle has a visually distinct colour rather than a banded look.
     */
    private static Color terrC(double x, double z, double h) {
        double t    = Math.max(0, Math.min(1, (h + 15.2) / 30.4));
        // Hash that gives independent pseudo-random value per triangle centroid
        double rf   = frac(Math.sin(x * 127.1 + z * 311.7) * 43758.5);
        double haze = Math.max(0, Math.min(1, (-z - 20) / 260.0));
        return new Color(
            Math.min(255, (int)(100 + t * 105 + rf * 45 + haze * 55)),
            Math.min(255, (int)( 45 + t *  65 + rf * 22 + haze * 40)),
            Math.min(255, (int)(  8 + t *  18 + rf * 10 + haze * 30)));
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

    // ========================= Cactus geometry helpers =========================

    // Directional light is (-0.8,-0.38,0.4) so light comes FROM (0.8,0,-0.4).
    // Normalized XZ component toward sun used for per-face sun tinting.
    private static final double SUN_HORIZ_X = 0.8944, SUN_HORIZ_Z = -0.4472;
    // Full 3D sun direction: normalize(0.8, 0.38, -0.4)
    private static final double SUN3D_X = 0.8231, SUN3D_Y = 0.3910, SUN3D_Z = -0.4116;

    /**
     * Ribbed cactus trunk as a (2*nRibs)-sided prism.
     *
     * Alternating ridge/valley radii (R+1.0 / R-0.6) create the classic
     * saguaro rib texture in low-poly style.  Each rectangular face is tinted
     * warm yellow-green on the sun side and cool blue-green on the shadow side,
     * computed from the face's outward normal dotted against the sun direction.
     */
    private static void addRibbedTrunk(Scene scene, double cx, double cy, double cz,
                                        double radius, double height, int nRibs,
                                        Material mat) {
        int N = nRibs * 2;
        double[] vx = new double[N], vz = new double[N];
        double ridgeR = radius + 1.4, valleyR = Math.max(0.5, radius - 0.9);
        for (int i = 0; i < N; i++) {
            double a = 2 * Math.PI * i / N;
            double r = (i % 2 == 0) ? ridgeR : valleyR;
            vx[i] = cx + r * Math.cos(a);
            vz[i] = cz + r * Math.sin(a);
        }
        for (int i = 0; i < N; i++) {
            int j = (i + 1) % N;
            double midX = (vx[i] + vx[j]) / 2 - cx;
            double midZ = (vz[i] + vz[j]) / 2 - cz;
            double mag  = Math.sqrt(midX*midX + midZ*midZ);
            if (mag < 1e-9) continue;
            // How much this face points toward the sun (-1 = full shadow, +1 = full sun)
            double sunDot = (midX/mag) * SUN_HORIZ_X + (midZ/mag) * SUN_HORIZ_Z;
            double t = Math.max(0, Math.min(1, (sunDot + 1) / 2.0));
            // Deep blue-green in shadow valleys -> vivid yellow-green on sun ridges
            Color fc = new Color(
                (int)(10 + t * 65),   // R: 10 (shadow) -> 75 (sun)
                (int)(42 + t * 108),  // G: 42 -> 150
                (int)(32 - t *  12)); // B: 32 (cool) -> 20 (warm)
            Point bl = new Point(vx[i], cy,          vz[i]);
            Point br = new Point(vx[j], cy,          vz[j]);
            Point tl = new Point(vx[i], cy + height, vz[i]);
            Point tr = new Point(vx[j], cy + height, vz[j]);
            // Winding that gives outward normals (verified analytically)
            scene.geometries.add(new Triangle(bl, tr, br).setEmission(fc).setMaterial(mat));
            scene.geometries.add(new Triangle(bl, tl, tr).setEmission(fc).setMaterial(mat));
        }
        // Top cap -- fan from center, CCW from above -> upward normals
        Color topC = new Color(42, 112, 35);
        Point topPt = new Point(cx, cy + height, cz);
        for (int i = 0; i < N; i++) {
            int j = (i + 1) % N;
            scene.geometries.add(new Triangle(
                topPt,
                new Point(vx[j], cy + height, vz[j]),
                new Point(vx[i], cy + height, vz[i]))
                .setEmission(topC).setMaterial(mat));
        }
    }

    /**
     * Ribbed arm segment as a (2*nRibs)-sided prism oriented along an arbitrary direction.
     * Cross-section basis is computed via Gram-Schmidt from (0,0,1), giving ribs
     * that are visible from the front camera for all arm orientations in the XY plane.
     * Each face is tinted by its full-3D dot-product with the sun direction.
     */
    private static void addRibbedArm(Scene scene,
                                      double fromX, double fromY, double fromZ,
                                      double dirX,  double dirY,  double dirZ,
                                      double radius, double length, int nRibs, Material mat) {
        double dMag = Math.sqrt(dirX*dirX + dirY*dirY + dirZ*dirZ);
        double dx = dirX/dMag, dy = dirY/dMag, dz = dirZ/dMag;

        // u - d via Gram-Schmidt from (0,0,1); fall back to (1,0,0) if d~=(0,0,+-1)
        double ux = 0, uy = 0, uz = 1;
        if (Math.abs(dz) > 0.9) { ux = 1; uy = 0; uz = 0; }
        double dot = ux*dx + uy*dy + uz*dz;
        ux -= dot*dx; uy -= dot*dy; uz -= dot*dz;
        double uMag = Math.sqrt(ux*ux + uy*uy + uz*uz);
        ux /= uMag; uy /= uMag; uz /= uMag;

        // v = d x u  (completes right-hand frame)
        double vx = dy*uz - dz*uy, vy = dz*ux - dx*uz, vz2 = dx*uy - dy*ux;

        int N = nRibs * 2;
        double ridgeR = radius + 0.6, valleyR = Math.max(0.2, radius - 0.4);
        double[] bx=new double[N], by=new double[N], bz2=new double[N];
        double[] ex=new double[N], ey=new double[N], ez2=new double[N];
        double endX = fromX+dx*length, endY = fromY+dy*length, endZ = fromZ+dz*length;

        for (int i = 0; i < N; i++) {
            double a = 2*Math.PI*i/N;
            double r = (i%2==0) ? ridgeR : valleyR;
            double ca = Math.cos(a), sa = Math.sin(a);
            double ox = r*(ca*ux + sa*vx), oy = r*(ca*uy + sa*vy), oz = r*(ca*uz + sa*vz2);
            bx[i]=fromX+ox; by[i]=fromY+oy; bz2[i]=fromZ+oz;
            ex[i]=endX+ox;  ey[i]=endY+oy;  ez2[i]=endZ+oz;
        }
        for (int i = 0; i < N; i++) {
            int j = (i+1)%N;
            // Outward normal = midpoint offset minus its d-component
            double mox=(bx[i]+bx[j])/2-fromX, moy=(by[i]+by[j])/2-fromY, moz=(bz2[i]+bz2[j])/2-fromZ;
            double dDot = mox*dx + moy*dy + moz*dz;
            double nx=mox-dDot*dx, ny=moy-dDot*dy, nz=moz-dDot*dz;
            double nm = Math.sqrt(nx*nx+ny*ny+nz*nz);
            if (nm < 1e-9) continue;
            double sunDot = (nx/nm)*SUN3D_X + (ny/nm)*SUN3D_Y + (nz/nm)*SUN3D_Z;
            double t = Math.max(0, Math.min(1, (sunDot+1)/2.0));
            Color fc = new Color(
                (int)(10 + t * 65),
                (int)(42 + t * 108),
                (int)(32 - t *  12));
            // Winding Triangle(base_i, end_j, base_j) gives outward normals (verified)
            scene.geometries.add(new Triangle(
                new Point(bx[i],by[i],bz2[i]),
                new Point(ex[j],ey[j],ez2[j]),
                new Point(bx[j],by[j],bz2[j])).setEmission(fc).setMaterial(mat));
            scene.geometries.add(new Triangle(
                new Point(bx[i],by[i],bz2[i]),
                new Point(ex[i],ey[i],ez2[i]),
                new Point(ex[j],ey[j],ez2[j])).setEmission(fc).setMaterial(mat));
        }
    }

    // ========================= 3-D math helpers (double[] vectors) ===========

    private static double[] normalize3(double[] v) {
        double len = Math.sqrt(v[0]*v[0] + v[1]*v[1] + v[2]*v[2]);
        return len < 1e-9 ? new double[]{0, 1, 0} : new double[]{v[0]/len, v[1]/len, v[2]/len};
    }
    private static double[] cross3(double[] a, double[] b) {
        return new double[]{ a[1]*b[2]-a[2]*b[1], a[2]*b[0]-a[0]*b[2], a[0]*b[1]-a[1]*b[0] };
    }
    private static double dot3(double[] a, double[] b) {
        return a[0]*b[0] + a[1]*b[1] + a[2]*b[2];
    }
    private static Vector toVec(double[] n) {
        try { return new Vector(n[0], n[1], n[2]).normalize(); }
        catch (IllegalArgumentException e) { return new Vector(0, 1, 0); }
    }

    // ========================= Diamond-Square mountain =======================

    /**
     * Generates a (2^n+1)x(2^n+1) heightmap via the Diamond-Square algorithm.
     * Values are in an arbitrary range; caller normalizes and scales.
     *
     * @param n         grid exponent (grid size = 2^n + 1)
     * @param roughness persistence per iteration (0.5 = natural; 0.7 = jagged)
     * @param seed      RNG seed for reproducibility
     */
    private static double[][] diamondSquare(int n, double roughness, long seed) {
        int sz = (1 << n) + 1;
        double[][] h = new double[sz][sz];
        java.util.Random rng = new java.util.Random(seed);
        // Corner seeds -- zero so edges fade to base level
        h[0][0] = h[0][sz-1] = h[sz-1][0] = h[sz-1][sz-1] = 0;

        double scale = 1.0;
        for (int step = sz-1; step > 1; step >>= 1, scale *= roughness) {
            int half = step >> 1;
            // Diamond step: fill square midpoints
            for (int y = 0; y < sz-1; y += step)
                for (int x = 0; x < sz-1; x += step) {
                    double avg = (h[y][x]+h[y][x+step]+h[y+step][x]+h[y+step][x+step])*0.25;
                    h[y+half][x+half] = avg + (rng.nextDouble()*2-1)*scale;
                }
            // Square step: fill edge midpoints
            for (int y = 0; y < sz; y += half)
                for (int x = (y+half)%step; x < sz; x += step) {
                    double sum = 0; int cnt = 0;
                    if (y-half>=0)   { sum+=h[y-half][x]; cnt++; }
                    if (y+half<sz)   { sum+=h[y+half][x]; cnt++; }
                    if (x-half>=0)   { sum+=h[y][x-half]; cnt++; }
                    if (x+half<sz)   { sum+=h[y][x+half]; cnt++; }
                    h[y][x] = sum/cnt + (rng.nextDouble()*2-1)*scale;
                }
        }
        return h;
    }

    /**
     * Replaces the two flat ridge panels with a true 3-D fractal mountain mass.
     * Uses a 33x33 Diamond-Square heightmap tessellated into SmoothTriangles
     * with per-vertex gradient normals so the sunset light wraps across slopes.
     *
     * The mountain occupies xin[xMin,xMax], zin[zBack,zFront] and rises to
     * {@code peakScale} units above {@code baseY}.  A sinusoidal X-envelope
     * plus a linear Z-taper (tall at back, zero at front) give natural silhouettes.
     */
    private static void addFractalMountain(Scene scene, Material mat,
                                            double xMin, double xMax,
                                            double zBack, double zFront,
                                            double baseY, double peakScale) {
        final int n = 5;                // 33x33 grid
        double[][] raw = diamondSquare(n, 0.62, 0xDEAD_BEEFL);
        int sz = raw.length; // 33

        // Normalise raw noise to [0,1]
        double rMin=Double.MAX_VALUE, rMax=-Double.MAX_VALUE;
        for (double[] row : raw) for (double v : row) { rMin=Math.min(rMin,v); rMax=Math.max(rMax,v); }
        double rRange = Math.max(rMax-rMin, 1e-6);

        double dx = (xMax-xMin)/(sz-1);
        double dz = (zFront-zBack)/(sz-1); // positive: z increases toward camera

        // World heights h[row][col], row->z, col->x
        double[][] h = new double[sz][sz];
        for (int r=0; r<sz; r++) for (int c=0; c<sz; c++) {
            double norm = (raw[r][c]-rMin)/rRange;
            double xEnv = Math.sin(Math.PI * c/(sz-1));        // fade at x edges
            double zEnv = 1.0 - (double)r/(sz-1);             // tall at back, 0 at front
            h[r][c] = peakScale * norm * xEnv * xEnv * zEnv;
        }

        // Vertex world positions
        Point[][] pts = new Point[sz][sz];
        for (int r=0; r<sz; r++) for (int c=0; c<sz; c++)
            pts[r][c] = new Point(xMin+c*dx, baseY+h[r][c], zBack+r*dz);

        // Per-vertex normals from central-difference gradient: n = normalize(-dh/dx, 1, -dh/dz)
        Vector[][] vn = new Vector[sz][sz];
        for (int r=0; r<sz; r++) for (int c=0; c<sz; c++) {
            double dhx = c>0&&c<sz-1 ? (h[r][c+1]-h[r][c-1])/(2*dx)
                        : c==0       ? (h[r][1]-h[r][0])/dx
                                     : (h[r][sz-1]-h[r][sz-2])/dx;
            double dhz = r>0&&r<sz-1 ? (h[r+1][c]-h[r-1][c])/(2*dz)
                        : r==0       ? (h[1][c]-h[0][c])/dz
                                     : (h[sz-1][c]-h[sz-2][c])/dz;
            vn[r][c] = toVec(new double[]{-dhx, 1.0, -dhz});
        }

        // Tessellate quads into two SmoothTriangles each
        for (int r=0; r<sz-1; r++) for (int c=0; c<sz-1; c++) {
            double avgH = (h[r][c]+h[r][c+1]+h[r+1][c]+h[r+1][c+1])*0.25;
            if (avgH < 0.5) continue; // skip flat background quads
            double litF = Math.max(0, Math.min(1, ((xMin+c*dx)+222)/444.0));
            int cr = (int)(55 + litF*45 + avgH*0.10);
            int cg = (int)(22 + litF*22 + avgH*0.04);
            int cb = (int)(12 + litF*10 + avgH*0.02);
            Color mc = new Color(Math.min(255,cr), Math.min(255,cg), Math.min(255,cb));

            // Winding Triangle(p00,p11,p01) and Triangle(p00,p10,p11) -> upward face normals
            scene.geometries.add(new SmoothTriangle(
                pts[r][c], pts[r+1][c], pts[r+1][c+1],
                vn[r][c],  vn[r+1][c], vn[r+1][c+1])
                .setEmission(mc).setMaterial(mat));
            scene.geometries.add(new SmoothTriangle(
                pts[r][c], pts[r+1][c+1], pts[r][c+1],
                vn[r][c],  vn[r+1][c+1], vn[r][c+1])
                .setEmission(mc).setMaterial(mat));
        }
    }

    // ========================= Bezier-tube cactus arms =======================

    /**
     * Builds a curved cactus arm as a cubic Bezier tube mesh.
     *
     * The arm spine follows the Bezier path P0->P1->P2->P3.  At each of
     * {@code nSamples} curve samples a {@code nSides}-sided ribbed circle
     * is extruded perpendicular to the tangent using a rotation-minimising
     * (parallel-transport) frame so the cross-section never flip-twists.
     * Each quad face is split into two SmoothTriangles whose vertex normals
     * are the outward radial directions, giving smooth Phong shading.
     */
    private static void buildBezierArm(Scene scene,
                                        double[] P0, double[] P1, double[] P2, double[] P3,
                                        double radius, int nSamples, int nSides, Material mat,
                                        Color thornColor, Material thornMat) {
        // -- Sample the Bezier curve ------------------------------------------
        double[][] pos  = new double[nSamples][3];
        double[][] tang = new double[nSamples][3];
        for (int k=0; k<nSamples; k++) {
            double t  = (double)k/(nSamples-1);
            double t2 = t*t, t3=t2*t, u=1-t, u2=u*u, u3=u2*u;
            for (int i=0; i<3; i++) {
                pos[k][i]  = u3*P0[i] + 3*u2*t*P1[i] + 3*u*t2*P2[i] + t3*P3[i];
                tang[k][i] = 3*(u2*(P1[i]-P0[i]) + 2*u*t*(P2[i]-P1[i]) + t2*(P3[i]-P2[i]));
            }
        }

        // -- Build rotation-minimising frame via parallel transport -----------
        double[][] us = new double[nSamples][3]; // first frame axis (- to tangent)
        double[][] vs = new double[nSamples][3]; // second frame axis
        {
            double[] T0 = normalize3(tang[0]);
            // Initial u: Gram-Schmidt of (0,1,0) against T
            double[] up = {0,1,0};
            double d = dot3(up, T0);
            double[] u0 = normalize3(new double[]{up[0]-d*T0[0], up[1]-d*T0[1], up[2]-d*T0[2]});
            us[0] = u0;
            vs[0] = normalize3(cross3(T0, u0));
            for (int k=1; k<nSamples; k++) {
                double[] Tk = normalize3(tang[k]);
                double dU = dot3(us[k-1], Tk);
                us[k] = normalize3(new double[]{us[k-1][0]-dU*Tk[0],
                                                us[k-1][1]-dU*Tk[1],
                                                us[k-1][2]-dU*Tk[2]});
                vs[k] = normalize3(cross3(Tk, us[k]));
            }
        }

        // -- Extrude ribbed cross-sections ------------------------------------
        double ridgeR = radius+0.5, valleyR = Math.max(0.15, radius-0.35);
        Point[][] circ  = new Point[nSamples][nSides];
        Vector[][] vnrm = new Vector[nSamples][nSides];
        for (int k=0; k<nSamples; k++) {
            double[] Tk = normalize3(tang[k]);
            for (int i=0; i<nSides; i++) {
                double a = 2*Math.PI*i/nSides;
                double r = (i%2==0) ? ridgeR : valleyR;
                double ca=Math.cos(a), sa=Math.sin(a);
                double ox=r*(ca*us[k][0]+sa*vs[k][0]);
                double oy=r*(ca*us[k][1]+sa*vs[k][1]);
                double oz=r*(ca*us[k][2]+sa*vs[k][2]);
                circ[k][i] = new Point(pos[k][0]+ox, pos[k][1]+oy, pos[k][2]+oz);
                // Outward radial normal (d-component removed for perpendicularity)
                double dDot = ox*Tk[0]+oy*Tk[1]+oz*Tk[2];
                vnrm[k][i] = toVec(new double[]{ox-dDot*Tk[0], oy-dDot*Tk[1], oz-dDot*Tk[2]});
            }
        }

        // -- Tessellate tube into SmoothTriangles ----------------------------
        // Winding (bik, bi1k, bjk) -> outward normal (verified analytically for tube)
        for (int k=0; k<nSamples-1; k++) for (int i=0; i<nSides; i++) {
            int j = (i+1)%nSides;
            // Sun colour from average outward direction
            double sunDot;
            try {
                Vector avgN = vnrm[k][i].add(vnrm[k+1][i]).add(vnrm[k][j]).add(vnrm[k+1][j]);
                sunDot = avgN.normalize()
                             .dotProduct(new Vector(SUN3D_X, SUN3D_Y, SUN3D_Z));
            } catch (Exception e) { sunDot = 0; }
            double ts = Math.max(0, Math.min(1, (sunDot+1)/2.0));
            Color fc = new Color((int)(10+ts*65),(int)(42+ts*108),(int)(32-ts*12));

            scene.geometries.add(new SmoothTriangle(
                circ[k][i], circ[k+1][i], circ[k][j],
                vnrm[k][i], vnrm[k+1][i], vnrm[k][j])
                .setEmission(fc).setMaterial(mat));
            scene.geometries.add(new SmoothTriangle(
                circ[k][j], circ[k+1][i], circ[k+1][j],
                vnrm[k][j], vnrm[k+1][i], vnrm[k+1][j])
                .setEmission(fc).setMaterial(mat));
        }

        // -- Thorns at Bezier tube ridge points -------------------------------
        // Added inline to reuse the already-computed pos[][], us[][], vs[][]
        // frame -- guarantees thorns land exactly on the rib peaks.
        // Thorns fire at every other sample (k odd) and every ridge (even i).
        // Count per arm: -nSamples/2- x (nSides/2) x 2 ~= 5 x 4 x 2 = 40 tris
        if (thornMat != null) {
            final double THORN_L = 2.5;  // outward reach
            final double THORN_H = 0.26; // base half-spread along tangent
            final double THORN_T = 0.40; // tip tilt along tangent
            for (int k = 1; k < nSamples - 1; k += 2) {
                double[] Tk = normalize3(tang[k]);
                for (int i = 0; i < nSides; i += 2) { // i even = ridge vertices
                    double a  = 2 * Math.PI * i / nSides;
                    double ca = Math.cos(a), sa = Math.sin(a);
                    // Offset to rib peak using the parallel-transport frame
                    double ox = ridgeR * (ca*us[k][0] + sa*vs[k][0]);
                    double oy = ridgeR * (ca*us[k][1] + sa*vs[k][1]);
                    double oz = ridgeR * (ca*us[k][2] + sa*vs[k][2]);
                    double rx = pos[k][0]+ox, ry = pos[k][1]+oy, rz = pos[k][2]+oz;
                    // Outward normal - tangent
                    double dDot = ox*Tk[0] + oy*Tk[1] + oz*Tk[2];
                    double[] outn = normalize3(new double[]{
                        ox - dDot*Tk[0], oy - dDot*Tk[1], oz - dDot*Tk[2]});
                    // Base spread along tangent; tip outward + slight forward tilt
                    Point p0  = new Point(rx+Tk[0]*THORN_H, ry+Tk[1]*THORN_H, rz+Tk[2]*THORN_H);
                    Point p1  = new Point(rx-Tk[0]*THORN_H, ry-Tk[1]*THORN_H, rz-Tk[2]*THORN_H);
                    Point tip = new Point(rx+outn[0]*THORN_L+Tk[0]*THORN_T,
                                          ry+outn[1]*THORN_L+Tk[1]*THORN_T,
                                          rz+outn[2]*THORN_L+Tk[2]*THORN_T);
                    scene.geometries.add(new Triangle(p0, p1, tip)
                            .setEmission(thornColor).setMaterial(thornMat));
                    scene.geometries.add(new Triangle(p1, p0, tip)
                            .setEmission(thornColor).setMaterial(thornMat));
                }
            }
        }
    }

    // ========================= Ribbed-spheroid barrel cactus =================

    /**
     * A proper barrel cactus: a UV-tessellated oblate spheroid whose radius is
     * modulated by {@code nRibs} cosine ribs.  Each face uses a SmoothTriangle
     * with per-vertex ellipsoid normals so the sunset light wraps smoothly
     * around the surface instead of showing hard polygon edges.
     *
     * @param nLon longitude slices (>= 2xnRibs for the ribs to resolve)
     * @param nLat latitude stacks
     * @param nRibs number of vertical ribs (each rib = one ridge + one valley)
     */
    private static void addRibbedBarrel(Scene scene, double cx, double cy, double cz,
                                         double radius, double height,
                                         int nLon, int nLat, int nRibs, Material mat) {
        double H   = height / 2.0;  // half-height (Y semi-axis)
        double R   = radius;        // XZ semi-axis (base)
        double rd  = 0.18;          // rib depth fraction

        Point[][]  pts = new Point[nLat+1][nLon+1];
        Vector[][] vns = new Vector[nLat+1][nLon+1];

        for (int vi=0; vi<=nLat; vi++) {
            double v   = -Math.PI/2 + Math.PI*vi/nLat;
            double sinV= Math.sin(v), cosV= Math.cos(v);
            for (int ui=0; ui<=nLon; ui++) {
                double u   = 2*Math.PI*ui/nLon;
                double r   = R*(1 + rd*Math.cos(nRibs*u));
                double x   = r*cosV*Math.cos(u);
                double y   = H*sinV;
                double z   = r*cosV*Math.sin(u);
                pts[vi][ui]= new Point(cx+x, cy+H+y, cz+z);
                // Ellipsoid normal: gradient of xֲ²/Rֲ² + yֲ²/Hֲ² + zֲ²/Rֲ² = 1
                vns[vi][ui]= toVec(new double[]{x/R, y/H, z/R});
            }
        }

        for (int vi=0; vi<nLat; vi++) for (int ui=0; ui<nLon; ui++) {
            Point p00=pts[vi][ui], p10=pts[vi+1][ui], p11=pts[vi+1][ui+1], p01=pts[vi][ui+1];
            Vector n00=vns[vi][ui], n10=vns[vi+1][ui], n11=vns[vi+1][ui+1], n01=vns[vi][ui+1];

            double sunDot;
            try {
                Vector avgN = n00.add(n10).add(n11).add(n01);
                sunDot = avgN.normalize()
                             .dotProduct(new Vector(SUN3D_X, SUN3D_Y, SUN3D_Z));
            } catch (Exception e) { sunDot = 0; }
            double ts = Math.max(0, Math.min(1, (sunDot+1)/2.0));
            Color fc = new Color((int)(10+ts*65),(int)(42+ts*108),(int)(32-ts*12));

            // Winding Triangle(p00,p10,p11) -> outward for sphere (verified)
            try { scene.geometries.add(new SmoothTriangle(p00,p10,p11,n00,n10,n11)
                      .setEmission(fc).setMaterial(mat)); }
            catch (IllegalArgumentException ignored) {}
            try { scene.geometries.add(new SmoothTriangle(p00,p11,p01,n00,n11,n01)
                      .setEmission(fc).setMaterial(mat)); }
            catch (IllegalArgumentException ignored) {}
        }
    }

    // ========================= Thorn / spike geometry =================

    /**
     * Adds straw-coloured thorn spikes along the ribs of a saguaro trunk.
     * Each spike is a double-sided flat triangle (2 triangles) so it is
     * visible from both sides as the camera orbits.
     * Alternate rows are staggered by half a rib-angle, matching the helical
     * thorn-cluster growth pattern of real cacti.
     *
     * Triangle count per trunk: rows x nRibs x 2  (~= 9 x 8 x 2 = 144)
     */
    private static void addThornsOnTrunk(Scene scene,
            double cx, double cy, double cz,
            double radius, double height, int nRibs,
            Color thornColor, Material thornMat) {
        final double RIDGE_R = radius + 1.4;  // must match addRibbedTrunk
        final double THORN_L = 3.0;            // outward length
        final double THORN_H = 0.27;           // half-height of base spread
        final double THORN_U = 0.55;           // upward tip tilt
        final double ROW_DY  = 7.5;            // vertical spacing

        int rows = (int)((height - 4.0) / ROW_DY);
        for (int row = 0; row < rows; row++) {
            double h = 3.5 + row * ROW_DY;
            double off = (row % 2 == 0) ? 0.0 : Math.PI / nRibs; // helical stagger
            for (int rib = 0; rib < nRibs; rib++) {
                double ang = 2 * Math.PI * rib / nRibs + off;
                double ox = Math.cos(ang), oz = Math.sin(ang);
                double rx = cx + ox * RIDGE_R, rz = cz + oz * RIDGE_R, ry = cy + h;
                Point p0  = new Point(rx, ry + THORN_H, rz);
                Point p1  = new Point(rx, ry - THORN_H, rz);
                Point tip = new Point(cx + ox * (RIDGE_R + THORN_L),
                                      ry + THORN_U,
                                      cz + oz * (RIDGE_R + THORN_L));
                scene.geometries.add(new Triangle(p0, p1, tip)
                        .setEmission(thornColor).setMaterial(thornMat));
                scene.geometries.add(new Triangle(p1, p0, tip)
                        .setEmission(thornColor).setMaterial(thornMat));
            }
        }
    }

    /**
     * Adds thorn spikes to the rib peaks of a ribbed UV-spheroid barrel cactus.
     * Thorns are placed at u = 2ֿ€ֲ·k/nRibs (rib peaks) across several latitude
     * bands (poles excluded).  The spike points along the ellipsoid gradient --
     * the same outward direction as the per-vertex normal used for smooth shading.
     *
     * Triangle count per barrel: nLatThorns x nRibs x 2  (~= 5 x 20 x 2 = 200)
     */
    private static void addThornsOnBarrel(Scene scene,
            double cx, double cy, double cz,
            double radius, double height, int nRibs,
            Color thornColor, Material thornMat) {
        double H  = height / 2.0;
        double R  = radius;
        double rd = 0.18;          // must match addRibbedBarrel
        final double THORN_L = 2.2;
        final double THORN_H = 0.28;

        final int nLatThorns = 5;
        for (int vi = 0; vi < nLatThorns; vi++) {
            // 15% -> 85% of latitude to avoid pole artifacts
            double vFrac = 0.15 + 0.70 * vi / (double)(nLatThorns - 1);
            double v    = -Math.PI / 2 + Math.PI * vFrac;
            double sinV = Math.sin(v), cosV = Math.cos(v);
            for (int ri = 0; ri < nRibs; ri++) {
                double u  = 2 * Math.PI * ri / nRibs;
                double r  = R * (1 + rd);               // rib peak
                double x  = r * cosV * Math.cos(u);
                double y  = H * sinV;
                double z  = r * cosV * Math.sin(u);
                double sx = cx + x, sy = cy + H + y, sz = cz + z;
                // Ellipsoid outward normal (same formula as addRibbedBarrel)
                double[] outn = normalize3(new double[]{x / R, y / H, z / R});
                // Up = dv direction (latitude tangent = direction along the rib)
                double[] up = normalize3(new double[]{
                    -r * sinV * Math.cos(u),
                     H * cosV,
                    -r * sinV * Math.sin(u)});
                Point p0  = new Point(sx + up[0]*THORN_H, sy + up[1]*THORN_H, sz + up[2]*THORN_H);
                Point p1  = new Point(sx - up[0]*THORN_H, sy - up[1]*THORN_H, sz - up[2]*THORN_H);
                Point tip = new Point(sx + outn[0]*THORN_L, sy + outn[1]*THORN_L, sz + outn[2]*THORN_L);
                scene.geometries.add(new Triangle(p0, p1, tip)
                        .setEmission(thornColor).setMaterial(thornMat));
                scene.geometries.add(new Triangle(p1, p0, tip)
                        .setEmission(thornColor).setMaterial(thornMat));
            }
        }
    }

    /** Short wide barrel cactus: ribbed prism + dome sphere cap. */
    private static void addBarrelCactus(Scene scene, double cx, double cy, double cz,
                                         double radius, double height, Material mat) {
        addRibbedTrunk(scene, cx, cy, cz, radius, height, 8, mat);
        scene.geometries.add(new Sphere(new Point(cx, cy + height, cz), radius * 0.75)
            .setEmission(new Color(35, 100, 25)).setMaterial(mat));
    }

    /** Small desert shrub -- cluster of four dark-green spheres. */
    private static void addShrub(Scene scene, double cx, double cy, double cz, Material mat) {
        Color c = new Color(28, 62, 18);
        scene.geometries.add(new Sphere(new Point(cx,   cy+3.5, cz),    5.0).setEmission(c).setMaterial(mat));
        scene.geometries.add(new Sphere(new Point(cx-4, cy+2.5, cz+2),  3.8).setEmission(c).setMaterial(mat));
        scene.geometries.add(new Sphere(new Point(cx+3, cy+2.5, cz-2),  3.5).setEmission(c).setMaterial(mat));
        scene.geometries.add(new Sphere(new Point(cx+1, cy+2.0, cz+3.5),3.0).setEmission(c).setMaterial(mat));
    }

    // ========================= Scene =========================

    private static Scene buildScene() {
        Scene scene = new Scene("Desert Sunset");
        scene.setBackground(new Color(52, 22, 68));
        scene.setAmbientLight(new AmbientLight(new Color(28, 18, 10), new Double3(1)));

        Material flat  = new Material().setKD(0.88).setKS(0.10).setShininess(8);
        Material skyM  = new Material().setKD(0.78).setKS(0.08).setShininess(5);
        Material rockM = new Material().setKD(0.72).setKS(0.25).setShininess(30);
        Material cactM = new Material().setKD(0.80).setKS(0.35).setShininess(45)
                                       .setRimLighting(new Color(220, 115, 40), 2.8);

        // -- Backdrop plane (warm horizon glow) ----------------------------
        scene.geometries.add(new Plane(new Point(0, 0, -700), new Vector(0, 0, 1))
            .setEmission(new Color(212, 122, 35)).setMaterial(skyM));

        // -- Sun -- 4 concentric spheres producing a physical bloom / corona -
        //
        // Each outer shell is transparent (KT) so camera rays pass inward.
        // glowFalloff creates limb-darkening: bright at centre (Nֲ·V=1),
        // fades to black at silhouette (Nֲ·V=0) so the glow blends smoothly
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

        // -- Sky -- 280 triangles, 3-stop sunset gradient -------------------
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

        // -- Sun light rays -- 9 fan triangles at z=-590 -------------------
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

        // -- Terrain -- 1568 triangles (BVH stress-test) --------------------
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

        // -- 3-D fractal mountain mass (Diamond-Square 33x33 heightmap) -------
        // Replaces the two flat ridge panels with a true volumetric mountain.
        // 2048 SmoothTriangles with gradient-computed vertex normals let the
        // sunset directional light wrap smoothly across every slope and ridge.
        addFractalMountain(scene, rockM,
            -450, 450,      // x extent
            -268, -130,     // z: aligned with terrain back edge (TZ0) to close the seam
            BASE_Y, 165.0); // base Y, peak scale

        // -- Saguaro cacti -- 5 ribbed trunks with Bezier-curved arms ----------
        //   + procedural thorns on every rib of trunks, arms, and barrel cacti.
        //   Thorn material: pale straw-yellow, high specular, rim-lit so the
        //   low sunset light catches each spine with a warm golden glint.
        {
            // Thorn material -- distinct from cactus body, catches rim lighting
            Color thornC = new Color(222, 208, 145);
            Material thornM = new Material()
                    .setKD(0.50).setKS(0.55).setShininess(60)
                    .setRimLighting(new Color(255, 235, 148), 1.4);

            double[][] saguaroPos = {
                {-168,65},{-108,98},{52,40},{165,62},{238,90}
            };
            for (double[] cp : saguaroPos) {
                final double cx=cp[0], cz=cp[1];
                final double cy = BASE_Y + terrH(cx, cz);
                final double tH = 55 + frac(cx*0.13+cz*0.19)*30;

                // Ribbed trunk + thorns staggered in a helical pattern
                addRibbedTrunk(scene, cx, cy, cz, 4.5, tH, 8, cactM);
                addThornsOnTrunk(scene, cx, cy, cz, 4.5, tH, 8, thornC, thornM);

                // -- Left arm: droop slightly then sweep up (classic saguaro curve) --
                final double laY = cy + tH*0.50;
                final double laL = 20 + frac(cx*0.31+cz*0.19)*14;
                buildBezierArm(scene,
                    new double[]{cx,          laY,             cz},
                    new double[]{cx-laL*0.35, laY-laL*0.05,   cz},
                    new double[]{cx-laL*0.80, laY+laL*0.38,   cz},
                    new double[]{cx-laL*0.88, laY+laL*0.82,   cz},
                    2.8, 12, 8, cactM, thornC, thornM);

                // -- Right arm (mirror) ---------------------------------------------
                final double raY = cy + tH*0.42;
                final double raL = 18 + frac(cx*0.27+cz*0.35)*12;
                buildBezierArm(scene,
                    new double[]{cx,          raY,             cz},
                    new double[]{cx+raL*0.35, raY-raL*0.05,   cz},
                    new double[]{cx+raL*0.80, raY+raL*0.38,   cz},
                    new double[]{cx+raL*0.88, raY+raL*0.82,   cz},
                    2.8, 12, 8, cactM, thornC, thornM);
            }

            // Barrel cacti -- ribbed UV-spheroid + ellipsoid-gradient thorns
            double bcy1 = BASE_Y+terrH(-40,-12), bcy2 = BASE_Y+terrH(95,35);
            addRibbedBarrel(scene, -40, bcy1, -12,  9.0, 24, 32, 16, 20, cactM);
            addThornsOnBarrel(scene, -40, bcy1, -12,  9.0, 24, 20, thornC, thornM);
            addRibbedBarrel(scene,  95, bcy2,  35,  7.5, 19, 32, 16, 20, cactM);
            addThornsOnBarrel(scene,  95, bcy2,  35,  7.5, 19, 20, thornC, thornM);

            // Desert shrubs -- small sphere clusters for ground cover
            Material shrubM = new Material().setKD(0.75).setKS(0.12).setShininess(8);
            addShrub(scene, -210, BASE_Y+terrH(-210, 80),  80, shrubM);
            addShrub(scene,  280, BASE_Y+terrH( 280, 55),  55, shrubM);
        }

        // -- Lights --------------------------------------------------------
        // 1. Ambient: set above.
        // 2. DirectionalLight: ~7deg above horizon -- nearly flat sunset angle so
        //    cacti and mountains cast long geometric shadows across the terrain.
        //    Direction vector (-0.888, -0.122, 0.444) normalises to elevation ~=7deg.
        scene.lights.add(new DirectionalLight(
            new Color(255, 165, 55), new Vector(-0.888, -0.122, 0.444)));
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

    // ========================= Anti-Aliasing comparison tests =========================

    /** Renders without anti-aliasing (one ray per pixel). */
    @Test
    void testNoAntiAliasing() {
        Scene scene = buildScene();
        scene.geometries.buildBVH();
        SimpleRayTracer tracer = new SimpleRayTracer(scene).setSoftShadowSamples(1);
        System.out.println("=== No Anti-Aliasing ===");
        long start = System.currentTimeMillis();
        buildCameraBuilder(scene, tracer)
                .setAntiAliasingSamples(1).setMultithreading(-2).build()
                .renderImage().writeToImage("mp2_no_AA");
        System.out.printf("Render time (no AA): %,d ms%n", System.currentTimeMillis() - start);
    }

    /** Renders with anti-aliasing (FINAL_AA x FINAL_AA grid = smooth curves). */
    @Test
    void testWithAntiAliasing() {
        Scene scene = buildScene();
        scene.geometries.buildBVH();
        SimpleRayTracer tracer = new SimpleRayTracer(scene).setSoftShadowSamples(1);
        System.out.printf("=== With Anti-Aliasing %dx%d ===%n", FINAL_AA, FINAL_AA);
        long start = System.currentTimeMillis();
        buildCameraBuilder(scene, tracer)
                .setAntiAliasingSamples(FINAL_AA).setMultithreading(-2).build()
                .renderImage().writeToImage("mp2_with_AA");
        System.out.printf("Render time (AA %dx%d): %,d ms%n",
                FINAL_AA, FINAL_AA, System.currentTimeMillis() - start);
    }

    // ========================= Soft-Shadow comparison tests =========================

    /** Renders with hard shadows (one shadow ray -- no area sampling). */
    @Test
    void testNoSoftShadows() {
        Scene scene = buildScene();
        scene.geometries.buildBVH();
        SimpleRayTracer tracer = new SimpleRayTracer(scene).setSoftShadowSamples(1);
        System.out.println("=== No Soft Shadows ===");
        long start = System.currentTimeMillis();
        buildCameraBuilder(scene, tracer)
                .setAntiAliasingSamples(1).setMultithreading(-2).build()
                .renderImage().writeToImage("mp2_no_SS");
        System.out.printf("Render time (no SS): %,d ms%n", System.currentTimeMillis() - start);
    }

    /** Renders with soft shadows (FINAL_SS x FINAL_SS shadow rays). */
    @Test
    void testWithSoftShadows() {
        Scene scene = buildScene();
        scene.geometries.buildBVH();
        SimpleRayTracer tracer = new SimpleRayTracer(scene)
                .setSoftShadowSamples(FINAL_SS)
                .setSamplingPattern(Blackboard.SamplingPattern.JITTERED);
        System.out.printf("=== With Soft Shadows %dx%d ===%n", FINAL_SS, FINAL_SS);
        long start = System.currentTimeMillis();
        buildCameraBuilder(scene, tracer)
                .setAntiAliasingSamples(1).setMultithreading(-2).build()
                .renderImage().writeToImage("mp2_with_SS");
        System.out.printf("Render time (SS %dx%d): %,d ms%n",
                FINAL_SS, FINAL_SS, System.currentTimeMillis() - start);
    }

    // ========================= Combined AA + SS test =========================

    /**
     * Side-by-side comparison: baseline (no AA, no SS) vs full quality (AA + SS).
     * Both renders use BVH + multithreading for maximum speed.
     */
    @Test
    void testCombinedFinal() {
        // [1/2] Baseline -- no improvements
        {
            Scene scene = buildScene();
            scene.geometries.buildBVH();
            SimpleRayTracer tracer = new SimpleRayTracer(scene).setSoftShadowSamples(1);
            System.out.println("=== [1/2] Baseline -- no AA, no SS ===");
            long start = System.currentTimeMillis();
            buildCameraBuilder(scene, tracer)
                    .setAntiAliasingSamples(1).setMultithreading(-2).build()
                    .renderImage().writeToImage("mp2_combined_OFF");
            System.out.printf("Render time (OFF): %,d ms%n", System.currentTimeMillis() - start);
        }

        // [2/2] Full quality -- AA + Soft Shadows
        {
            Scene scene = buildScene();
            scene.geometries.buildBVH();
            SimpleRayTracer tracer = new SimpleRayTracer(scene)
                    .setSoftShadowSamples(FINAL_SS)
                    .setSamplingPattern(Blackboard.SamplingPattern.JITTERED);
            System.out.printf("=== [2/2] Full quality -- AA %dx%d + SS %dx%d ===%n",
                    FINAL_AA, FINAL_AA, FINAL_SS, FINAL_SS);
            long start = System.currentTimeMillis();
            buildCameraBuilder(scene, tracer)
                    .setAntiAliasingSamples(FINAL_AA).setMultithreading(-2).build()
                    .renderImage().writeToImage("mp2_combined_ON");
            System.out.printf("Render time (ON): %,d ms%n", System.currentTimeMillis() - start);
        }
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
