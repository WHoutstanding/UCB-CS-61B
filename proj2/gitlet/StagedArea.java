package gitlet;

import java.io.Serializable;
import java.util.*;

public class StagedArea implements Serializable {
    /* Addition is file name to byte[] type. */
    TreeMap<String, String> addition;
    /* Removal is file name. */
    TreeMap<String, String> removal;
    public StagedArea() {
        this.addition = new TreeMap<>();
        this.removal = new TreeMap<>();
    }
}
