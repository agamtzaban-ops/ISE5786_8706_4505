package scene;

import geometries.api.Geometry;
import geometries.impl.Geometries;
import geometries.impl.Sphere;
import geometries.impl.Triangle;
import lighting.*;
import primitives.*;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.File;

/**
 * SceneLoader class responsible for external scene data parsing.
 * Updated for Stage 7 bonus maintenance to support Material, Emission, and External Lights.
 */
public class SceneLoader {

    public static Scene loadSceneFromXML(String filePath, String sceneName) {
        Scene scene = new Scene(sceneName);

        try {
            File xmlFile = new File(filePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            Element root = doc.getDocumentElement();

            // 1. Scene Background
            if (root.hasAttribute("background-color")) {
                scene.setBackground(parseColor(root.getAttribute("background-color")));
            }

            // 2. Ambient Light
            NodeList ambientNodes = doc.getElementsByTagName("ambient-light");
            if (ambientNodes.getLength() > 0) {
                Element ambientElement = (Element) ambientNodes.item(0);
                if (ambientElement.hasAttribute("color")) {
                    Color color = parseColor(ambientElement.getAttribute("color"));
                    double ka = ambientElement.hasAttribute("ka") ? Double.parseDouble(ambientElement.getAttribute("ka")) : 1.0;
                    scene.setAmbientLight(new AmbientLight(color.scale(ka)));
                }
            }

            // 3. Geometries
            NodeList geometriesNodes = doc.getElementsByTagName("geometries");
            if (geometriesNodes.getLength() > 0) {
                parseGeometries(scene.geometries, (Element) geometriesNodes.item(0));
            }

            // 4. Lights (Stage 7 Bonus)
            NodeList lightsNodes = doc.getElementsByTagName("lights");
            if (lightsNodes.getLength() > 0) {
                parseLights(scene, (Element) lightsNodes.item(0));
            }

        } catch (Exception e) {
            throw new RuntimeException("Critical Error: Failed to parse XML file: " + filePath, e);
        }

        return scene;
    }

    private static void parseGeometries(Geometries geometries, Element container) {
        NodeList children = container.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element el = (Element) node;
                Geometry geo = switch (el.getTagName()) {
                    case "sphere" -> new Sphere(parsePoint(el.getAttribute("center")), Double.parseDouble(el.getAttribute("radius")));
                    case "triangle" -> new Triangle(parsePoint(el.getAttribute("p0")), parsePoint(el.getAttribute("p1")), parsePoint(el.getAttribute("p2")));
                    default -> null;
                };

                if (geo != null) {
                    applyStage6Properties(el, geo);
                    geometries.add(geo);
                }
            }
        }
    }

    private static void parseLights(Scene scene, Element container) {
        NodeList children = container.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element el = (Element) node;
                Color intensity = parseColor(el.getAttribute("color"));

                switch (el.getTagName()) {
                    case "directional":
                        scene.lights.add(new DirectionalLight(intensity, parseVector(el.getAttribute("direction"))));
                        break;
                    case "point":
                        PointLight pl = new PointLight(intensity, parsePoint(el.getAttribute("position")));
                        applyAttenuation(el, pl);
                        scene.lights.add(pl);
                        break;
                    case "spot":
                        SpotLight sl = new SpotLight(intensity, parsePoint(el.getAttribute("position")), parseVector(el.getAttribute("direction")));
                        applyAttenuation(el, sl);
                        if (el.hasAttribute("narrowBeam")) {
                            sl.setNarrowBeam(Integer.parseInt(el.getAttribute("narrowBeam")));
                        }
                        scene.lights.add(sl);
                        break;
                }
            }
        }
    }

    private static void applyAttenuation(Element el, PointLight pl) {
        if (el.hasAttribute("kC")) pl.setKc(Double.parseDouble(el.getAttribute("kC")));
        if (el.hasAttribute("kL")) pl.setKl(Double.parseDouble(el.getAttribute("kL")));
        if (el.hasAttribute("kQ")) pl.setKq(Double.parseDouble(el.getAttribute("kQ")));
    }

    private static void applyStage6Properties(Element el, Geometry geo) {
        if (el.hasAttribute("emission")) {
            geo.setEmission(parseColor(el.getAttribute("emission")));
        }

        Material mat = new Material();
        double kd = el.hasAttribute("kd") ? Double.parseDouble(el.getAttribute("kd")) : 0.5;
        double ks = el.hasAttribute("ks") ? Double.parseDouble(el.getAttribute("ks")) : 0.5;
        int shininess = el.hasAttribute("nShininess") ? (int) Double.parseDouble(el.getAttribute("nShininess")) : 30;

        mat.setKd(kd).setKs(ks).setShininess(shininess);
        geo.setMaterial(mat);
    }

    private static Color parseColor(String str) {
        String[] rgb = str.trim().split("\\s+");
        return new Color(Double.parseDouble(rgb[0]), Double.parseDouble(rgb[1]), Double.parseDouble(rgb[2]));
    }

    private static Point parsePoint(String str) {
        String[] xyz = str.trim().split("\\s+");
        return new Point(Double.parseDouble(xyz[0]), Double.parseDouble(xyz[1]), Double.parseDouble(xyz[2]));
    }

    private static Vector parseVector(String str) {
        String[] xyz = str.trim().split("\\s+");
        return new Vector(Double.parseDouble(xyz[0]), Double.parseDouble(xyz[1]), Double.parseDouble(xyz[2]));
    }

    /**
     * Stub for JSON scene loading.
     * Currently not implemented.
     */
    @SuppressWarnings("unused")
    public static Scene loadSceneFromJSON(String filePath, String sceneName) {
        return new Scene(sceneName);
    }
}