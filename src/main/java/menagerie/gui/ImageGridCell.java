package menagerie.gui;

import javafx.geometry.Pos;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import menagerie.model.ImageInfo;
import org.controlsfx.control.GridCell;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.io.IOException;
import java.util.Iterator;


public class ImageGridCell extends GridCell<ImageInfo> {

    private static final String UNSELECTED_BG_CSS = "-fx-background-color: derive(-fx-color, 50%)";
    private static final String SELECTED_BG_CSS = "-fx-background-color: derive(-fx-accent, 100%)";

    final private ImageView view;
    final private ImageGridView grid;


    public ImageGridCell(ImageGridView imageGridView) {
        super();

        this.grid = imageGridView;
        this.view = new ImageView();
        setGraphic(view);
        setAlignment(Pos.CENTER);
        setStyle(UNSELECTED_BG_CSS);
    }

    @Override
    protected void updateItem(ImageInfo item, boolean empty) {
        if (empty) {
            view.setImage(null);
        } else {
            view.setImage(item.getThumbnail());
        }

        super.updateItem(item, empty);

        if (grid.isSelected(item)) {
            setStyle(SELECTED_BG_CSS);
        } else {
            setStyle(UNSELECTED_BG_CSS);
        }
    }

}
