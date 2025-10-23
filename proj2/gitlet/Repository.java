package gitlet;

import java.io.File;
import java.util.List;
import java.util.Locale;

import static gitlet.Utils.*;

// TODO: any imports you need here

/** Represents a gitlet repository.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author TODO
 */
public class Repository {
    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    /**  The HEAD pointer. */
    public static String HEAD;
    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    /** The .gitlet/commit directory. */
    public static final File COMMIT_DIR = join(GITLET_DIR, "commit");
    /** The .gitlet/blobs directory. */
    public static final File BlOB_DIR = join(GITLET_DIR, "blob");
    /** The .gitlet/branch directory. */
    public static final File BRANCH_DIR = join(GITLET_DIR, "branch");

    /* TODO: fill in the rest of this class. */
    public static void init() {
        /* TODO It will have a single branch: master, which initially points to this initial commit,
                and master will be the current branch. */

        if (GITLET_DIR.exists()) {
            System.out.println("A Gitlet version-control system already exists in the current directory.");
            return;
        }

        /* Create .gitlet folder. */
        GITLET_DIR.mkdir();
        COMMIT_DIR.mkdir();
        BlOB_DIR.mkdir();
        BRANCH_DIR.mkdir();

        /* Create staged area object. */
        StagedArea stagedArea = new StagedArea();
        /* Index is the file of storing stagedArea object. */
        File index = new File(GITLET_DIR, "index");
        /* Store stagedArea object to index. */
        writeObject(index, stagedArea);

        /* Create head file storing the pointer.  */
        File headFile = new File(GITLET_DIR, "HEAD");
        writeContents(headFile, "master");


        /* Create commit0. */
        Commit initialCommit = new Commit("initial commit");
        byte[] initialCommitByte = serialize(initialCommit);
        String initialCommitSha1 = sha1((Object) initialCommitByte);

        /* Create master branch. */
        File masterBranchFile = join(BRANCH_DIR, "master");
        writeContents(masterBranchFile, initialCommitSha1);

        /* Store the initial commit. */
        File initialCommitFile = join(COMMIT_DIR, initialCommitSha1);
        writeObject(initialCommitFile, initialCommit);
    }

    public static void add(String fileName) {
        /* The file we need to add. */
        File addFile = join(CWD, fileName);

        /* The added file must exist. */
        if (!addFile.exists()) {
            System.out.println("File does not exist.");
            return;
        }

        /* The text of added file. */
        String addFileText = readContentsAsString(addFile);
        /* The byte[] of addFile. */
        byte[] addFileByte = serialize(addFileText);
        /* The sha1 of addFile. */
        String addFileSha1 = sha1((Object) addFileByte);

        /* Read stagedArea object from index. */
        File index = join(GITLET_DIR, "index");
        StagedArea stagedArea = readObject(index, StagedArea.class);

        /* Judge if file is changed. */
        if (!stagedArea.addition.containsKey(fileName)) {
            stagedArea.addition.put(fileName, addFileText);
        } else {
            String stagedFileText = stagedArea.addition.get(fileName);
            byte[] stagedFileByte = serialize(stagedFileText);
            String stagedFileSha1 = sha1((Object) stagedFileByte);
            if (!stagedFileSha1.equals(addFileSha1)) {
               stagedArea.addition.put(fileName, addFileText);
            }
        }

        /* Write staged area to index. */
        writeObject(index, stagedArea);
    }

