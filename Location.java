public class Location {
    int x, y;

    public Location(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public int hashCode() {
        return x * Screen.ySize + y;
    }

    @Override
    public boolean equals(Object o) {
        Location other = (Location)o;
        return x == other.x && y == other.y;
    }
}