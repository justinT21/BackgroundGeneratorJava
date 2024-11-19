import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.Hashtable;

import javax.swing.JPanel;

public class Screen extends JPanel {
    private Hashtable<Location, MapObject> gridMapTable;
    public static final int xSize = 50;
    public static final int ySize = 50;
    public static final int xDim = 20;
    public static final int yDim = 20;

    public Screen() {
        gridMapTable = new Hashtable<>(xSize * ySize);
        BackgroundGenerator<Location, MapObject, CustomHashMap<Color, String>> gen = new BackgroundGenerator<>(
                "france-road-map.jpg", xSize, ySize);
        gen.bindPossibleColorTable(
                CustomHashMap::new, new int[][] { { 172, 220, 242 }, { 193, 161, 156 },
                        { 199, 207, 190 }, { 149, 181, 145 } },
                new String[] { "water", "road", "grass", "mountain" });
        gen.generate(gridMapTable::put, Location::new, MapObject::new);
    }

    @Override
    public void paintComponent(Graphics g) {
        for (int i = 0; i < xSize; i++) {
            for (int j = 0; j < ySize; j++) {
                gridMapTable.get(new Location(i, j)).drawMe(g, i * xDim, j * yDim, xDim, yDim);
            }
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(xSize * xDim, ySize * yDim);
    }

    public void animate() {
        while (true) {
            repaint();

            try {
                Thread.sleep(20);
            } catch (Exception e) {
            }
        }
    }
}
