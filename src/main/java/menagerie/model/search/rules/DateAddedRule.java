package menagerie.model.search.rules;

import menagerie.model.menagerie.Item;

import java.util.Date;

public class DateAddedRule extends SearchRule {

    public enum Type {
        LESS_THAN,
        GREATER_THAN,
        EQUAL_TO
    }

    private final long time;
    private final Type type;


    public DateAddedRule(Type type, long time, boolean inverted) {
        super(inverted);
        priority = 10;

        this.time = time;
        this.type = type;
    }

    @Override
    public boolean accept(Item item) {
        boolean result = false;
        switch (type) {
            case LESS_THAN:
                result = item.getDateAdded() < time;
                break;
            case GREATER_THAN:
                result = item.getDateAdded() > time;
                break;
            case EQUAL_TO:
                result = item.getDateAdded() == time;
                break;
        }

        if (isInverted()) result = !result;

        return result;
    }

    @Override
    public String toString() {
        String result = "Added Date Rule: " + type + " " + time + " (" + new Date(time) + ")";
        if (isInverted()) result += " [inverted]";
        return result;
    }

}
