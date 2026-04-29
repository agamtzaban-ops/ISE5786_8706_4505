package scene;

import geometries.impl.Geometries;
import geometries.impl.Sphere;
import geometries.impl.Triangle;
import lighting.AmbientLight;
import primitives.Color;
import primitives.Point;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.File;

/**
 * SceneLoader class responsible for external scene data parsing.
 * Implements SRP by separating XML logic from the Scene logic.
 * Designed for future extensibility (Lights, Materials, etc.).
 */
public class SceneLoader {

    /**
     * Loads a scene from an XML file configuration.
     * @param filePath The path to the XML file.
     * @param sceneName The name for the newly created scene.
     * @return A fully initialized Scene object.
     */
    public static Scene loadSceneFromXML(String filePath, String sceneName) {
        Scene scene = new Scene(sceneName);

        try {
            // Document loading logic
            File xmlFile = new File(filePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            Element root = doc.getDocumentElement();

            // 1. Scene Background Initialization
            if (root.hasAttribute("background-color")) {
                scene.setBackground(parseColor(root.getAttribute("background-color")));
            }

            // 2. Ambient Light Initialization
            NodeList ambientNodes = doc.getElementsByTagName("ambient-light");
            if (ambientNodes.getLength() > 0) {
                Element ambientElement = (Element) ambientNodes.item(0);
                if (ambientElement.hasAttribute("color")) {
                    Color color = parseColor(ambientElement.getAttribute("color"));
                    scene.setAmbientLight(new AmbientLight(color));
                }
            }

            // 3. Geometries Initialization
            NodeList geometriesNodes = doc.getElementsByTagName("geometries");
            if (geometriesNodes.getLength() > 0) {
                parseGeometries(scene.geometries, (Element) geometriesNodes.item(0));
            }

        } catch (Exception e) {
            throw new RuntimeException("Critical Error: Failed to parse XML file: " + filePath, e);
        }

        return scene;
    }

    /**
     * Parses the geometries container.
     * Uses switch-case for OCP (Open/Closed Principle) to allow future shape types.
     */
    private static void parseGeometries(Geometries geometries, Element container) {
        NodeList children = container.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element el = (Element) node;
                String type = el.getTagName();

                switch (type) {
                    case "sphere" -> geometries.add(new Sphere(
                            parsePoint(el.getAttribute("center")),
                            Double.parseDouble(el.getAttribute("radius"))
                    ));
                    case "triangle" -> geometries.add(new Triangle(
                            parsePoint(el.getAttribute("p0")),
                            parsePoint(el.getAttribute("p1")),
                            parsePoint(el.getAttribute("p2"))
                    ));
                    // Future shapes like "plane" or "cylinder" will be added here
                }
            }
        }
    }

    /**
     * Factory method: String "R G B" -> primitives.Color
     * Implements DRY to avoid repeated splitting logic.
     */
    private static Color parseColor(String str) {
        String[] rgb = str.trim().split("\\s+");
        return new Color(Double.parseDouble(rgb[0]),
                Double.parseDouble(rgb[1]),
                Double.parseDouble(rgb[2]));
    }

    /**
     * Factory method: String "X Y Z" -> primitives.Point
     * Implements DRY to avoid repeated splitting logic.
     */
    private static Point parsePoint(String str) {
        String[] xyz = str.trim().split("\\s+");
        return new Point(Double.parseDouble(xyz[0]),
                Double.parseDouble(xyz[1]),
                Double.parseDouble(xyz[2]));
    }
}