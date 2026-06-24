package renderer;

import primitives.*;
import scene.Scene;

import java.util.List;
import java.util.MissingResourceException;

import static primitives.Util.isZero;

/**
 * Represents the virtual camera in a ray-tracing renderer.
 *
 * <p>The camera defines the viewpoint, orientation, and view-plane geometry used to
 * construct rays into the scene. It supports super-sampling (Anti-Aliasing) via a
 * {@link Blackboard}-based beam infrastructure, and multi-threading via
 * {@link PixelManager}.</p>
 *
 * <p>Constructed exclusively via the nested {@link Builder} class (Builder pattern).</p>
 *
 * <h3>Usage example:</h3>
 * <pre>{@code
 * Camera camera = Camera.getBuilder()
 *     .setLocation(new Point(0, 0, 1000))
 *     .setDirection(new Vector(0, 0, -1), new Vector(0, 1, 0))
 *     .setVpSize(150, 150)
 *     .setVpDistance(1000)
 *     .setResolution(800, 800)
 *     .setAntiAliasingSamples(9)
 *     .setRayTracer(scene, RayTracerType.SIMPLE)
 *     .build();
 *
 * camera.renderImage().writeToImage();
 * }</pre>
 */
public class Camera implements Cloneable {

    // ========================= Camera Geometry =========================

    /** The camera's position in 3D space. */
    private Point _p0;

    /** The forward direction vector (toward the scene). */
    private Vector _vTo;

    /** The upward direction vector. */
    private Vector _vUp;

    /** The rightward direction vector (derived from vTo × vUp). */
    private Vector _vRight;

    // ========================= View Plane =========================

    /** Width of the view plane. */
    private double _width;

    /** Height of the view plane. */
    private double _height;

    /** Distance from the camera to the view plane. */
    private double _distance;

    // ========================= Resolution =========================

    /** Number of pixels along the X axis. */
    private int _nX = 1;

    /** Number of pixels along the Y axis. */
    private int _nY = 1;

    // ========================= Derived / Cached (OPTIMIZATION) =========================

    /** The center point of the view plane. Pre-computed at build time. */
    private Point _vpCenter;

    /** Width of a single pixel. Pre-computed at build time. */
    private double _pixelWidth;

    /** Height of a single pixel. Pre-computed at build time. */
    private double _pixelHeight;

    /**
     * OPTIMIZATION: Pre-computed horizontal center offset.
     * Avoids recalculating (_nX - 1) / 2d in every pixel calculation.
     * Value: (nX - 1) / 2.0
     */
    private double _centerOffsetX;

    /**
     * OPTIMIZATION: Pre-computed vertical center offset.
     * Avoids recalculating (_nY - 1) / 2d in every pixel calculation.
     * Value: (nY - 1) / 2.0
     */
    private double _centerOffsetY;

    // ========================= Rendering Infrastructure =========================

    /** The image writer used to write pixel colors to the output image. */
    private ImageWriter _imageWriter;

    /** The ray tracer used to compute the color for each ray. */
    private RayTracerBase _rayTracer;

    /** Default output file name for the rendered image. */
    private String _imageName = "default";

    // ========================= Multi-threading =========================

    /**
     * Number of threads to use for rendering.
     * <ul>
     *   <li>0  – single-threaded (default)</li>
     *   <li>-1 – parallel stream (implicit multi-threading)</li>
     *   <li>-2 – number of logical processors minus {@value #SPARE_THREADS}</li>
     *   <li>&gt;0 – explicit thread count</li>
     * </ul>
     */
    private int _threadsCount = 0;

    /**
     * Number of threads to spare for the JVM when using auto thread-count (-2).
     * Prevents thread starvation of JVM internal threads.
     */
    private static final int SPARE_THREADS = 2;

    /**
     * Progress print interval in percent (0 = disabled).
     * Controls how often the rendering progress is printed to the console.
     */
    private double _printInterval = 0;

    /** Pixel manager for synchronized pixel selection and progress reporting. */
    private PixelManager _pixelManager;

    // ========================= Super-Sampling =========================

    /**
     * Number of samples per row/column in the Anti-Aliasing grid.
     * <ul>
     *   <li>1 – super-sampling disabled (single ray per pixel)</li>
     *   <li>n – n×n grid of rays per pixel</li>
     * </ul>
     * Default: 1 (disabled).
     */
    private int _antiAliasingSamples = 1;

