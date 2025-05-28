package org.example.core.util;

import java.io.File;

public class FileFinder {
    private FileFinder() {}

    public static File find(File dir, String name) {
        if (!dir.exists()) return null;
        File[] files = dir.listFiles();
        if (files == null) return null;
        for (File f : files) {
            if (f.isDirectory()) {
                File res = find(f, name);
                if (res != null) return res;
            } else if (f.getName().equals(name)) {
                return f;
            }
        }
        return null;
    }
}
