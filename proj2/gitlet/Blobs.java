package gitlet;

import java.io.Serializable;

public class Blobs implements Serializable {
    private String text;

    public Blobs(String contents) {
        this.text = contents;
    }

    public String text() {
        return this.text;
    }
}