    /**
     * Side length of the sampling target area within each pixel.
     *
     * <p>Controls how wide the beam of rays is spread inside the pixel.
     * A value of 0 disables super-sampling regardless of {@code _antiAliasingSamples}.</p>
     *
     * <ul>
     *   <li>-1 (default) – use the pixel's physical width automatically</li>
     *   <li> 0           – super-sampling disabled (equivalent to 1 sample)</li>
     *   <li>&gt;0        – explicit area size (in world units)</li>
     * </ul>
     */
    private double _samplingAreaSize = -1;

    // ========================= Constructor =========================

    /** Private constructor — use {@link #getBuilder()} instead. */
    private Camera() {}

    /**
     * Returns a new {@link Builder} for constructing a {@code Camera} instance.
     *
     * @return a fresh {@link Builder}
     */
    public static Builder getBuilder() {
        return new Builder();
    }

    // ========================= Rendering =========================

    /**
     * Renders the full image by iterating over all pixels and casting rays.
     * Delegates to the appropriate rendering strategy based on the thread count.
     *
     * @return this camera (for method chaining)
     * @throws MissingResourceException if the image writer or ray tracer is not set
     */
    public Camera renderImage() {
        _pixelManager = new PixelManager(_nY, _nX, _printInterval);
        return switch (_threadsCount) {
            case 0  -> renderImageNoThreads();
            case -1 -> renderImageStream();
            default -> renderImageRawThreads();
        };
    }

    /**
     * Renders the image using a single thread.
     * Simple nested loop over all pixels.
     *
     * @return this camera
     */
    private Camera renderImageNoThreads() {
        for (int i = 0; i < _nY; i++) {
            for (int j = 0; j < _nX; j++) {
                castRay(j, i);
            }
        }
        return this;
    }

    /**
     * Renders the image using Java parallel streams (implicit multi-threading).
     * May cause memory issues at very high resolutions due to stream overhead.
     *
     * @return this camera
     */
    private Camera renderImageStream() {
        java.util.stream.IntStream.range(0, _nY).parallel()
                .forEach(i -> java.util.stream.IntStream.range(0, _nX).parallel()
                        .forEach(j -> castRay(j, i)));
        return this;
    }