    public static void commit(String message) {
        /* Return if there is no message of new commit. */
        if (message == null) {
            System.out.println("Please enter a commit message.");
            return;
        }

        /* Creates a new commit. */
        Commit newCommit = new Commit(message);

        /* Read branch from HEAD. */
        File headFile = join(GITLET_DIR, "HEAD");
        String head = readContentsAsString(headFile);
        /* Read parent commit of current branch. */
        File branchFile = join(BRANCH_DIR, head);
        String parentCommitSha1 = readContentsAsString(branchFile);
        File parentCommitFile = join(COMMIT_DIR, parentCommitSha1);
        Commit parentCommit = readObject(parentCommitFile, Commit.class);

        /* Read the staged area. */
        File index = join(GITLET_DIR, "index");
        StagedArea stagedArea = readObject(index, StagedArea.class);

        /* Return if no changes added to the commit. */
        if (stagedArea.addition.isEmpty()) {
            System.out.println("No changes added to the commit.");
            return;
        }

        /* Pass parent files to new commit. */
        newCommit.files = parentCommit.files;

        /* Iterate all added files. */
        for (String fileName: stagedArea.addition.keySet()) {
            String fileText = stagedArea.addition.get(fileName);

            Blobs blob = new Blobs(fileText);
            byte[] blobByte = serialize(blob);
            String blobSha1 = sha1((Object) blobByte);

            /* Write file byte to file. */
            File blobFile = join(BlOB_DIR, blobSha1);
            writeObject(blobFile, blob);

            /* Add file to commit. */
            newCommit.files.put(fileName, blobSha1);
        }

        /* Iterate all removed files. */
        for (String fileName: stagedArea.removal) {
            newCommit.files.remove(fileName);
        }

        /* Pass parent commit. */
        newCommit.parent.add(parentCommitSha1);

        /* Serialize new commit. */
        byte[] newCommitByte = serialize(newCommit);
        String newCommitSha1 = sha1((Object) newCommitByte);
        /* Store new commit to file. */
        File newComitFile = join(COMMIT_DIR, newCommitSha1);
        writeObject(newComitFile, newCommit);

        /* Update branch. */
        writeContents(branchFile, newCommitSha1);

        /* Clear staged area. */
        stagedArea.addition.clear();
        stagedArea.removal.clear();
        writeObject(index, stagedArea);
    }

    public static void rm(String fileName) {
        /* Read branch from HEAD. */
        File headFile = join(GITLET_DIR, "HEAD");
        String head = readContentsAsString(headFile);

        /* Read commit of current branch. */
        File branchFile = join(BRANCH_DIR, head);
        String currentCommitSha1 = readContentsAsString(branchFile);

        /* Read current commit. */
        File currentCommitFile = join(COMMIT_DIR, currentCommitSha1);
        Commit currentCommit = readObject(currentCommitFile, Commit.class);

        /* Read staged area. */
        File index = join(GITLET_DIR, "index");
        StagedArea stagedArea = readObject(index, StagedArea.class);

        /* If the file is staged and is not tracked. */
        if (!stagedArea.addition.containsKey(fileName) && !currentCommit.files.containsKey(fileName)) {
            System.out.println("No reason to remove the file.");
            return;
        }

        /* If the file is tracked. */
        if (currentCommit.files.containsKey(fileName)) {
            stagedArea.removal.add(fileName);
            File rmFile = join(CWD, fileName);
            restrictedDelete(rmFile);
        }

        /* Remove from staged area. */
        stagedArea.addition.remove(fileName);

        /* Store stagedArea to index. */
        writeObject(index, stagedArea);
    }

    public static void log() {
        /* Read branch from HEAD. */
        File headFile = join(GITLET_DIR, "HEAD");
        String head = readContentsAsString(headFile);
        /* Read parent commit of current branch. */
        File branchFile = join(BRANCH_DIR, head);
        String currentCommitSha1 = readContentsAsString(branchFile);

        /* Read current commit. */
        File currentCommitFile = join(COMMIT_DIR, currentCommitSha1);
        Commit currentCommit = readObject(currentCommitFile, Commit.class);

        /* Print history commit. */
        while (currentCommit != null) {
            System.out.println("===");
            System.out.println("commit " + currentCommitSha1);
            if (currentCommit.parent != null && currentCommit.parent.size() == 2) {
                System.out.println("Merge: " + currentCommit.parent.get(0).substring(0, 7)
                                             + " " + currentCommit.parent.get(1).substring(0, 7));
            }
            System.out.printf("Date: %s\n", currentCommit.time());
            System.out.println(currentCommit.message + "\n");

            if (currentCommit.parent == null) { break; }
            currentCommitSha1 = currentCommit.parent.get(0);
            currentCommitFile = join(COMMIT_DIR, currentCommitSha1);
            currentCommit = readObject(currentCommitFile, Commit.class);
        }
    }

    public static void globalLog() {
        /* Read commit directory. */
        File commitDirectory = join(COMMIT_DIR);
        /* Read all file names. */
        List<String> fileNames = plainFilenamesIn(commitDirectory);

        if (fileNames == null) { return; }

        for (String str: fileNames) {
            File file = join(COMMIT_DIR, str);
            Commit commit = readObject(file, Commit.class);

            /* Filter initial commit. */
            if (commit.message.equals("initial commit")) { continue; }

            /* Print global log. */
            System.out.println("===");
            System.out.println("commit " + str);
            if (commit.parent.size() != 1) {
                System.out.println("Merge: " + commit.parent.get(0).substring(0, 7)
                        + " " + commit.parent.get(1).substring(0, 7));
            }
            System.out.printf(Locale.US, "Date: %tc\n", commit.timestamp);
            System.out.println(commit.message + "\n");
        }
    }

