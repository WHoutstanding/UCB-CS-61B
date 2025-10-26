package gitlet;

import java.io.Serializable;
import java.util.*;

/** Gitlet commit object.
 *  @author Wang hao
 */
public class Commit implements Serializable {
    /** The message of this Commit. */
    public String message;
    /** The date of this Commit. */
    public Date timestamp;
    /** files the commit track. */
    public TreeMap<String, String> files;
    /* Parent commit. */
    public List<String> parent;

    public Commit(String message) {
        this.message = message;
        this.timestamp = new Date();
        this.files = new TreeMap<>();
        this.parent = new ArrayList<>();

        if (message.equals("initial commit")) {
            this.parent = null;
        }
    }

    public String time() {
        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb, Locale.US);

        // 使用Formatter格式化日期
        formatter.format("%1$ta %1$tb %1$td %1$tH:%1$tM:%1$tS %1$tY %1$tz",
                this.timestamp);

        formatter.close();
        return sb.toString();
    }
}
