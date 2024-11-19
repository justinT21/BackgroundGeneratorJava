import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.StreamSupport;

import javax.imageio.ImageIO;

/**
 * A class to take in an image, find the closest background supported, and then
 * put into an external HashTable. To use, first instantiate a new instance,
 * then bind Color and String pairs, and then finally generate the grid.
 * <p>
 * Example use:
 * 
 * <pre>
 * int xSize = 300;
 * int ySize = 300;
 * gridMapTable = new MyHashTable<>(xSize * ySize);
 * BackgroundGenerator<Location, MapObject, MyHashMap<Color, String>> gen = new BackgroundGenerator<>(
 *         "france-road-map.jpg", xSize, ySize);
 * gen.bindPossibleColorTable(
 *         MyHashMap::new, new int[][] { { 172, 220, 242 }, { 193, 161, 156 },
 *                 { 199, 207, 190 }, { 149, 181, 145 } },
 *         new String[] { "water", "road", "grass", "mountain" });
 * gen.generate(gridMapTable::put, Location::new, MapObject::new);
 * </pre>
 * <p>
 * Note: Using a rgb color and string pairs allow you to tailor the color values
 * to the image you have chosen and then use different ones when drawing, e.g.
 * the roads are red in the image when you want them to be light gray. A good
 * way to pick colors is to use a color picker on the pixel in the debugged
 * image.
 * <p>
 * Implementation Notes:
 * The heavy use of generics and the <code>java.util.function</code> package
 * (what allows functions to be passed as arguments) is to most easily allow the
 * use of external data structures provided by the user. The general algorithm
 * is to take the image and then scale it to the desired grid size using average
 * sampling(more described at AreaAveragingScaleFilter) then finds the color
 * closest to the pixel out of the provided ones by taking the argmin of color
 * distance and then converts that using the user provided string pair to create
 * a respective external MapObject to put into the external HashTable.
 * 
 * @param K is the Location class, or the key class of your HashTable, e.g.
 *          Location
 * @param V is the MapObject class, or the value class of your HashTable, e.g.
 *          MapObject
 * @param H is your implementation of a HashMap that must implement the internal
 *          HashMap interface with types Color and String, e.g. MyHashMap<Color,
 *          String>
 * @see java.awt.image.AreaAveragingScaleFilter
 */
public class BackgroundGenerator<K, V, H extends BackgroundGenerator.HashMap<Color, String>> {
    private BufferedImage img;
    private int xSize, ySize;
    private H possibleColors;

    /**
     * @param imgFilePath file path for the image
     * @param xSize       grid X size
     * @param ySize       grid Y size
     */
    public BackgroundGenerator(String imgFilePath, int xSize, int ySize) {
        deconstructImg(imgFilePath, xSize, ySize, false);
    }

    /**
     * @param imgFilePath file path for the image
     * @param xSize       grid X size
     * @param ySize       grid Y size
     * @param debug       gives debugging info if true, e.g. the downsized image
     */
    public BackgroundGenerator(String imgFilePath, int xSize, int ySize, boolean debug) {
        deconstructImg(imgFilePath, xSize, ySize, debug);
    }

    /**
     * Binds a map from Colors to its respective background string.
     * <p>
     * Implementation Note: this is needed for most of our hashMaps as we can't deal
     * with negative hashCodes which java.awt.Color does. This method conviently
     * overrides that to be always positive
     * 
     * @param genHashMap function to create type <code>H</code> aka the constructor
     *                   of type <code>H</code>, e.g.
     *                   <code>MyHashMap::new</code>
     * @param colors     array of an array of 3 ints: r, g, b
     * @param strings    array of strings that match with the respective rbg values
     */
    public void bindPossibleColorTable(Function<Integer, H> genHashMap, int[][] colors, String[] strings) {
        possibleColors = genHashMap.apply(0x00ffffff);
        if (colors.length != strings.length) {
            throw new IllegalArgumentException("string array and color array should be the same length");
        }
        for (int i = 0; i < colors.length; i++) {
            possibleColors.put(new Color(colors[i][0], colors[i][1], colors[i][2]) {
                @Override
                public int hashCode() {
                    return Math.abs(super.hashCode());
                }
            }, strings[i]);
        }
    }

