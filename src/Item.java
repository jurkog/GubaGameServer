/**
 * Created by Jurko on 11/11/2014.
 */
public class Item {
    int x, y;
    String id;

    public Item(String id, int x, int y) {
        this.id = id;
        this.x = x;
        this.y = y;
    }

    public Item(String id) {
        this.id = id;
    }
}