    /**
     * Renders the image using raw Java threads with dynamic pixel assignment.
     * Each thread pulls the next available pixel from {@link PixelManager} until
     * all pixels are exhausted. This load-balancing approach ensures even work
     * distribution across threads.
     *
     * @return this camera
     */
    private Camera renderImageRawThreads() {
        var threads = new java.util.LinkedList<Thread>();

        /* Create the specified number of worker threads */
        int threadCount = _threadsCount;
        for (int t = 0; t < threadCount; t++) {
            Thread worker = new Thread(() -> {
                /* Each thread renders pixels until the pixel manager exhausts them */
                PixelManager.Pixel pixel;
                while ((pixel = _pixelManager.nextPixel()) != null) {
                    castRay(pixel.col(), pixel.row());
                }
            });
            threads.add(worker);
        }

        /* Start all worker threads */
        for (Thread thread : threads) {
            thread.start();
        }

        /* Wait for all threads to complete */
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                /* Restore the interrupted status if interrupted while waiting */
                Thread.currentThread().interrupt();
                throw new RuntimeException("Rendering was interrupted", e);
            }
        }

        return this;
    }

    /**
     * Casts a ray (or beam of rays) through pixel (j, i) and writes the resulting color.
     *
     * <p>When Anti-Aliasing is disabled ({@code _antiAliasingSamples == 1}), a single ray
     * is cast through the pixel center. When enabled, a beam of rays is generated via
     * {@link Blackboard} and their colors are averaged.</p>
     *
     * <p>This method handles:
     * <ul>
     *   <li>Pixel center calculation via {@link #getPixelCenter(int, int)}</li>
     *   <li>Sample point generation with Blackboard infrastructure</li>
     *   <li>Ray tracing and color accumulation</li>
     *   <li>Proper handling of zero-length direction vectors</li>
     * </ul>
     * </p>
     *
     * @param j column index of the pixel (0 to nX-1)
     * @param i row index of the pixel (0 to nY-1)
     */
    private void castRay(int j, int i) {
        /* Calculate the center point of this pixel on the view plane */
        Point pixelCenter = getPixelCenter(j, i);

        /*
         * Determine the sampling area size:
         *   _samplingAreaSize == -1  → use the pixel's physical width (default)
         *   _samplingAreaSize ==  0  → disabled; Blackboard returns center only
         *   _samplingAreaSize  >  0  → explicit world-unit size for sampling window
         */
        double areaSize = _samplingAreaSize < 0 ? _pixelWidth : _samplingAreaSize;

        /*
         * Generate sample points across the target area using the Blackboard.
         * When super-sampling is disabled, this returns only the center point.
         */
        List<Point> samplePoints = new Blackboard()
                .setCenter(pixelCenter)
                .setSize(areaSize)
                .setVRight(_vRight)
                .setVUp(_vUp)
                .setNumSamples(_antiAliasingSamples)
                .generatePoints();

        /*
         * Trace rays through all sample points and accumulate colors.
         * Skip any samples that produce zero-length direction vectors
         * (edge case when sample coincides with camera origin).
         */
        Color accumulatedColor = Color.BLACK;
        int tracedRays = 0;

        for (Point samplePoint : samplePoints) {
            try {
                /* Construct ray from camera through this sample point */
                Ray ray = new Ray(_p0, samplePoint.subtract(_p0));

                /* Trace ray and accumulate the resulting color */
                accumulatedColor = accumulatedColor.add(_rayTracer.traceRay(ray));
                tracedRays++;
            } catch (IllegalArgumentException e) {
                /*
                 * Zero-length direction vector (sample coincides with camera).
                 * This is rare but can occur with certain super-sampling patterns.
                 * Simply skip this sample and continue.
                 */
                // Expected exception - sample skipped
            }
        }

        /*
         * Average the accumulated color over the rays actually traced.
         * At minimum, the central pixel-center ray succeeds when the view distance > 0,
         * so tracedRays is guaranteed > 0.
         */
        Color pixelColor = accumulatedColor.reduce(tracedRays > 0 ? tracedRays : 1);

        /* Write the computed color to the image and report progress */
        _imageWriter.writePixel(j, i, pixelColor);
        _pixelManager.pixelDone();
    }

    // ========================= Ray Construction Helpers =========================

    /**
     * Computes the 3D center point of pixel (j, i) on the view plane.
     *
     * <p>The pixel center is calculated from the view-plane center by applying
     * horizontal ({@code _vRight}) and vertical ({@code _vUp}) offsets based on
     * the pixel's position in the image grid.</p>
     *
     * <p>OPTIMIZATION: Uses pre-computed center offsets ({@code _centerOffsetX},
     * {@code _centerOffsetY}) to avoid redundant calculations across millions of
     * pixel calls.</p>
     *
     * @param j column index (0-based, left to right)
     * @param i row index    (0-based, top to bottom)
     * @return the 3D center point of pixel (j, i)
     */
    private Point getPixelCenter(int j, int i) {
        /*
         * Calculate pixel position relative to the view plane center.
         * Using pre-computed center offsets (_centerOffsetX, _centerOffsetY)
         * avoids recalculating (_nX - 1) / 2d and (_nY - 1) / 2d on every call.
         *
         * This single optimization saves millions of divisions per render!
         */
        double pixelOffsetX = (j - _centerOffsetX) * _pixelWidth;
        double pixelOffsetY = -(i - _centerOffsetY) * _pixelHeight;

        /* Start from the view plane center */
        Point center = _vpCenter;

        /*
         * Apply horizontal offset using the right vector only if non-zero.
         * The isZero() guard prevents unnecessary vector operations.
         */
        if (!isZero(pixelOffsetX)) {
            center = center.add(_vRight.scale(pixelOffsetX));
        }

        /*
         * Apply vertical offset using the up vector only if non-zero.
         * The negative sign above accounts for the inversion of screen coordinates.
         */
        if (!isZero(pixelOffsetY)) {
            center = center.add(_vUp.scale(pixelOffsetY));
        }

        return center;
    }

    /**
     * Constructs a single ray from the camera through the center of pixel (j, i).
     *
     * <p>This method is retained for compatibility with tests that construct
     * rays directly (e.g., unit tests for ray construction geometry).</p>
     *
     * @param j column index
     * @param i row index
     * @return the ray from {@code _p0} through the center of pixel (j, i)
     */
    public Ray constructRay(int j, int i) {
        return new Ray(_p0, getPixelCenter(j, i).subtract(_p0));
    }

    // ========================= Image Output =========================

    /**
     * Paints a debug grid onto the image using the specified interval and color.
     * Grid lines are drawn at every {@code interval}-th row and column.
     *
     * @param interval pixel gap between grid lines (must be positive)
     * @param color    color of the grid lines
     * @return this camera (for method chaining)
     */
    public Camera printGrid(int interval, Color color) {
        for (int i = 0; i < _nY; i++) {
            for (int j = 0; j < _nX; j++) {
                if (i % interval == 0 || j % interval == 0) {
                    _imageWriter.writePixel(j, i, color);
                }
            }
        }
        return this;
    }

    /**
     * Writes the rendered image to a file using the camera's default image name.
     *
     * @throws MissingResourceException if the image writer has not been initialized
     */
    public void writeToImage() {
        if (_imageWriter == null) {
            throw new MissingResourceException("Missing ImageWriter", "Camera", "imageWriter");
        }
        _imageWriter.writeToImage(_imageName);
    }

    /**
     * Writes the rendered image to a file with the given name.
     * Provided for compatibility with legacy tests that pass an explicit file name.
     *
     * @param fileName the output file name (without extension)
     * @throws MissingResourceException if the image writer has not been initialized
     */
    public void writeToImage(String fileName) {
        if (_imageWriter == null) {
            throw new MissingResourceException("Missing ImageWriter", "Camera", "imageWriter");
        }
        _imageWriter.writeToImage(fileName);
    }

    // ========================= Cloneable =========================

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    // ========================= Builder =========================

    /**
     * Fluent builder for constructing a {@link Camera} instance.
     *
     * <p>All mandatory parameters must be set before calling {@link #build()}.
     * Optional parameters (e.g., anti-aliasing, multi-threading) have sensible defaults.</p>
     */
    public static class Builder {

        /** The camera under construction. */
        private final Camera _camera = new Camera();

        /** Explicit direction vector (alternative to target point). */
        private Vector _to;

        /** Up vector supplied by the caller. */
        private Vector _up;

        /** Optional target point — used to derive {@code vTo}. */
        private Point _target;

        /** Optional rotation angle (degrees) around the {@code vTo} axis. */
        private double _rotationAngle = 0;

        // -------- Location & Orientation --------

        /**
         * Sets the camera's position in world space.
         *
         * @param location the camera origin point
         * @return this builder
         */
        public Builder setLocation(Point location) {
            _camera._p0 = location;
            return this;
        }

        /**
         * Sets the camera's orientation using an explicit forward and up vector.
         *
         * @param to  the forward direction (need not be normalized)
         * @param up  the upward direction (need not be normalized)
         * @return this builder
         */
        public Builder setDirection(Vector to, Vector up) {
            _to = to;
            _up = up;
            return this;
        }

        /**
         * Sets the camera's orientation by pointing at a target point, with a given up vector.
         *
         * @param target the point the camera looks at
         * @param up     the upward direction
         * @return this builder
         */
        public Builder setDirection(Point target, Vector up) {
            _target = target;
            _up = up;
            return this;
        }

        /**
         * Sets the camera's orientation by pointing at a target point.
         * The up vector defaults to {@code (0, 1, 0)}.
         *
         * @param target the point the camera looks at
         * @return this builder
         */
        public Builder setDirection(Point target) {
            _target = target;
            _up = new Vector(0, 1, 0);
            return this;
        }

        // -------- View Plane --------

        /**
         * Sets the physical size of the view plane.
         *
         * @param width  view-plane width  (must be positive)
         * @param height view-plane height (must be positive)
         * @return this builder
         */
        public Builder setVpSize(double width, double height) {
            _camera._width  = width;
            _camera._height = height;
            return this;
        }

        /**
         * Sets the distance from the camera to the view plane.
         *
         * @param distance distance (must be positive)
         * @return this builder
         */
        public Builder setVpDistance(double distance) {
            _camera._distance = distance;
            return this;
        }

        // -------- Resolution --------

        /**
         * Sets the output image resolution in pixels.
         *
         * @param nX number of columns (must be positive)
         * @param nY number of rows    (must be positive)
         * @return this builder
         */
        public Builder setResolution(int nX, int nY) {
            _camera._nX = nX;
            _camera._nY = nY;
            return this;
        }

        // -------- Output --------

        /**
         * Sets the default output file name for the rendered image.
         *
         * @param name file name (without extension)
         * @return this builder
         */
        public Builder setImageName(String name) {
            _camera._imageName = name;
            return this;
        }

        // -------- Ray Tracer --------

        /**
         * Sets the ray tracer by providing a pre-constructed instance.
         *
         * @param scene     the scene (unused here, kept for API symmetry)
         * @param rayTracer the ray tracer instance
         * @return this builder
         */
        public Builder setRayTracer(Scene scene, RayTracerBase rayTracer) {
            _camera._rayTracer = rayTracer;
            return this;
        }

        /**
         * Sets the ray tracer by type.
         *
         * @param scene the scene to render
         * @param type  the ray tracer type (e.g., {@link RayTracerType#SIMPLE})
         * @return this builder
         * @throws IllegalArgumentException if the type is unknown
         */
        public Builder setRayTracer(Scene scene, RayTracerType type) {
            if (type == RayTracerType.SIMPLE) {
                _camera._rayTracer = new SimpleRayTracer(scene);
            } else {
                throw new IllegalArgumentException("Unknown RayTracerType: " + type);
            }
            return this;
        }

        // -------- Anti-Aliasing --------

        /**
         * Sets the number of samples per dimension for Anti-Aliasing (super-sampling).
         *
         * <p>The total number of rays cast per pixel will be {@code samples × samples}.
         * Set to 1 (the default) to disable Anti-Aliasing.</p>
         *
         * <ul>
         *   <li>1  – disabled (single ray per pixel)</li>
         *   <li>3  – 3×3 = 9 rays (debug quality)</li>
         *   <li>9  – 9×9 = 81 rays (demo quality)</li>
         *   <li>17 – 17×17 ≈ 289 rays (production quality)</li>
         * </ul>
         *
         * @param samples number of samples per row/column (must be ≥ 1)
         * @return this builder
         * @throws IllegalArgumentException if {@code samples < 1}
         */
        public Builder setAntiAliasingSamples(int samples) {
            if (samples < 1) {
                throw new IllegalArgumentException("Anti-aliasing samples must be at least 1");
            }
            _camera._antiAliasingSamples = samples;
            return this;
        }

        /**
         * Sets the side length of the sampling target area within each pixel.
         *
         * <p>This controls how wide the beam of rays spreads inside the pixel.
         * Placing this parameter in the Camera Builder is correct by RDD: it describes
         * <em>how the camera fires rays</em>, which is the camera's responsibility.</p>
         *
         * <ul>
         *   <li>Omitting this call (default) → area equals the pixel's physical width</li>
         *   <li>0 → disables super-sampling (beam collapses to a single central ray),
         *       regardless of the sample count set via {@link #setAntiAliasingSamples}</li>
         *   <li>&gt;0 → explicit world-unit size for the sampling window</li>
         * </ul>
         *
         * @param size side length of the sampling area (must be ≥ 0)
         * @return this builder
         * @throws IllegalArgumentException if {@code size < 0}
         */
        public Builder setSamplingAreaSize(double size) {
            if (size < 0) {
                throw new IllegalArgumentException("Sampling area size must be non-negative");
            }
            _camera._samplingAreaSize = size;
            return this;
        }

        // -------- Multi-threading --------

        /**
         * Sets the number of threads to use for rendering.
         *
         * <ul>
         *   <li>0  – single-threaded (default)</li>
         *   <li>-1 – parallel stream (implicit multi-threading)</li>
         *   <li>-2 – logical processors minus {@value Camera#SPARE_THREADS} spare threads</li>
         *   <li>&gt;0 – explicit thread count</li>
         * </ul>
         *
         * @param threads thread count (must be ≥ -2)
         * @return this builder
         * @throws IllegalArgumentException if {@code threads < -2}
         */
        public Builder setMultithreading(int threads) {
            if (threads < -2) {
                throw new IllegalArgumentException("Multithreading parameter must be -2 or higher");
            }
            if (threads == -2) {
                int cores = Runtime.getRuntime().availableProcessors() - SPARE_THREADS;
                _camera._threadsCount = Math.max(1, cores);
            } else {
                _camera._threadsCount = threads;
            }
            return this;
        }

        /**
         * Sets the progress print interval as a percentage.
         * Set to 0 (the default) to disable progress printing.
         *
         * @param interval printing interval in percent (0 = disabled)
         * @return this builder
         * @throws IllegalArgumentException if {@code interval < 0}
         */
        public Builder setDebugPrint(double interval) {
            if (interval < 0) {
                throw new IllegalArgumentException("Interval must be non-negative");
            }
            _camera._printInterval = interval;
            return this;
        }

        // -------- Optional: Camera Rotation --------

        /**
         * Rotates the camera around its forward ({@code vTo}) axis by the given angle.
         *
         * @param angle rotation angle in degrees
         * @return this builder
         */
        public Builder rotate(double angle) {
            _rotationAngle = angle;
            return this;
        }

        // -------- Build --------

        /**
         * Validates all parameters and constructs the immutable {@link Camera} instance.
         *
         * @return the constructed camera
         * @throws MissingResourceException  if mandatory parameters are missing
         * @throws IllegalArgumentException  if any parameter value is invalid
         */
        public Camera build() {
            /* Ray tracer — default to SimpleRayTracer if not set */
            if (_camera._rayTracer == null) {
                setRayTracer(new Scene("default"), RayTracerType.SIMPLE);
            }

            /* Resolution and ImageWriter */
            if (_camera._nX <= 0 || _camera._nY <= 0) {
                throw new IllegalArgumentException("Resolution must be positive");
            }
            _camera._imageWriter = new ImageWriter(_camera._nX, _camera._nY);

            /* Camera position and orientation */
            if (_camera._p0 == null || _up == null) {
                throw new MissingResourceException("Missing camera parameters", "Camera", "build");
            }

            /* Derive vTo from explicit vector or target point */
            if (_to != null) {
                _camera._vTo = _to.normalize();
            } else if (_target != null) {
                _camera._vTo = _target.subtract(_camera._p0).normalize();
            } else {
                throw new MissingResourceException("Missing direction", "Camera", "vTo");
            }

            /* Orthonormal basis: vRight = vTo × vUp, then re-derive vUp for orthogonality */
            _camera._vRight = _camera._vTo.crossProduct(_up).normalize();
            _camera._vUp    = _camera._vRight.crossProduct(_camera._vTo).normalize();

            /* Optional camera rotation around vTo */
            if (!isZero(_rotationAngle)) {
                double rad   = Math.toRadians(_rotationAngle);
                double cos   = Util.alignZero(Math.cos(rad));
                double sin   = Util.alignZero(Math.sin(rad));
                Vector vUpOrig    = _camera._vUp;
                Vector vRightOrig = _camera._vRight;

                if (isZero(sin)) {
                    /* 0° or 180° — scale only, no cross terms */
                    _camera._vUp    = vUpOrig.scale(cos);
                    _camera._vRight = vRightOrig.scale(cos);
                } else if (isZero(cos)) {
                    /* 90° or 270° — pure swap */
                    _camera._vUp    = vRightOrig.scale(sin);
                    _camera._vRight = vUpOrig.scale(-sin);
                } else {
                    /* General rotation */
                    _camera._vUp    = vUpOrig.scale(cos).add(vRightOrig.scale(sin)).normalize();
                    _camera._vRight = vRightOrig.scale(cos).subtract(vUpOrig.scale(sin)).normalize();
                }
            }

            /* View plane dimensions */
            if (_camera._width <= 0 || _camera._height <= 0 || _camera._distance <= 0) {
                throw new IllegalArgumentException("View-plane dimensions and distance must be positive");
            }

            _camera._vpCenter     = _camera._p0.add(_camera._vTo.scale(_camera._distance));
            _camera._pixelWidth   = _camera._width  / _camera._nX;
            _camera._pixelHeight  = _camera._height / _camera._nY;

            /*
             * OPTIMIZATION: Pre-compute center offsets to avoid recalculating
             * (_nX - 1) / 2d and (_nY - 1) / 2d millions of times during rendering.
             */
            _camera._centerOffsetX = (_camera._nX - 1) / 2.0;
            _camera._centerOffsetY = (_camera._nY - 1) / 2.0;

            try {
                return (Camera) _camera.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException("Camera build failed", e);
            }
        }
    }
}