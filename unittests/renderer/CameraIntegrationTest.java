package renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import geometries.api.Intersectable;
import geometries.impl.Plane;
import geometries.impl.Sphere;
import geometries.impl.Triangle;
import org.junit.jupiter.api.Test;
import geometries.*;
import primitives.*;
import java.util.List;

/**
 * Integration tests for Camera ray construction
 * and Geometry intersections.
 */
class CameraIntegrationTest {

    /**
     * Helper method to count intersections of a camera's rays with a geometry.
     * Shoots rays through a 3x3 view plane and sums up the intersection points.
     *
     * @param camera   the camera to shoot rays from
     * @param geometry the intersectable geometry to test
     * @param expected the expected total number of intersections
     * @param testName the name of the test (for the assertion message)
     */
    private void assertCountIntersections(Camera camera, Intersectable geometry,
                                          int expected, String testName) {
        int count = 0;

        // עוברים על כל 9 הפיקסלים במשטח צפייה של 3x3
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                // יצירת קרן דרך הפיקסל הספציפי
                Ray ray = camera.constructRay(j, i);

                // מציאת חיתוכים של הקרן עם הגוף
                List<Point> intersections = geometry.findIntersections(ray);

                // אם יש חיתוכים, נוסיף את הכמות שלהם לספירה הכוללת
                if (intersections != null) {
                    count += intersections.size();
                }
            }
        }

        // מוודאים שסך החיתוכים תואם למצופה
        assertEquals(expected, count, testName);
    }

    /**
     * Integration tests for Camera and Sphere intersections.
     */
    @Test
    void testSphereIntersections() {
        // מצלמה בסיסית שפונה ישר לכיוון ציר ה-Z השלילי
        Camera camera1 = Camera.getBuilder()
                .setLocation(Point.ZERO)
                .setDirection(new Vector(0, 0, -1), new Vector(0, 1, 0))
                .setVpDistance(1d)
                .setVpSize(3d, 3d)
                .setResolution(3, 3)
                .build();

        // מצלמה שמוזזת מעט אחורה על ציר ה-Z
        Camera camera2 = Camera.getBuilder()
                .setLocation(new Point(0, 0, 0.5))
                .setDirection(new Vector(0, 0, -1), new Vector(0, 1, 0))
                .setVpDistance(1d)
                .setVpSize(3d, 3d)
                .setResolution(3, 3)
                .build();

        // TC01: Sphere r=1, in front of camera (2 intersections, only center ray hits)
        assertCountIntersections(camera1, new Sphere(new Point(0, 0, -3), 1d), 2,
                "TC01: Small sphere in front");

        // TC02: Sphere r=2.5, in front of camera (18 intersections, all 9 rays hit twice)
        assertCountIntersections(camera2, new Sphere(new Point(0, 0, -2.5), 2.5), 18,
                "TC02: Large sphere, all rays hit twice");

        // TC03: Sphere r=2, in front of camera (10 intersections, center and cross rays hit)
        assertCountIntersections(camera2, new Sphere(new Point(0, 0, -2), 2d), 10,
                "TC03: Medium sphere, some rays hit");

        // TC04: Sphere r=4, camera is INSIDE the sphere (9 intersections, all rays hit once)
        assertCountIntersections(camera2, new Sphere(new Point(0, 0, -1), 4d), 9,
                "TC04: Camera inside sphere");

        // TC05: Sphere r=0.5, sphere is BEHIND the camera (0 intersections)
        assertCountIntersections(camera1, new Sphere(new Point(0, 0, 1), 0.5), 0,
                "TC05: Sphere behind camera");
    }
    /**
     * Integration tests for Camera and Plane intersections.
     */
    @Test
    void testPlaneIntersections() {
        Camera camera = Camera.getBuilder()
                .setLocation(Point.ZERO)
                .setDirection(new Vector(0, 0, -1), new Vector(0, 1, 0))
                .setVpDistance(1d)
                .setVpSize(3d, 3d)
                .setResolution(3, 3)
                .build();

        // TC01: Plane is parallel to the View Plane (9 intersections)
        assertCountIntersections(camera,
                new Plane(new Point(0, 0, -5), new Vector(0, 0, 1)), 9,
                "TC01: Parallel plane");

        // TC02: Plane is slightly tilted (9 intersections)
        assertCountIntersections(camera,
                new Plane(new Point(0, 0, -5), new Vector(0, 1, 2)), 9,
                "TC02: Tilted plane, all hit");

        // TC03: Plane is highly tilted, lower rays hit, upper rays miss (6 intersections)
        assertCountIntersections(camera,
                new Plane(new Point(0, 0, -5), new Vector(0, 1, 1)), 6,
                "TC03: Highly tilted plane");
    }

    /**
     * Integration tests for Camera and Triangle intersections.
     */
    @Test
    void testTriangleIntersections() {
        Camera camera = Camera.getBuilder()
                .setLocation(Point.ZERO)
                .setDirection(new Vector(0, 0, -1), new Vector(0, 1, 0))
                .setVpDistance(1d)
                .setVpSize(3d, 3d)
                .setResolution(3, 3)
                .build();

        // TC01: Small triangle directly in front of the center pixel (1 intersection)
        assertCountIntersections(camera,
                new Triangle(new Point(1, 1, -2), new Point(-1, 1, -2), new Point(0, -1, -2)), 1,
                "TC01: Small triangle");

        // TC02: Tall and narrow triangle, crosses the center and upper-center pixels (2 intersections)
        assertCountIntersections(camera,
                new Triangle(new Point(1, 1, -2), new Point(-1, 1, -2), new Point(0, -20, -2)), 2,
                "TC02: Tall triangle");
    }
}