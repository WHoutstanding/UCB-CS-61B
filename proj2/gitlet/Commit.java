package gitlet;

// TODO: any imports you need here

import java.io.Serializable;
import java.util.*;

/** Represents a gitlet commit object.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author TODO
 */
public class Commit implements Serializable {
    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /** The message of this Commit. */
    public String message;
    /** The date of this Commit. */
    public Date timestamp;
    /** files the commit track. */
    public TreeMap<String, String> files;
    /* Parent commit. */
    public List<String> parent;


    /* TODO: fill in the rest of this class. */
    public Commit(String message) {
        this.message = message;
        this.timestamp = new Date(); //TODO: verify this is the epoch date
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
