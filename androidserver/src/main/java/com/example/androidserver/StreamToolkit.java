package com.example.androidserver;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by apple on 16/8/20.
 */
public class StreamToolkit {
    public static final String readLine(InputStream nis) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        int c1 = 0;
        int c2 = 0;
        while (c2 != -1 && (c1 == '\r' && c2 == '\n')) {
            c1=c2;
            c2 = nis.read();
            stringBuilder.append((char) c2);
        }
        if (stringBuilder.length() == 0) {
            return null;
        }
        return stringBuilder.toString();
    }
}
