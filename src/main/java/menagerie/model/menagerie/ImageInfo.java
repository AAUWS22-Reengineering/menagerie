package menagerie.model.menagerie;

import com.sun.org.apache.xerces.internal.impl.dv.util.HexBin;
import javafx.scene.image.Image;
import menagerie.util.ImageInputStreamConverter;
import menagerie.util.MD5Hasher;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ImageInfo implements Comparable<ImageInfo> {

    // ---------------------------- Constants ----------------------------------------

    public static final int THUMBNAIL_SIZE = 150;

    // -------------------------------- Variables ------------------------------------

    private final Menagerie menagerie;

    private final int id;
    private final long dateAdded;
    private final List<Tag> tags = new ArrayList<>();

    private File file;
    private String md5;
    private ImageHistogram histogram;

    private SoftReference<Image> thumbnail;
    private SoftReference<Image> image;

    private ImageTagUpdateListener tagListener = null;


    public ImageInfo(Menagerie menagerie, int id, long dateAdded, File file, String md5, ImageHistogram histogram) {
        this.menagerie = menagerie;
        this.id = id;
        this.dateAdded = dateAdded;
        this.file = file;
        this.md5 = md5;
        this.histogram = histogram;
    }

    public File getFile() {
        return file;
    }

    public long getDateAdded() {
        return dateAdded;
    }

    public int getId() {
        return id;
    }

    public Image getThumbnail() {
        Image img = null;
        if (thumbnail != null) img = thumbnail.get();
        if (img == null) {
            try {
                menagerie.PS_GET_IMG_THUMBNAIL.setInt(1, id);
                ResultSet rs = menagerie.PS_GET_IMG_THUMBNAIL.executeQuery();
                if (rs.next()) {
                    InputStream binaryStream = rs.getBinaryStream("thumbnail");
                    if (binaryStream != null) {
                        img = ImageInputStreamConverter.imageFromInputStream(binaryStream);
                        thumbnail = new SoftReference<>(img);
                    }
                }
            } catch (SQLException | IOException e) {
                e.printStackTrace();
            }
        }
        if (img == null) {
            img = buildThumbnail();
            thumbnail = new SoftReference<>(img);

            if (img != null) {
                final Image finalBullshit = img;
                img.progressProperty().addListener((observable, oldValue, newValue) -> {
                    if (!finalBullshit.isError() && newValue.doubleValue() == 1.0) {
                        menagerie.getUpdateQueue().enqueueUpdate(() -> {
                            try {
                                menagerie.PS_SET_IMG_THUMBNAIL.setBinaryStream(1, ImageInputStreamConverter.imageToInputStream(finalBullshit));
                                menagerie.PS_SET_IMG_THUMBNAIL.setInt(2, id);
                                menagerie.PS_SET_IMG_THUMBNAIL.executeUpdate();
                            } catch (SQLException | IOException e) {
                                e.printStackTrace();
                            }
                        });
                        menagerie.getUpdateQueue().commit();
                    }
                });
            }
        }
        return img;
    }

    public Image getImage() {
        Image img = null;
        if (image != null) img = image.get();
        if (img == null) {
            img = new Image(file.toURI().toString(), true);
            image = new SoftReference<>(img);
        }
        return img;
    }

    private Image getImageAsync() {
        Image img = null;
        if (image != null) img = image.get();
        if (img == null) {
            img = new Image(file.toURI().toString());
            image = new SoftReference<>(img);
        } else if (img.isBackgroundLoading() && img.getProgress() != 1) {
            img = new Image(file.toURI().toString());
        }
        return img;
    }

    public String getMD5() {
        return md5;
    }

    public ImageHistogram getHistogram() {
        return histogram;
    }

    public void initializeMD5() {
        if (md5 != null) return;

        try {
            md5 = HexBin.encode(MD5Hasher.hash(getFile()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void commitMD5ToDatabase() {
        if (md5 == null) return;

        menagerie.imageMD5Updated(this);

        menagerie.getUpdateQueue().enqueueUpdate(() -> {
            try {
                menagerie.PS_SET_IMG_MD5.setNString(1, md5);
                menagerie.PS_SET_IMG_MD5.setInt(2, id);
                menagerie.PS_SET_IMG_MD5.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        menagerie.getUpdateQueue().commit();

    }

    public void initializeHistogram() {
        try {
            histogram = new ImageHistogram(getImageAsync());
        } catch (HistogramReadException e) {
            // A comment to make the warning of an empty catch block go away
        }
    }

    public void commitHistogramToDatabase() {
        if (histogram == null) return;

        menagerie.getUpdateQueue().enqueueUpdate(() -> {
            try {
                menagerie.PS_SET_IMG_HISTOGRAM.setBinaryStream(1, histogram.getAlphaAsInputStream());
                menagerie.PS_SET_IMG_HISTOGRAM.setBinaryStream(2, histogram.getRedAsInputStream());
                menagerie.PS_SET_IMG_HISTOGRAM.setBinaryStream(3, histogram.getGreenAsInputStream());
                menagerie.PS_SET_IMG_HISTOGRAM.setBinaryStream(4, histogram.getBlueAsInputStream());
                menagerie.PS_SET_IMG_HISTOGRAM.setInt(5, id);
                menagerie.PS_SET_IMG_HISTOGRAM.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });

    }

    private Image buildThumbnail() {
        String extension = file.getName().toLowerCase();
        extension = extension.substring(extension.indexOf('.') + 1);

        switch (extension) {
            case "png":
            case "jpg":
            case "jpeg":
            case "bmp":
            case "gif":
                return new Image(file.toURI().toString(), THUMBNAIL_SIZE, THUMBNAIL_SIZE, true, true, true);
            case "webm":
            case "mp4":
            case "avi":
                //TODO: Load video into VLCJ player and take snapshot
                return null;
        }

        return null;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public boolean hasTag(Tag t) {
        return tags.contains(t);
    }

    public void addTag(Tag t) {
        if (hasTag(t)) return;
        tags.add(t);
        t.incrementFrequency();

        menagerie.getUpdateQueue().enqueueUpdate(() -> {
            try {
                menagerie.PS_ADD_TAG_TO_IMG.setInt(1, id);
                menagerie.PS_ADD_TAG_TO_IMG.setInt(2, t.getId());
                menagerie.PS_ADD_TAG_TO_IMG.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        menagerie.getUpdateQueue().commit();

        menagerie.imageTagsUpdated(this);

        if (tagListener != null) tagListener.tagsChanged();

    }

    public void removeTag(Tag t) {
        if (!hasTag(t)) return;
        tags.remove(t);
        t.decrementFrequency();

        menagerie.getUpdateQueue().enqueueUpdate(() -> {
            try {
                menagerie.PS_REMOVE_TAG_FROM_IMG.setInt(1, id);
                menagerie.PS_REMOVE_TAG_FROM_IMG.setInt(2, t.getId());
                menagerie.PS_REMOVE_TAG_FROM_IMG.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        menagerie.getUpdateQueue().commit();

        menagerie.imageTagsUpdated(this);

        if (tagListener != null) tagListener.tagsChanged();

    }

    public boolean renameTo(File dest) {
        if (file.equals(dest)) return true;

        boolean succeeded = file.renameTo(dest);

        if (succeeded) {
            file = dest;

            menagerie.getUpdateQueue().enqueueUpdate(() -> {
                try {
                    menagerie.PS_SET_IMG_PATH.setNString(1, file.getAbsolutePath());
                    menagerie.PS_SET_IMG_PATH.setInt(2, id);
                    menagerie.PS_SET_IMG_PATH.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });
            menagerie.getUpdateQueue().commit();
        }

        return succeeded;
    }

    public double getSimilarityTo(ImageInfo other, boolean compareBlackAndWhiteHists) {
        if (md5 != null && md5.equals(other.getMD5())) {
            return 1.0;
        } else if (histogram != null && other.getHistogram() != null) {
            if (compareBlackAndWhiteHists || (!histogram.isBlackAndWhite() && !other.getHistogram().isBlackAndWhite())) {
                return histogram.getSimilarity(other.getHistogram());
            }
        }

        return 0;
    }

    public void remove(boolean deleteFile) {
        menagerie.removeImage(this, deleteFile);
    }

    public void setTagListener(ImageTagUpdateListener tagListener) {
        this.tagListener = tagListener;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ImageInfo && ((ImageInfo) obj).getId() == getId();
    }

    @Override
    public int compareTo(ImageInfo o) {
        return getId() - o.getId();
    }

    @Override
    public String toString() {
        return "Image (" + getId() + ") \"" + getFile().getAbsolutePath() + "\" - " + new Date(getDateAdded());
    }

}
