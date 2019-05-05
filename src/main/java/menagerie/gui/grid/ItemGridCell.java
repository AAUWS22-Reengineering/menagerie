package menagerie.gui.grid;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import menagerie.model.menagerie.GroupItem;
import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.MediaItem;
import menagerie.util.listeners.ObjectListener;
import org.controlsfx.control.GridCell;


public class ItemGridCell extends GridCell<Item> {

    private static final String UNSELECTED_BG_CSS = "-fx-background-color: -item-unselected-color";
    private static final String SELECTED_BG_CSS = "-fx-background-color: -item-selected-color";

    private static final Font LARGE_FONT = Font.font(Font.getDefault().getName(), FontWeight.BOLD, 28);
    private static final Font SMALL_FONT = Font.font(Font.getDefault().getName(), FontWeight.BOLD, 14);

    private final ImageView view = new ImageView();
    private final Label centerLabel = new Label();
    private final Label bottomRightLabel = new Label();

    private final ObjectListener<Image> imageReadyListener;


    public ItemGridCell() {
        super();
        this.getStyleClass().add("item-grid-cell");

        centerLabel.setPadding(new Insets(5));
        centerLabel.setFont(LARGE_FONT);
        centerLabel.setEffect(new DropShadow());
        centerLabel.setWrapText(true);

        bottomRightLabel.setPadding(new Insets(2));
        bottomRightLabel.setFont(SMALL_FONT);
        bottomRightLabel.setEffect(new DropShadow());
        StackPane.setAlignment(bottomRightLabel, Pos.BOTTOM_RIGHT);

        setGraphic(new StackPane(view, centerLabel, bottomRightLabel));
        setAlignment(Pos.CENTER);
        setStyle(UNSELECTED_BG_CSS);

        imageReadyListener = view::setImage;

    }

    @Override
    protected void updateItem(Item item, boolean empty) {
        if (getItem() != null && getItem().getThumbnail() != null) {
            getItem().getThumbnail().setDoNotLoad(true);
            getItem().getThumbnail().removeImageReadyListener(imageReadyListener);
        }

        if (empty) {
            view.setImage(null);
            centerLabel.setText(null);
            bottomRightLabel.setText(null);
        } else {
            if (item.getThumbnail() != null) {
                item.getThumbnail().setDoNotLoad(false);
                if (item.getThumbnail().getImage() != null) {
                    view.setImage(item.getThumbnail().getImage());
                } else {
                    view.setImage(null);
                    item.getThumbnail().addImageReadyListener(imageReadyListener);
                }
            }
            if (item instanceof MediaItem) {
                if (((MediaItem) item).isVideo()) {
                    centerLabel.setText("Video");
                    centerLabel.setFont(LARGE_FONT);
                } else {
                    centerLabel.setText(null);
                }
                if (((MediaItem) item).isInGroup()) {
                    bottomRightLabel.setText(((MediaItem) item).getPageIndex() + "");
                } else {
                    bottomRightLabel.setText(null);
                }
                if (!((MediaItem) item).isImage() && !((MediaItem) item).isVideo()) {
                    centerLabel.setText(((MediaItem) item).getFile().getName());
                    centerLabel.setFont(SMALL_FONT);
                    //                    Icon icon = FileSystemView.getFileSystemView().getSystemIcon(((MediaItem) item).getFile());
                    //                    ImageIcon imgIcon = (ImageIcon) icon;
                    //                    BufferedImage bi = new BufferedImage(imgIcon.getIconWidth(), imgIcon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
                    //                    Graphics2D g2d = bi.createGraphics();
                    //                    g2d.drawImage(imgIcon.getImage(), 0, 0, null);
                    //                    view.setImage(SwingFXUtils.toFXImage(bi, null));
                }
            } else if (item instanceof GroupItem) {
                centerLabel.setText(((GroupItem) item).getTitle());
                centerLabel.setFont(SMALL_FONT);
                bottomRightLabel.setText(((GroupItem) item).getElements().size() + "");
            }
        }

        super.updateItem(item, empty);

        if (getGridView() != null && getGridView() instanceof ItemGridView && ((ItemGridView) getGridView()).isSelected(item)) {
            setStyle(SELECTED_BG_CSS);
        } else {
            setStyle(UNSELECTED_BG_CSS);
        }
    }

}
