package renderer;

import static java.awt.Color.BLUE;
import static java.awt.Color.RED;

import geometries.impl.*;
import lighting.PointLight;
import org.junit.jupiter.api.Test;

import lighting.AmbientLight;
import lighting.SpotLight;
import primitives.*;
import scene.Scene;

/**
 * Tests for reflection and transparency functionality, test for partial
 * shadows
 * (with transparency)
 * @author Dan Zilberstein
 */
class TransparencyReflectionTests {
   /** Default constructor to satisfy JavaDoc generator */
   TransparencyReflectionTests() { /* to satisfy JavaDoc generator */ }

   /** Scene for the tests */
   private final Scene          _scene         = new Scene("Test scene");
   /** Camera builder for the tests with triangles */
   private final Camera.Builder _cameraBuilder = Camera.getBuilder()     //
      .setRayTracer(_scene, RayTracerType.SIMPLE);

   /** Produce a picture of a sphere lighted by a spot light */
   @Test
   @SuppressWarnings("java:S109")
   void testTwoSpheres() {
      _scene.geometries.add( //
                            new Sphere(new Point(0, 0, -50), 50D).setEmission(new Color(BLUE)) //
                               .setMaterial(new Material().setKD(0.4).setKS(0.3).setShininess(100).setKT(0.3)), //
                            new Sphere(new Point(0, 0, -50), 25D).setEmission(new Color(RED)) //
                               .setMaterial(new Material().setKD(0.5).setKS(0.5).setShininess(100))); //
      _scene.lights.add( //
                        new SpotLight(new Color(1000, 600, 0), new Point(-100, -100, 500), new Vector(-1, -1, -2)) //
                           .setKl(0.0004).setKq(0.0000006));

      _cameraBuilder
         .setLocation(new Point(0, 0, 1000)) //
         .setDirection(Point.ZERO, Vector.AXIS_Y) //
         .setVpDistance(1000).setVpSize(150, 150) //
         .setResolution(500, 500) //
         .build() //
         .renderImage() //
         .writeToImage("refractionTwoSpheres");
   }

   /** Produce a picture of a sphere lighted by a spot light */
   @Test
   @SuppressWarnings("java:S109")
   void testTwoSpheresOnMirrors() {
      _scene.geometries.add( //
                            new Sphere(new Point(-950, -900, -1000), 400D).setEmission(new Color(0, 50, 100)) //
                               .setMaterial(new Material().setKD(0.25).setKS(0.25).setShininess(20) //
                                  .setKT(new Double3(0.5, 0, 0))), //
                            new Sphere(new Point(-950, -900, -1000), 200D).setEmission(new Color(100, 50, 20)) //
                               .setMaterial(new Material().setKD(0.25).setKS(0.25).setShininess(20)), //
                            new Triangle(new Point(1500, -1500, -1500), new Point(-1500, 1500, -1500), //
                                         new Point(670, 670, 3000)) //
                               .setEmission(new Color(20, 20, 20)) //
                               .setMaterial(new Material().setKR(1)), //
                            new Triangle(new Point(1500, -1500, -1500), new Point(-1500, 1500, -1500), //
                                         new Point(-1500, -1500, -2000)) //
                               .setEmission(new Color(20, 20, 20)) //
                               .setMaterial(new Material().setKR(new Double3(0.5, 0, 0.4))));
      _scene.setAmbientLight(new AmbientLight(new Color(26, 26, 26)));
      _scene.lights.add(new SpotLight(new Color(1020, 400, 400), new Point(-750, -750, -150), new Vector(-1, -1, -4)) //
         .setKl(0.00001).setKq(0.000005));

      _cameraBuilder
         .setLocation(new Point(0, 0, 10000)) //
         .setDirection(Point.ZERO, Vector.AXIS_Y) //
         .setVpDistance(10000).setVpSize(2500, 2500) //
         .setResolution(500, 500) //
         .build() //
         .renderImage() //
         .writeToImage("reflectionTwoSpheresMirrored");
   }

