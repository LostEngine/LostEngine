package dev.lost.engine.utils;

import lombok.Getter;

public class FoliaUtils {

    @Getter
    private static boolean isFolia = false;

    static {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            isFolia = true;
        } catch (ClassNotFoundException ignored) {
        }
    }

}
