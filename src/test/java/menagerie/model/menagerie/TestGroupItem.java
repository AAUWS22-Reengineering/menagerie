package menagerie.model.menagerie;

public class TestGroupItem extends GroupItem {
  /**
   * ID uniqueness is not verified by this.
   *
   * @param menagerie Menagerie that owns this item.
   * @param id        Unique ID of this item.
   * @param dateAdded Date this item was added to the Menagerie.
   * @param title     Title of this group.
   */
  public TestGroupItem(Menagerie menagerie, int id, long dateAdded, String title) {
    super(menagerie, id, dateAdded, title);
  }

  public void setMenagerie(Menagerie m) {
    this.menagerie = m;
  }
}
