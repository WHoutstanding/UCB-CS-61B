package gitlet;

import java.io.File;

import static gitlet.Utils.*;

public class Test {
    public static final File CWD = new File(System.getProperty("user.dir"));
    public static void main(String[] args) {
        File addFile = join(CWD, "Hello.txt");
        /* The byte[] of addFile. */
        byte[] addFileByte = readContents(addFile);
        /* The sha1 of addFile. */
//        String addFileSha1 = sha1((Object) addFileByte);
        File newFile = join(CWD, "newHello.txt");
        writeObject(newFile, addFileByte);
    }
}
