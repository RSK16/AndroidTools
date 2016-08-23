package com.example.imageloader.bean;

/**
 * Created by apple on 16/8/18.
 */
public class FolderBean {
    /**
     * 当前文件夹的路径
     */
    private String dir;
    private String firstImgPath;
    private String name;
    private int count;

    public String getDir() {
        return dir;
    }

    public String getFirstImgPath() {
        return firstImgPath;
    }

    public String getName() {
        return name;
    }

    public int getCount() {
        return count;
    }

    public void setDir(String dir) {
        this.dir = dir;
        int lastIndexOf = this.dir.indexOf("/");
        this.name = this.dir.substring(lastIndexOf);
    }

    public void setFirstImgPath(String firstImgPath) {
        this.firstImgPath = firstImgPath;
    }


    public void setCount(int count) {
        this.count = count;
    }
}