   /**
    * Produce a picture of a two triangles lighted by a spot light with a
    * partially
    * transparent Sphere producing partial shadow
    */
   @Test
   @SuppressWarnings("java:S109")
   void testTrianglesTransparentSphere() {
      _scene.geometries.add(
                            new Triangle(new Point(-150, -150, -115), new Point(150, -150, -135),
                                         new Point(75, 75, -150))
                               .setMaterial(new Material().setKD(0.5).setKS(0.5).setShininess(60)),
                            new Triangle(new Point(-150, -150, -115), new Point(-70, 70, -140), new Point(75, 75, -150))
                               .setMaterial(new Material().setKD(0.5).setKS(0.5).setShininess(60)),
                            new Sphere(new Point(60, 50, -50), 30D).setEmission(new Color(BLUE))
                               .setMaterial(new Material().setKD(0.2).setKS(0.2).setShininess(30).setKT(0.6)));
      _scene.setAmbientLight(new AmbientLight(new Color(38, 38, 38)));
      _scene.lights.add(
                        new SpotLight(new Color(700, 400, 400), new Point(60, 50, 0), new Vector(0, 0, -1))
                           .setKl(4E-5).setKq(2E-7));

      _cameraBuilder
         .setLocation(new Point(0, 0, 1000)) //
         .setDirection(Point.ZERO, Vector.AXIS_Y) //
         .setVpDistance(1000).setVpSize(200, 200) //
         .setResolution(600, 600) //
         .build() //
         .renderImage() //
         .writeToImage("refractionShadow");
   }
   @Test
   void testMyCustomImage() {

      // ── Reflective floor ──────────────────────────────────────────────────
      _scene.geometries.add(
              new Triangle(
                      new Point(-300, -60, 100),
                      new Point( 300, -60, 100),
                      new Point( 300, -60, -400))
                      .setEmission(new Color(8, 8, 18))
                      .setMaterial(new Material()
                              .setKD(0.25).setKS(0.5).setShininess(80).setKR(0.35)),
              new Triangle(
                      new Point(-300, -60, 100),
                      new Point( 300, -60, -400),
                      new Point(-300, -60, -400))
                      .setEmission(new Color(8, 8, 18))
                      .setMaterial(new Material()
                              .setKD(0.25).setKS(0.5).setShininess(80).setKR(0.35))
      );

      // ── Back wall ─────────────────────────────────────────────────────────
      _scene.geometries.add(
              new Triangle(
                      new Point(-300, -60,  -380),
                      new Point( 300, -60,  -380),
                      new Point( 300,  300, -380))
                      .setEmission(new Color(20, 15, 35))
                      .setMaterial(new Material()
                              .setKD(0.6).setKS(0.1).setShininess(10)),
              new Triangle(
                      new Point(-300, -60,  -380),
                      new Point( 300,  300, -380),
                      new Point(-300,  300, -380))
                      .setEmission(new Color(20, 15, 35))
                      .setMaterial(new Material()
                              .setKD(0.6).setKS(0.1).setShininess(10))
      );

      // ── Large transparent blue sphere – sits on floor (center) ───────────
      // radius=65, floor y=-60, so center y = -60+65 = 5
      _scene.geometries.add(
              new Sphere(new Point(0, 5, -180), 65D)
                      .setEmission(new Color(0, 15, 50))
                      .setMaterial(new Material()
                              .setKD(0.2).setKS(0.3).setShininess(50).setKT(0.7))
      );

      // ── Red inner sphere ──────────────────────────────────────────────────
      _scene.geometries.add(
              new Sphere(new Point(0, 5, -180), 30D)
                      .setEmission(new Color(160, 20, 20))
                      .setMaterial(new Material()
                              .setKD(0.5).setKS(0.5).setShininess(80))
      );

      // ── Mirror sphere – right side, sits on floor ─────────────────────────
      // radius=55, center y = -60+55 = -5
      _scene.geometries.add(
              new Sphere(new Point(160, -5, -160), 55D)
                      .setEmission(new Color(5, 5, 5))
                      .setMaterial(new Material()
                              .setKD(0.05).setKS(0.1).setShininess(100).setKR(0.85))
      );

      // ── Gold triangle – left, tip pointing up ────────────────────────────
      _scene.geometries.add(
              new Triangle(
                      new Point(-180, -60, -150),
                      new Point(-100, -60, -150),
                      new Point(-140,  80, -180))
                      .setEmission(new Color(130, 100, 10))
                      .setMaterial(new Material()
                              .setKD(0.5).setKS(0.6).setShininess(50))
      );

      // ── Small green sphere – left foreground, sits on floor ───────────────
      // radius=28, center y = -60+28 = -32
      _scene.geometries.add(
              new Sphere(new Point(-155, -32, -110), 28D)
                      .setEmission(new Color(10, 70, 25))
                      .setMaterial(new Material()
                              .setKD(0.5).setKS(0.4).setShininess(40))
      );

      // ── Ambient light ─────────────────────────────────────────────────────
      _scene.setAmbientLight(
              new AmbientLight(new Color(10, 10, 20), new Double3(1)));

      // ── Warm key light – upper left ───────────────────────────────────────
      _scene.lights.add(
              new SpotLight(
                      new Color(380, 260, 110),
                      new Point(-150, 250, 100),
                      new Vector(1, -1, -1))
                      .setKl(3E-5).setKq(1E-6));

      // ── Cool fill light – upper right ────────────────────────────────────
      _scene.lights.add(
              new SpotLight(
                      new Color(80, 150, 420),
                      new Point(200, 200, 80),
                      new Vector(-1, -1, -1))
                      .setKl(3E-5).setKq(1E-6));

      // ── Camera ────────────────────────────────────────────────────────────
      _cameraBuilder
              .setLocation(new Point(0, 100, 350))
              .setDirection(new Point(0, -30, -180), Vector.AXIS_Y)
              .setVpDistance(270)
              .setVpSize(220, 220)
              .setResolution(600, 600)
              .build()
              .renderImage()
              .writeToImage("myCustomImage_Stage8");
   }
   /**
    * Bonus 1: Impressive scene with 10+ different geometries demonstrating
    * all implemented geometry types: Plane, Polygon, Sphere, Triangle, Cylinder, Tube.
    * Shows global effects: reflection, transparency, shadows with partial transparency.
    * Uses two light sources (warm + cool) for realistic depth and shading.
    */
   @Test
   void testMegaBonusAllGeometries() {

      // ── Floor plane – reflective mirror surface ──────────────────────────
      _scene.geometries.add(
              new Plane(new Point(0, -50, 0), new Vector(0, 1, 0))
                      .setEmission(new Color(10, 10, 20))
                      .setMaterial(new Material()
                              .setKD(0.3).setKS(0.5).setShininess(80).setKR(0.4))
      );

      // ── Back wall – large polygon as backdrop ────────────────────────────
      _scene.geometries.add(
              new Polygon(
                      new Point(-200, -50, -200),
                      new Point( 200, -50, -200),
                      new Point( 200, 200, -200),
                      new Point(-200, 200, -200))
                      .setEmission(new Color(25, 15, 35))
                      .setMaterial(new Material()
                              .setKD(0.6).setKS(0.2).setShininess(20))
      );

      // ── Left pillar – cylinder with slight reflection ────────────────────
      _scene.geometries.add(
              new Cylinder(
                      new Ray(new Point(-60, -50, -100), new Vector(0, 1, 0)), 12, 100)
                      .setEmission(new Color(60, 40, 80))
                      .setMaterial(new Material()
                              .setKD(0.4).setKS(0.7).setShininess(60).setKR(0.2))
      );

      // ── Large transparent sphere – outer shell (blue tint) ───────────────
      // Note: kT only, no kR on a closed shape to avoid infinite ray trapping
      _scene.geometries.add(
              new Sphere(new Point(30, 10, -80), 45D)
                      .setEmission(new Color(0, 20, 60))
                      .setMaterial(new Material()
                              .setKD(0.2).setKS(0.3).setShininess(50).setKT(0.75))
      );

      // ── Inner sphere – red core visible through transparent outer sphere ──
      _scene.geometries.add(
              new Sphere(new Point(30, 10, -80), 20D)
                      .setEmission(new Color(180, 30, 30))
                      .setMaterial(new Material()
                              .setKD(0.5).setKS(0.5).setShininess(80))
      );

      // ── Mirror sphere – high reflection, right side ──────────────────────
      // Note: kR only, no kT on a closed shape
      _scene.geometries.add(
              new Sphere(new Point(120, -10, -60), 35D)
                      .setEmission(new Color(5, 5, 5))
                      .setMaterial(new Material()
                              .setKD(0.05).setKS(0.1).setShininess(120).setKR(0.9))
      );

      // ── Green matte sphere – left foreground ─────────────────────────────
      _scene.geometries.add(
              new Sphere(new Point(-100, -25, -30), 25D)
                      .setEmission(new Color(10, 60, 30))
                      .setMaterial(new Material()
                              .setKD(0.5).setKS(0.4).setShininess(40))
      );

      // ── Small transparent purple sphere – right foreground ───────────────
      _scene.geometries.add(
              new Sphere(new Point(90, -30, -20), 20D)
                      .setEmission(new Color(60, 0, 80))
                      .setMaterial(new Material()
                              .setKD(0.3).setKS(0.3).setShininess(40).setKT(0.5))
      );

      // ── Gold triangle – right side, casts shadow on floor ────────────────
      _scene.geometries.add(
              new Triangle(
                      new Point(100, -50, -50),
                      new Point(160, -50, -50),
                      new Point(130,  30, -70))
                      .setEmission(new Color(120, 90, 10))
                      .setMaterial(new Material()
                              .setKD(0.5).setKS(0.6).setShininess(50))
      );

      // ── Cyan triangle – upper left, decorative background element ────────
      _scene.geometries.add(
              new Triangle(
                      new Point(-140, 100, -180),
                      new Point( -80, 100, -180),
                      new Point(-110, 160, -180))
                      .setEmission(new Color(0, 80, 100))
                      .setMaterial(new Material()
                              .setKD(0.5).setKS(0.5).setShininess(30))
      );

      // ── Horizontal tube – subtle background accent ────────────────────────
      // Kept short and far back to avoid dominating the composition
      _scene.geometries.add(
              new Tube(5, new Ray(new Point(-50, 60, -150), new Vector(1, 0, 0)))
                      .setEmission(new Color(80, 50, 20))
                      .setMaterial(new Material()
                              .setKD(0.4).setKS(0.4).setShininess(20))
      );

      // ── Ambient light – very subtle cool tint ────────────────────────────
      _scene.setAmbientLight(
              new AmbientLight(new Color(15, 15, 25), new Double3(1)));

      // ── Warm spotlight – upper left, main key light ───────────────────────
      _scene.lights.add(
              new SpotLight(
                      new Color(300, 200, 100),
                      new Point(-150, 200, 100),
                      new Vector(1, -1, -1))
                      .setKl(4E-5).setKq(2E-6));

      // ── Cool spotlight – upper right, fill light for contrast ────────────
      _scene.lights.add(
              new SpotLight(
                      new Color(80, 130, 280),
                      new Point(200, 180, 50),
                      new Vector(-1, -1, -1))
                      .setKl(4E-5).setKq(2E-6));

      // ── Soft point light – behind scene, adds depth to background ────────
      _scene.lights.add(
              new PointLight(new Color(60, 50, 90), new Point(0, 50, -180))
                      .setKl(8E-5).setKq(4E-6));

      // ── Camera – slightly elevated, looking down toward scene center ──────
      _cameraBuilder
              .setLocation(new Point(0, 60, 300))
              .setDirection(new Point(20, -10, -80), Vector.AXIS_Y)
              .setVpDistance(250)
              .setVpSize(220, 220)
              .setResolution(800, 800)
              .build()
              .renderImage()
              .writeToImage("megaBonus_AllGeometries");
   }
   /**
    * Bonus 2: Same scene from Bonus 1 viewed from different angles and distances.
    * Uses camera rotation (rotate()) implemented in Stage 4 bonus.
    * Each render shows the scene from a unique viewpoint to demonstrate
    * the camera's full range of motion.
    */
   @Test
   void testMegaBonusMultipleAngles() {
      buildMegaScene();



      // ── View 2: Right side, closer, rotated 25 degrees ───────────────────
      _cameraBuilder
              .setLocation(new Point(200, 80, 150))
              .setDirection(new Point(-20, -10, -80), Vector.AXIS_Y)
              .setVpDistance(200)
              .setVpSize(220, 220)
              .setResolution(600, 600)
              .rotate(25)
              .build()
              .renderImage()
              .writeToImage("megaBonus_angle2_right_rotated");

      // ── View 3: Top-down diagonal, rotated 45 degrees ────────────────────
      _cameraBuilder
              .setLocation(new Point(-150, 200, 200))
              .setDirection(new Point(20, -20, -80), Vector.AXIS_Y)
              .setVpDistance(300)
              .setVpSize(220, 220)
              .setResolution(600, 600)
              .rotate(45)
              .build()
              .renderImage()
              .writeToImage("megaBonus_angle3_top_rotated");
   }

