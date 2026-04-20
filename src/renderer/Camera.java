package renderer;

import primitives.Point;
import primitives.Ray;
import primitives.Vector;
import java.util.MissingResourceException;

/**
 * Camera class representing the view point of the scene.
 * Implements the Builder design pattern.
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

    // View plane resolution (default is 1x1)
    private int _nX = 1;
    private int _nY = 1;

    // Computed helper fields
    private Point _vpCenter;
    private double _pixelWidth;
    private double _pixelHeight;

    /** Private default constructor */
    private Camera() {}

    /**
     * Gets a new Builder object to construct a Camera.
     * @return a new Builder instance
     */
    public static Builder getBuilder() {
        return new Builder();
    }

    /**
     * Constructs a ray through a specific pixel on the view plane.
     *
     * @param j column index of the pixel (xIndex)
     * @param i row index of the pixel (yIndex)
     * @return Ray starting from camera location through the pixel
     */
    public Ray constructRay(int j, int i) {
        // Start at the center of the view plane
        Point pIJ = _vpCenter;

        // Calculate the movement from the center in pixel units
        // j is the x-axis index, i is the y-axis index
        double xJ = (j - (_nX - 1) / 2d) * _pixelWidth;
        double yI = -(i - (_nY - 1) / 2d) * _pixelHeight;

        // Move the point horizontally along vRight axis
        if (!primitives.Util.isZero(xJ)) {
            pIJ = pIJ.add(_vRight.scale(xJ));
        }

        // Move the point vertically along vUp axis
        if (!primitives.Util.isZero(yI)) {
            pIJ = pIJ.add(_vUp.scale(yI));
        }

        // The vector from the camera location to the pixel center
        Vector vIJ = pIJ.subtract(_p0);

        return new Ray(_p0, vIJ);
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    /**
     * Inner Builder class for constructing the Camera.
     */
    public static class Builder {
        // The camera instance being built
        private final Camera _camera = new Camera();

        // Temporary fields for direction calculation
        private Vector _to;
        private Vector _up;
        private Point _target;

        // Rotation angle in degrees (clockwise)
        private double _rotationAngle = 0;

        /**
         * Sets the camera location.
         * @param location the location point
         * @return this Builder
         */
        public Builder setLocation(Point location) {
            _camera._p0 = location;
            return this;
        }

        /**
         * Sets the camera direction using two vectors.
         * @param to forward direction
         * @param up general up direction
         * @return this Builder
         */
        public Builder setDirection(Vector to, Vector up) {
            this._to = to;
            this._up = up;
            return this;
        }

        /**
         * Sets the camera direction using a target point and up vector.
         * @param target point to look at
         * @param up general up direction
         * @return this Builder
         */
        public Builder setDirection(Point target, Vector up) {
            this._target = target;
            this._up = up;
            return this;
        }

        /**
         * Sets the camera direction using a target point, defaulting up to Y-axis.
         * @param target point to look at
         * @return this Builder
         */
        public Builder setDirection(Point target) {
            this._target = target;
            this._up = new Vector(0, 1, 0);
            return this;
        }

        /**
         * Sets the physical size of the view plane.
         * @param width physical width
         * @param height physical height
         * @return this Builder
         */
        public Builder setVpSize(double width, double height) {
            _camera._width = width;
            _camera._height = height;
            return this;
        }

        /**
         * Sets the distance from the camera to the view plane.
         * @param distance physical distance
         * @return this Builder
         */
        public Builder setVpDistance(double distance) {
            _camera._distance = distance;
            return this;
        }

        /**
         * Sets the resolution of the view plane (number of pixels).
         * @param nX number of pixels in width
         * @param nY number of pixels in height
         * @return this Builder
         */
        public Builder setResolution(int nX, int nY) {
            _camera._nX = nX;
            _camera._nY = nY;
            return this;
        }

        /**
         * Rotates the camera around its viewing direction (vTo).
         * @param angle rotation angle in degrees clockwise
         * @return this Builder
         */
        public Builder rotate(double angle) {
            this._rotationAngle = angle;
            return this;
        }

        /**
         * Verifies that the resolution parameters are strictly positive.
         */
        private void checkResolution() {
            if (_camera._nX <= 0 || _camera._nY <= 0) {
                throw new IllegalArgumentException("Resolution dimensions must be strictly positive");
            }
        }

        /**
         * Verifies location and direction data.
         * Calculates missing vectors, applies rotation, and ensures perfect orthogonality.
         */
        private void checkLocationAndDirection() {
            String className = "Camera";
            if (_camera._p0 == null) {
                throw new MissingResourceException("Missing camera location", className, "Location");
            }
            if (_up == null) {
                throw new MissingResourceException("Missing general up vector", className, "Up");
            }
            if (_to == null && _target == null) {
                throw new MissingResourceException("Missing target or direction", className, "Direction");
            }

            // Calculate _vTo if missing, and normalize it
            if (_to == null) {
                _camera._vTo = _target.subtract(_camera._p0).normalize();
            } else {
                _camera._vTo = _to.normalize();
            }

            // Calculate _vRight = _vTo x _up
            try {
                _camera._vRight = _camera._vTo.crossProduct(_up).normalize();
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Direction and Up vectors cannot be parallel");
            }

            // Calculate actual _vUp = _vRight x _vTo (ensures exact orthogonality)
            _camera._vUp = _camera._vRight.crossProduct(_camera._vTo).normalize();

            // Apply camera rotation around vTo if needed (Bonus)
            if (!primitives.Util.isZero(_rotationAngle)) {
                double rad = Math.toRadians(_rotationAngle);
                double cos = primitives.Util.alignZero(Math.cos(rad));
                double sin = primitives.Util.alignZero(Math.sin(rad));

                Vector origUp = _camera._vUp;
                Vector origRight = _camera._vRight;

                // Rotate vUp: origUp * cos + origRight * sin (Clockwise)
                Vector vUpCos = primitives.Util.isZero(cos) ? null : origUp.scale(cos);
                Vector vUpSin = primitives.Util.isZero(sin) ? null : origRight.scale(sin);

                if (vUpCos != null && vUpSin != null) {
                    _camera._vUp = vUpCos.add(vUpSin).normalize();
                } else if (vUpCos != null) {
                    _camera._vUp = vUpCos.normalize();
                } else if (vUpSin != null) {
                    _camera._vUp = vUpSin.normalize();
                }

                // Rotate vRight: origRight * cos - origUp * sin (Clockwise)
                Vector vRightCos = primitives.Util.isZero(cos) ? null : origRight.scale(cos);
                Vector vRightSin = primitives.Util.isZero(sin) ? null : origUp.scale(sin);

                if (vRightCos != null && vRightSin != null) {
                    _camera._vRight = vRightCos.subtract(vRightSin).normalize();
                } else if (vRightCos != null) {
                    _camera._vRight = vRightCos.normalize();
                } else if (vRightSin != null) {
                    _camera._vRight = vRightSin.scale(-1).normalize();
                }
            }
        }

        /**
         * Verifies view plane parameters and calculates helper fields.
         */
        private void checkViewPlane() {
            if (_camera._width <= 0 || _camera._height <= 0) {
                throw new IllegalArgumentException("View plane size must be strictly positive");
            }
            if (_camera._distance <= 0) {
                throw new IllegalArgumentException("View plane distance must be strictly positive");
            }

            // Calculate View Plane Center (Pc = P0 + d * Vto)
            _camera._vpCenter = _camera._p0.add(_camera._vTo.scale(_camera._distance));

            // Calculate pixel dimensions
            _camera._pixelWidth = _camera._width / _camera._nX;
            _camera._pixelHeight = _camera._height / _camera._nY;
        }

        /**
         * Final build stage: validates data, calculates missing fields,
         * and returns a cloned Camera object.
         * @return a ready-to-use Camera instance
         */
        public Camera build() {
            checkResolution();
            checkLocationAndDirection();
            checkViewPlane();
            try {
                return (Camera) _camera.clone();
            } catch (CloneNotSupportedException e) {
                return null;
            }
        }
    }
}