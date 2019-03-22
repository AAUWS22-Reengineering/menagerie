package menagerie.model.search.rules;

import menagerie.model.menagerie.MediaItem;
import menagerie.model.menagerie.Item;

public class MD5Rule extends SearchRule {

    private final String md5;


    public MD5Rule(String md5, boolean inverted) {
        super(inverted);
        priority = 100;

        this.md5 = md5;
    }

    @Override
    public boolean accept(Item item) {
        boolean result = item instanceof MediaItem && ((MediaItem) item).getMD5().equalsIgnoreCase(md5);
        if (isInverted()) result = !result;
        return result;
    }

    @Override
    public String toString() {
        String result = "MD5 Rule: " + md5;
        if (isInverted()) result += " [inverted]";
        return result;
    }

}