   /**
    * Extracts the Bonus 1 scene setup into a reusable helper method,
    * so the same scene can be rendered from multiple camera angles in Bonus 2.
    */
   private void buildMegaScene() {
      _scene.geometries.add(
              new Plane(new Point(0, -50, 0), new Vector(0, 1, 0))
                      .setEmission(new Color(10, 10, 20))
                      .setMaterial(new Material()
                              .setKD(0.3).setKS(0.5).setShininess(80).setKR(0.4)),

              new Polygon(
                      new Point(-200, -50, -200),
                      new Point( 200, -50, -200),
                      new Point( 200,  200, -200),
                      new Point(-200,  200, -200))
                      .setEmission(new Color(25, 15, 35))
                      .setMaterial(new Material()
                              .setKD(0.6).setKS(0.2).setShininess(20)),

              new Cylinder(
                      new Ray(new Point(-60, -50, -100), new Vector(0, 1, 0)), 12, 100)
                      .setEmission(new Color(60, 40, 80))
                      .setMaterial(new Material()
                              .setKD(0.4).setKS(0.7).setShininess(60).setKR(0.2)),

              new Sphere(new Point(30, 10, -80), 45D)
                      .setEmission(new Color(0, 20, 60))
                      .setMaterial(new Material()
                              .setKD(0.2).setKS(0.3).setShininess(50).setKT(0.75)),

              new Sphere(new Point(30, 10, -80), 20D)
                      .setEmission(new Color(180, 30, 30))
                      .setMaterial(new Material()
                              .setKD(0.5).setKS(0.5).setShininess(80)),

              new Sphere(new Point(120, -10, -60), 35D)
                      .setEmission(new Color(5, 5, 5))
                      .setMaterial(new Material()
                              .setKD(0.05).setKS(0.1).setShininess(120).setKR(0.9)),

              new Sphere(new Point(-100, -25, -30), 25D)
                      .setEmission(new Color(10, 60, 30))
                      .setMaterial(new Material()
                              .setKD(0.5).setKS(0.4).setShininess(40)),

              new Sphere(new Point(90, -30, -20), 20D)
                      .setEmission(new Color(60, 0, 80))
                      .setMaterial(new Material()
                              .setKD(0.3).setKS(0.3).setShininess(40).setKT(0.5)),

              new Triangle(
                      new Point(100, -50, -50),
                      new Point(160, -50, -50),
                      new Point(130,  30, -70))
                      .setEmission(new Color(120, 90, 10))
                      .setMaterial(new Material()
                              .setKD(0.5).setKS(0.6).setShininess(50)),

              new Triangle(
                      new Point(-140, 100, -180),
                      new Point( -80, 100, -180),
                      new Point(-110, 160, -180))
                      .setEmission(new Color(0, 80, 100))
                      .setMaterial(new Material()
                              .setKD(0.5).setKS(0.5).setShininess(30)),

              new Tube(5, new Ray(new Point(-180, 40, -195), new Vector(1, 0, 0)))
                      .setEmission(new Color(50, 30, 10))
                      .setMaterial(new Material()
                              .setKD(0.3).setKS(0.2).setShininess(10))
      );

      _scene.setAmbientLight(
              new AmbientLight(new Color(15, 15, 25), new Double3(1)));

      _scene.lights.add(
              new SpotLight(
                      new Color(300, 200, 100),
                      new Point(-150, 200, 100),
                      new Vector(1, -1, -1))
                      .setKl(4E-5).setKq(2E-6));

      _scene.lights.add(
              new SpotLight(
                      new Color(80, 130, 280),
                      new Point(200, 180, 50),
                      new Vector(-1, -1, -1))
                      .setKl(4E-5).setKq(2E-6));

      _scene.lights.add(
              new PointLight(new Color(60, 50, 90), new Point(0, 50, -180))
                      .setKl(8E-5).setKq(4E-6));
   }
}