    public static void find(String commitMessage) {
        /* Read .gitlet/commit directory. */
        File commitDirectory = join(COMMIT_DIR);
        /* Read all file names. */
        List<String> fileNames = plainFilenamesIn(commitDirectory);

        /* Return if this is no commit. */
        if (fileNames == null) { return; }

        /* Iterate all commits. */
        for (String str : fileNames) {
            File file = join(COMMIT_DIR, str);
            Commit commit = readObject(file, Commit.class);

            /* Filter initial commit. */
            if (commit.message.equals(commitMessage)) {
                System.out.println(str);
            }
        }
    }

    public static void status() {
        /* Read branch from HEAD. */
        File headFile = join(GITLET_DIR, "HEAD");
        String head = readContentsAsString(headFile);
        System.out.println("=== Branches ===");
        System.out.println("*" + head);

        /* Read .gitlet/branch directory. */
        File branchDirectory = join(BRANCH_DIR);
        /* Read all branches. */
        List<String> branches = plainFilenamesIn(branchDirectory);

        /* Print all other branches. */
        if (branches != null) {
            for (String branch: branches) {
                if (!branch.equals(head)) {
                    System.out.println(branch);
                }
            }
        }
        System.out.println();

        /* Read staged area. */
        File index = join(GITLET_DIR, "index");
        StagedArea stagedArea = readObject(index, StagedArea.class);

        /* Read staged files. */
        System.out.println("=== Staged Files ===");
        for (String file: stagedArea.addition.keySet()) {
            System.out.println(file);
        }
        System.out.println();

        /* Read removal files. */
        System.out.println("=== Removed Files ===");
        for (String file: stagedArea.removal) {
            System.out.println(file);
        }
        System.out.println();

        System.out.println("=== Modifications Not Staged For Commit ===\n");
        System.out.println("=== Untracked Files ===");
    }

    private static void checkoutHelper(String fileName, Commit currentCommit) {
        /* Get sha1 of current commit about file. */
        String commitFileSha1 = currentCommit.files.get(fileName);

        /* Read text from current commit. */
        File blobFile = join(BlOB_DIR, commitFileSha1);
        Blobs blob = readObject(blobFile, Blobs.class);
        String text = blob.text;

        /*Write contents to file. */
        File file = join(CWD, fileName);
        writeContents(file, text);
    }

    public static void checkout_fileName(String fileName) {
        /* Read branch from HEAD. */
        File headFile = join(GITLET_DIR, "HEAD");
        String head = readContentsAsString(headFile);

        /* Read commit of current branch. */
        File branchFile = join(BRANCH_DIR, head);
        String currentCommitSha1 = readContentsAsString(branchFile);

        /* Read current commit. */
        File currentCommitFile = join(COMMIT_DIR, currentCommitSha1);
        Commit currentCommit = readObject(currentCommitFile, Commit.class);

        /* If the file does not exist in the previous commit, abort. */
        if (!currentCommit.files.containsKey(fileName)) {
            System.out.println("File does not exist in that commit.");
            return;
        }

        /* Call checkout helper function to overwrite file. */
        checkoutHelper(fileName, currentCommit);
    }

    public static void checkout_commitId_fileName(String commitId, String fileName) {
        /* Find complete commit id. */
        commitId = check_commitId(commitId);

        /* Read the commit file with the given commit id. */
        File currentCommitFile = join(COMMIT_DIR, commitId);
        /* If no commit with the given id exists. */
        if (!currentCommitFile.exists()) {
            System.out.println("No commit with that id exists.");
            return;
        }
        /* Read commit object with given commit id. */
        Commit currentCommit = readObject(currentCommitFile, Commit.class);

        /* Read contents from current commit. */
        if (!currentCommit.files.containsKey(fileName)) {
            System.out.println("File does not exist in that commit.");
            return;
        }

        /* Call checkout helper function to overwrite file. */
        checkoutHelper(fileName, currentCommit);
    }

