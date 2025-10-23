package gitlet;

import java.io.Serializable;

public class Blobs implements Serializable {
    public String text;

    public Blobs(String contents) {
        this.text = contents;
    }
}
