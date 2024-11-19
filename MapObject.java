import java.awt.Color;
import java.awt.Graphics;

public class MapObject {
    private String name;

    public MapObject(String name) {
        this.name = name.toLowerCase();
    }

    public void drawMe(Graphics g, int x, int y, int width, int height) {
        if (name.equals("water")) {
            g.setColor(Color.BLUE);
            g.fillRect(x, y, width, height);
        } else if (name.equals("road")) {
            g.setColor(Color.LIGHT_GRAY);
            g.fillRect(x, y, width, height);
        } else if (name.equals("grass")) {
            g.setColor(Color.GREEN);
            g.fillRect(x, y, width, height);
        } else if (name.equals("mountain")) {
            g.setColor(Color.DARK_GRAY);
            g.fillRect(x, y, width, height);
        }
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return name;
    }
}
