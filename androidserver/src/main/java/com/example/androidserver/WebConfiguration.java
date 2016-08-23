package com.example.androidserver;

/**
 * Created by apple on 16/8/20.
 */
public class WebConfiguration {
    /**
     * 端口
     */
    private int port;

    /**
     * 最大监听数
     */
    private int maxParallelsl;

    public void setPort(int port) {
        this.port = port;
    }

    public void setMaxParallelsl(int maxParallelsl) {
        this.maxParallelsl = maxParallelsl;
    }

    public int getPort() {
        return port;
    }

    public int getMaxParallelsl() {
        return maxParallelsl;
    }

}
