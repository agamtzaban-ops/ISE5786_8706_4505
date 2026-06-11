package primitives;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Texture — loads a 2D image file and samples it by (u,v) UV coordinates.
 *
 * <p>Usage in a scene:</p>
 * <pre>
 *   Texture earthTex = new Texture("images/earth.jpg");
 *   material.setTexture(earthTex);
 * </pre>
 *
 * <p>When the ray tracer shades an intersection it calls
 * {@link #getColor(double, double)} with the UV pair returned by
 * {@code geometry.getUV(intersectionPoint)}, and uses the sampled color
 * instead of (or blended with) the geometry's fixed emission.</p>
 */
public class Texture {

    /** The loaded image. */
    private final BufferedImage _image;

    /** Image width in pixels. */
    private final int _width;

    /** Image height in pixels. */
    private final int _height;

    /**
     * Loads a texture from a file path.
     *
     * @param filePath path to the image file (JPG, PNG, BMP, GIF …)
     * @throws RuntimeException if the file cannot be read
     */
    public Texture(String filePath) {
        try {
            BufferedImage loaded = ImageIO.read(new File(filePath));
            if (loaded == null)
                throw new RuntimeException(
                        "Texture: ImageIO could not decode file (unsupported format or corrupt): "
                        + new File(filePath).getAbsolutePath());
            _image  = loaded;
            _width  = _image.getWidth();
            _height = _image.getHeight();
        } catch (IOException e) {
            throw new RuntimeException("Texture: cannot load image: " + filePath, e);
        }
    }

    /**
     * Samples the texture at the given UV coordinates.
     *
     * @param u horizontal coordinate [0.0 , 1.0]
     * @param v vertical   coordinate [0.0 , 1.0]
     * @return the {@link Color} of the pixel at (u,v)
     */
    public Color getColor(double u, double v) {
        // Clamp to [0,1] to avoid out-of-bounds
        u = Math.max(0.0, Math.min(1.0, u));
        v = Math.max(0.0, Math.min(1.0, v));

        // Convert to pixel coordinates (row 0 = top of image)
        int px = (int) Math.min(u * _width,  _width  - 1);
        int py = (int) Math.min(v * _height, _height - 1);

        // Extract RGB from the packed int returned by BufferedImage
        int rgb  = _image.getRGB(px, py);
        int r    = (rgb >> 16) & 0xFF;
        int g    = (rgb >>  8) & 0xFF;
        int b    =  rgb        & 0xFF;

        return new Color(r, g, b);
    }
}