    public static void checkout_branchName(String branchName) {
        /* Read branch from HEAD. */
        File headFile = join(GITLET_DIR, "HEAD");
        String head = readContentsAsString(headFile);

        /* Read branch. */
        File branchFile = join(BRANCH_DIR, branchName);
        /* If no branch with that name exists, print. */
        if (!branchFile.exists()) {
            System.out.println("No such branch exists.");
            return;
        }

        /* If that branch is the current branch, print. */
        String currentBranch = readContentsAsString(headFile);
        if (currentBranch.equals(branchName)) {
            System.out.println("No need to checkout the current branch.");
            return;
        }
        /* Read commit object from current branch. */
        File currentBranchCommit = join(COMMIT_DIR, head);
        Commit currentCommit = readObject(currentBranchCommit, Commit.class);

        /* Read commit object from given branch. */
        String givenBranchSha1 = readContentsAsString(branchFile);
        File givenBranchCommit = join(COMMIT_DIR, givenBranchSha1);
        Commit givenCommit = readObject(givenBranchCommit, Commit.class);

        for (String fileName: givenCommit.files.keySet()) {
            File file = join(CWD, fileName);
            if (file.exists() && !currentCommit.files.containsKey(fileName)) {
                System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                return;
            }

            /* Call checkout helper function to overwrite file. */
            checkoutHelper(fileName, givenCommit);
        }

        /* Read the staged area. */
        File index = join(GITLET_DIR, "index");
        StagedArea stagedArea = readObject(index, StagedArea.class);

        /* Clear staged area. */
        stagedArea.addition.clear();
        stagedArea.removal.clear();
        writeObject(index, stagedArea);
    }


    public static void branch(String branchName) {
        /* new branch file. */
        File newBranchFile = join(BRANCH_DIR, branchName);
        if (newBranchFile.exists()) {
            System.out.println("A branch with that name already exists.");
            return;
        }

        /* Read branch from HEAD. */
        File headFile = join(GITLET_DIR, "HEAD");
        String head = readContentsAsString(headFile);

        /* Read commit of current branch. */
        File currentBranchFile = join(BRANCH_DIR, head);
        String currentCommitSha1 = readContentsAsString(currentBranchFile);

        /* Store sha1 of current commit to new branch file. */
        writeContents(newBranchFile, currentCommitSha1);
    }

    public static void rm_branch(String branchName){
        File rmBranchFile = join(BRANCH_DIR, branchName);
        if (!rmBranchFile.exists()) {
            System.out.println("A branch with that name does not exist.");
            return;
        }

        /* Read branch from HEAD. */
        File headFile = join(GITLET_DIR, "HEAD");
        String head = readContentsAsString(headFile);

        if (branchName.equals(head)) {
            System.out.println("Cannot remove the current branch.");
            return;
        }

        /* Remove branch. */
        restrictedDelete(rmBranchFile);
    }

    public static String check_commitId(String commitId) {
        if (commitId.length() < 40) {
            int length = commitId.length();
            /* Read .gitlet/commit directory. */
            File commitDirectory = join(COMMIT_DIR);
            /* Read all branches. */
            List<String> commits = plainFilenamesIn(commitDirectory);

            assert commits != null;
            for (String commit: commits) {
                if (commit.substring(0, length).equals(commitId)) {
                    return commit;
                }
            }
        }

        return commitId;
    }
    public static void reset(String commitId) {
        /* Read commit object from given branch. */
        File givenCommitFile = join(COMMIT_DIR, commitId);
        if (!givenCommitFile.exists()) {
            System.out.println("No commit with that id exists.");
            return;
        }

        commitId = check_commitId(commitId);
        /* Read branch from HEAD. */
        File headFile = join(GITLET_DIR, "HEAD");
        String head = readContentsAsString(headFile);

        /* Read current branch. */
        File currentBranchFile = join(BRANCH_DIR, head);
        String currentCommitSha1 = readContentsAsString(currentBranchFile);

        /* Read commit object from current branch. */
        File currentCommitFile = join(COMMIT_DIR, currentCommitSha1);
        Commit currentCommit = readObject(currentCommitFile, Commit.class);
        Commit givenCommit = readObject(givenCommitFile, Commit.class);

        for (String fileName: givenCommit.files.keySet()) {
            File file = join(CWD, fileName);
            if (file.exists() && !currentCommit.files.containsKey(fileName)) {
                System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                return;
            }

            /* Call checkout helper function to overwrite file. */
            checkoutHelper(fileName, givenCommit);
        }

        writeObject(currentBranchFile, commitId);

        List<String> files = plainFilenamesIn(CWD);

        if (files == null) { return; }
        for (String file: files) {
            if (!givenCommit.files.containsKey(file)) {
                restrictedDelete(file);
            }
        }
    }
}
