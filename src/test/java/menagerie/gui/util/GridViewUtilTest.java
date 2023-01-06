package menagerie.gui.util;

import javafx.collections.FXCollections;
import javafx.scene.input.Dragboard;
import menagerie.gui.UITest;
import menagerie.gui.grid.ItemGridView;
import menagerie.model.menagerie.ItemTestUtils;
import menagerie.model.menagerie.MediaItem;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

public class GridViewUtilTest extends UITest {

  private final MediaItem m = new MediaItem(null, 1, 1, ItemTestUtils.WHITE_IMAGE_FILE);

  private ItemGridView getItemGridViewMock() {
    ItemGridView itemGridViewMock = mock(ItemGridView.class);
    when(itemGridViewMock.getSelected())
        .thenReturn(FXCollections.observableArrayList(
            ItemTestUtils.getGroup(),
            m));
    return itemGridViewMock;
  }

  @Test
  void testUpdateMediaItem() throws InterruptedException {
    ItemGridView itemGridView = getItemGridViewMock();
    // force thumbnail creation
    m.getThumbnail().want();
    Thread.sleep(UITest.SLEEP_TIME);

    Dragboard dbMock = mock(Dragboard.class);
    GridViewUtil.doDragAndDrop(dbMock, itemGridView);

    verify(dbMock, times(1)).setDragView(m.getThumbnail().getImage());
  }
}
