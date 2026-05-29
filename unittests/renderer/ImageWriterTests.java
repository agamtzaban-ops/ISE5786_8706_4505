package renderer;

import org.junit.jupiter.api.Test;
import primitives.Color;

import static java.awt.Color.*;

/**
 * Unit test for ImageWriter class.
 */
class ImageWriterTests {

    /**
     * Test method for creating a simple grid image.
     */
    @Test
    void testImageWriter() {
        // Constants for the image definition
        final int nX = 800;
        final int nY = 500;
        final int step = 50;

        final Color background = new Color(CYAN); // Blue
        final Color gridColor = new Color(YELLOW);  // Yellow

        // 1. Create the ImageWriter object with only resolution
        ImageWriter imageWriter = new ImageWriter(nX, nY);

        // Loop through all pixels
        for (int i = 0; i < nX; i++) {
            for (int j = 0; j < nY; j++) {
                // 2. Write pixel using your primitives.Color directly
                imageWriter.writePixel(i, j,
                        (i % step == 0 || j % step == 0) ? gridColor : background);
            }
        }

        // 3. Save the image and provide the filename here
        imageWriter.writeToImage("base_grid");
    }
}