    /**
     * Adds all the generated backgrounds to the hashTable
     * 
     * @param mapPutFunc    function to put the Location(<code>K</code>) and
     *                      MapObject(<code>V</code>), e.g.
     *                      <code>MyHashMap::put</code>
     * @param locationGen   constructor of type Location(<code>K</code>). It must
     *                      take 2 integer arguments: x and y, e.g.
     *                      <code>Location::new</code>
     * @param backgroundGen constructor of type MapObject(<code>K</code>). It must
     *                      take only 1 string which matches to the ones provided in
     *                      <code>bindPossibleColors</code>, e.g.
     *                      <code>MapObject::new</code>
     */
    public void generate(BiConsumer<K, V> mapPutFunc, BiFunction<Integer, Integer, K> locationGen,
            Function<String, V> backgroundGen) {
        if (possibleColors == null) {
            throw new IllegalStateException("You must bind a Color Table before generating!");
        }
        for (int i = 0; i < xSize; i++) {
            for (int j = 0; j < ySize; j++) {
                mapPutFunc.accept(locationGen.apply(i, j),
                        getClosestBackground(img.getRGB(i, j), backgroundGen));
            }
        }
    }

    /**
     * HashMap interface that requires only the minimal functions
     */
    public interface HashMap<K, V> {
        public V get(K key);

        public V put(K key, V value);

        /**
         * @return HashSet<K> that is only required to be Iterable
         */
        public HashSet<K> keySet();
    }

    /**
     * Interface with minimal functions needed from HashSet. If you don't have an
     * iterator, I suggest you keep an internal DLList in the HashSet that also
     * implements Iterable and then add the following code:
     * 
     * <p>
     * 
     * <pre>
     * public Iterator<E> iterator() {
     *     return new DLListIterator(this);
     * }
     *
     * private class DLListIterator implements Iterator<E> {
     *     private Node<E> node;
     * 
     *     private DLListIterator(MyDLList<E> list) {
     *         node = list.head;
     *     }
     * 
     *     public boolean hasNext() {
     *         return node != null;
     *     }
     * 
     *     public E next() {
     *         Node<E> temp = node;
     *         node = node.next();
     *         return temp.get();
     *     }
     * }
     * </pre>
     */
    public interface HashSet<K> extends Iterable<K> {
    }

    private void deconstructImg(String imgFilePath, int xSize, int ySize, boolean debug) {
        this.xSize = xSize;
        this.ySize = ySize;

        try {
            img = ImageIO.read(new File(imgFilePath));
        } catch (IOException e) {
            System.err.println("Could not convert map! No file found");
            if (debug) {
                System.exit(-1);
                return; // java compiler tricks :)
            }
        }
        // downsize image by averging pixels
        Image temp = img.getScaledInstance(xSize, ySize, Image.SCALE_AREA_AVERAGING);
        // convert Image back to BufferedImage so we can use getRGB()
        img = new BufferedImage(xSize, ySize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gImg = img.createGraphics();
        gImg.drawImage(temp, 0, 0, null);
        gImg.dispose();

        if (debug) {
            try {
                // writes out downsized image
                ImageIO.write(img, "png", new File("temp.png"));
            } catch (Exception e) {
                System.out.println("didnt write");
            }
        }
    }

    private V getClosestBackground(int rgb, Function<String, V> backgroundGen) {
        // find the argmin of color distance, aka the color most similar to the pixel
        Color min = StreamSupport.stream(possibleColors.keySet().spliterator(), true).reduce(
                (i, j) -> Integer.compare(calculateColorDistance(rgb, i), calculateColorDistance(rgb, j)) <= 0 ? i : j)
                .orElseThrow();

        // convert minimized color to string
        String retString = possibleColors.get(min);

        return backgroundGen.apply(retString);
    }

    private static int calculateColorDistance(int rgb, Color color) {
        Color compare = new Color(rgb);
        // distance is found using the square of the difference of vectors with axes r,
        // g, b
        // Square root is not needed since it's all relative
        return (compare.getGreen() - color.getGreen()) * (compare.getGreen() - color.getGreen())
                + (compare.getBlue() - color.getBlue()) * (compare.getBlue() - color.getBlue())
                + (compare.getRed() - color.getRed()) * (compare.getRed() - color.getRed());
    }
}
