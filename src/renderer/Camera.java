package renderer;

import primitives.*;
import scene.Scene;
import java.util.MissingResourceException;

/**
 * Camera class representing the view point of the scene.
 * Implemented according to Stage 5 instructions with Builder pattern.
 */
public class Camera implements Cloneable {
    // Camera location and directions
    private Point _p0;
    private Vector _vTo;
    private Vector _vUp;
    private Vector _vRight;

    // View plane geometry
    private double _width;
    private double _height;
    private double _distance;

    // View plane resolution
    private int _nX = 1;
    private int _nY = 1;

    // Computed helper fields
    private Point _vpCenter;
    private double _pixelWidth;
    private double _pixelHeight;

    // Rendering infrastructure fields
    private ImageWriter _imageWriter;
    private RayTracerBase _rayTracer;
    private String _imageName = "default";

    /** Private default constructor for Builder */
    private Camera() {}

    /**
     * Static factory method for Builder
     * @return a new Builder object
     */
    public static Builder getBuilder() {
        return new Builder();
    }

    // --- Rendering Operations ---

    /**
     * Renders the image by iterating over all pixels.
     * @return the camera itself
     */
    public Camera renderImage() {
        for (int i = 0; i < _nY; i++) {
            for (int j = 0; j < _nX; j++) {
                castRay(j, i);
            }
        }
        return this;
    }

    /**
     * Helper method to cast a ray and write the pixel color.
     * @param j column index
     * @param i row index
     */
    private void castRay(int j, int i) {
        Ray ray = constructRay(j, i);
        Color pixelColor = _rayTracer.traceRay(ray);
        _imageWriter.writePixel(j, i, pixelColor);
    }

    /**
     * Prints a grid onto the image.
     * @param interval gap between grid lines
     * @param color grid line color
     * @return the camera itself
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
     * Writes the image to a file using the internal image name.
     */
    public void writeToImage() {
        if (_imageWriter == null)
            throw new MissingResourceException("Missing ImageWriter", "Camera", "imageWriter");
        _imageWriter.writeToImage(_imageName);
    }

    /**
     * Writes the image to a file with a specific name (To support legacy tests).
     * @param fileName the name of the file
     */
    public void writeToImage(String fileName) {
        if (_imageWriter == null)
            throw new MissingResourceException("Missing ImageWriter", "Camera", "imageWriter");
        _imageWriter.writeToImage(fileName);
    }

    /**
     * Constructs a ray through the center of a given pixel.
     * @param j column index
     * @param i row index
     * @return the ray from camera through pixel (j,i)
     */
    public Ray constructRay(int j, int i) {
        Point pIJ = _vpCenter;
        double xJ = (j - (_nX - 1) / 2d) * _pixelWidth;
        double yI = -(i - (_nY - 1) / 2d) * _pixelHeight;

        if (!Util.isZero(xJ)) pIJ = pIJ.add(_vRight.scale(xJ));
        if (!Util.isZero(yI)) pIJ = pIJ.add(_vUp.scale(yI));

        return new Ray(_p0, pIJ.subtract(_p0));
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    // --- Inner Builder Class ---

    public static class Builder {
        private final Camera _camera = new Camera();
        private Vector _to, _up;
        private Point _target;
        private double _rotationAngle = 0;

        public Builder setLocation(Point location) {
            _camera._p0 = location;
            return this;
        }

        public Builder setDirection(Vector to, Vector up) {
            this._to = to;
            this._up = up;
            return this;
        }

        public Builder setDirection(Point target, Vector up) {
            this._target = target;
            this._up = up;
            return this;
        }

        public Builder setDirection(Point target) {
            this._target = target;
            this._up = new Vector(0, 1, 0);
            return this;
        }

        public Builder setVpSize(double width, double height) {
            _camera._width = width;
            _camera._height = height;
            return this;
        }

        public Builder setVpDistance(double distance) {
            _camera._distance = distance;
            return this;
        }

        public Builder setResolution(int nX, int nY) {
            _camera._nX = nX;
            _camera._nY = nY;
            return this;
        }

        public Builder setImageName(String name) {
            _camera._imageName = name;
            return this;
        }

        public Builder rotate(double angle) {
            this._rotationAngle = angle;
            return this;
        }

        public Builder setRayTracer(Scene scene, RayTracerBase rayTracer) {
            _camera._rayTracer = rayTracer;
            return this;
        }

        public Builder setRayTracer(Scene scene, RayTracerType type) {
            if (type == RayTracerType.SIMPLE) {
                _camera._rayTracer = new SimpleRayTracer(scene);
            } else {
                throw new IllegalArgumentException("Unknown RayTracerType");
            }
            return this;
        }

        public Camera build() {
            // Stage 5 RayTracer check
            if (_camera._rayTracer == null) {
                setRayTracer(new Scene("test"), RayTracerType.SIMPLE);
            }

            // Resolution and ImageWriter
            if (_camera._nX <= 0 || _camera._nY <= 0)
                throw new IllegalArgumentException("Resolution must be positive");
            _camera._imageWriter = new ImageWriter(_camera._nX, _camera._nY);

            // Orientation setup
            if (_camera._p0 == null || _up == null)
                throw new MissingResourceException("Missing camera params", "Camera", "build");

            if (_to != null) {
                _camera._vTo = _to.normalize();
            } else if (_target != null) {
                _camera._vTo = _target.subtract(_camera._p0).normalize();
            } else {
                throw new MissingResourceException("Missing direction", "Camera", "Direction");
            }

            _camera._vRight = _camera._vTo.crossProduct(_up).normalize();
            _camera._vUp = _camera._vRight.crossProduct(_camera._vTo).normalize();

            // Bonus: Camera rotation around vTo with safety checks for Zero Vector
            if (!Util.isZero(_rotationAngle)) {
                double rad = Math.toRadians(_rotationAngle);
                double cos = Util.alignZero(Math.cos(rad));
                double sin = Util.alignZero(Math.sin(rad));
                Vector vUpOrig = _camera._vUp;
                Vector vRightOrig = _camera._vRight;

                if (Util.isZero(sin)) { // Rotation by 0 or 180 degrees
                    _camera._vUp = vUpOrig.scale(cos);
                    _camera._vRight = vRightOrig.scale(cos);
                } else if (Util.isZero(cos)) { // Rotation by 90 or 270 degrees
                    _camera._vUp = vRightOrig.scale(sin);
                    _camera._vRight = vUpOrig.scale(-sin);
                } else { // Combined rotation
                    _camera._vUp = vUpOrig.scale(cos).add(vRightOrig.scale(sin)).normalize();
                    _camera._vRight = vRightOrig.scale(cos).subtract(vUpOrig.scale(sin)).normalize();
                }
            }

            // View Plane dimensions
            if (_camera._width <= 0 || _camera._height <= 0 || _camera._distance <= 0)
                throw new IllegalArgumentException("VP dimensions must be positive");

            _camera._vpCenter = _camera._p0.add(_camera._vTo.scale(_camera._distance));
            _camera._pixelWidth = _camera._width / _camera._nX;
            _camera._pixelHeight = _camera._height / _camera._nY;

            try {
                return (Camera) _camera.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException("Build failed", e);
            }
        }
    }
}