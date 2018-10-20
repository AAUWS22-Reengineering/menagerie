package menagerie.model.search;

import menagerie.model.ImageInfo;

public class MD5Rule extends SearchRule {

    private final String md5;


    public MD5Rule(String md5) {
        priorty = 100;

        this.md5 = md5;
    }

    public String getMd5() {
        return md5;
    }

    @Override
    public boolean accept(ImageInfo img) {
        return img.getMd5().equalsIgnoreCase(md5);
    }

    @Override
    public String toString() {
        return "MD5 Rule: " + md5;
    }

}
