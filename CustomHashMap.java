import java.util.HashMap;
import java.util.Set;

public class CustomHashMap<K, V> extends HashMap<K, V> implements BackgroundGenerator.HashMap<K, V> {
    public CustomHashMap(int size) {
        super(size);
    }

    @Override
    public CustomHashSet<K> keySet() {
        Set<K> keys = super.keySet();
        return new CustomHashSet<>() {
            {
                addAll(keys);
            }
        };
    }
}
