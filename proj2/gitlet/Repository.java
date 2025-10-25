package gitlet;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
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
        File headFile = new File(GITLET_DIR, "head");
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

    /* Return branch name of head. */
    private static String readHeadBranch() {
        File headFile = join(GITLET_DIR, "head");
        return readContentsAsString(headFile);
    }

    /* Return branch commit sha1 of head. */
    private static String readHeadBranchCommitSha1() {
        String currentBranch = readHeadBranch();
        File currentBranchFile = join(BRANCH_DIR, currentBranch);
        return readContentsAsString(currentBranchFile);
    }

    /* Read branch commit object of head. */
    private static Commit readHeadBranchCommitObject() {
        String currentCommitSha1 = readHeadBranchCommitSha1();
        File currentCommitFile = join(COMMIT_DIR, currentCommitSha1);
        return readObject(currentCommitFile, Commit.class);
    }

    /* Read file text of commit from blob. */
    private static String readCommitFileBlob(Commit commit, String fileName){
        String fileBlobSha1 = commit.files.get(fileName);
        File blobFile =  join(BlOB_DIR, fileBlobSha1);
        Blobs blob = readObject(blobFile, Blobs.class);
        return blob.text;
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

        /* Read stagedArea object from index. */
        File index = join(GITLET_DIR, "index");
        StagedArea stagedArea = readObject(index, StagedArea.class);

        /* Stage file. */
        stagedArea.addition.put(fileName, addFileText);

        /* Read current commit of head branch. */
        Commit currentCommit = readHeadBranchCommitObject();
//        File headFile = join(GITLET_DIR, "head");
//        String branch = readContentsAsString(headFile);
//
//        File branchFile = join(BRANCH_DIR, branch);
//        String commitBranchSha1 = readContentsAsString(branchFile);
//
//        File commitBranchFile = join(COMMIT_DIR, commitBranchSha1);
//        Commit currentCommit = readObject(commitBranchFile, Commit.class);

        /* Judge if text of current commit is equal to added txt. */
        if (currentCommit.files.containsKey(fileName)) {
            /* Read file text of current commit from blob. */
            String text = readCommitFileBlob(currentCommit, fileName);

            /* If text of current commit is equal to added text, do not stage. */
            if (text.equals(addFileText)) {
                stagedArea.addition.remove(fileName);
            }
        }

        /* 如果暂存的内容和删除的内容相同，则取消暂存和取消删除。 */
        if (stagedArea.removal.containsKey(fileName)) {
            if (stagedArea.removal.get(fileName).equals(addFileText)) {
                stagedArea.addition.remove(fileName);
                stagedArea.removal.remove(fileName);
            }
        }

        /* Write staged area to index. */
        writeObject(index, stagedArea);
    }

    public static void commit(String message) {
        /* Return if there is no message of new commit. */
        if (message == null || message.isEmpty()) {
            System.out.println("Please enter a commit message.");
            return;
        }

        /* Creates a new commit. */
        Commit newCommit = new Commit(message);

        /* Read branch from HEAD. */
        String branch = readHeadBranch();

        /* Read parent commit of current branch. */
        File branchFile = join(BRANCH_DIR, branch);
        String parentCommitSha1 = readContentsAsString(branchFile);

        File parentCommitFile = join(COMMIT_DIR, parentCommitSha1);
        Commit parentCommit = readObject(parentCommitFile, Commit.class);

        /* Read the staged area. */
        File index = join(GITLET_DIR, "index");
        StagedArea stagedArea = readObject(index, StagedArea.class);

        /* Return if no changes added to the commit. */
        if (stagedArea.addition.isEmpty() && stagedArea.removal.isEmpty()) {
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
        for (String fileName: stagedArea.removal.keySet()) {
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
        /* Read branch commit object of head. */
        Commit currentCommit = readHeadBranchCommitObject();

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
            stagedArea.removal.put(fileName, currentCommit.files.get(fileName));
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
        String branch = readHeadBranch();

        /* Read parent commit of current branch. */
        File branchFile = join(BRANCH_DIR, branch);
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
        /* Print all commits of current branch. */
        log();

        /* Read branch commit object of head. */
        Commit currentCommit = readHeadBranchCommitObject();

        /* Read commit directory. */
        File commitDirectory = join(COMMIT_DIR);
        /* Read all file names. */
        List<String> fileNames = plainFilenamesIn(commitDirectory);

        /* Return if no commits. */
        if (fileNames == null) { return; }
        for (String fileName: fileNames) {
            /* If commit is not current commit, print. */
            if (currentCommit.files.containsKey(fileName)) {
                continue;
            }

            /* Iterates all commits. */
            File file = join(COMMIT_DIR, fileName);
            Commit commit = readObject(file, Commit.class);

            System.out.println("===");
            System.out.println("commit " + fileName);
            if (commit.parent != null && commit.parent.size() == 2) {
                System.out.println("Merge: " + commit.parent.get(0).substring(0, 7)
                        + " " + commit.parent.get(1).substring(0, 7));
            }
            System.out.printf("Date: %s\n", commit.time());
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
        boolean flag = false;
        for (String fileName: fileNames) {
            File file = join(COMMIT_DIR, fileName);
            Commit commit = readObject(file, Commit.class);

            /* Filter initial commit. */
            if (commit.message.equals(commitMessage)) {
                System.out.println(fileName);
                flag = true;
            }
        }

        /*  If no such commit exists, prints the error message. */
        if (!flag) {
            System.out.println("Found no commit with that message.");
        }
    }

    public static void status() {
        /* Read branch from HEAD. */
        String currentBranch = readHeadBranch();

        System.out.println("=== Branches ===");
        System.out.println("*" + currentBranch);

        /* Read .gitlet/branch directory. */
        File branchDirectory = join(BRANCH_DIR);
        /* Read all branches. */
        List<String> branches = plainFilenamesIn(branchDirectory);

        /* Print all other branches. */
        if (branches != null) {
            for (String branch: branches) {
                if (!branch.equals(currentBranch)) {
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
        for (String file: stagedArea.removal.keySet()) {
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
        /* Read branch commit object of head. */
        Commit currentCommit = readHeadBranchCommitObject();

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
        String currentBranch = readHeadBranch();

        /* Read branch. */
        File branchFile = join(BRANCH_DIR, branchName);
        /* If no branch with that name exists, print. */
        if (!branchFile.exists()) {
            System.out.println("No such branch exists.");
            return;
        }

        /* If that branch is the current branch, print. */
        if (currentBranch.equals(branchName)) {
            System.out.println("No need to checkout the current branch.");
            return;
        }

        /* Read branch commit object of head. */
        Commit currentCommit = readHeadBranchCommitObject();

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

        /* Delete files that not exits in given commit. */
        List<String> fileNames = plainFilenamesIn(CWD);
        for (String fileName: fileNames) {
            if (!givenCommit.files.containsKey(fileName)) {
                File file = join(CWD, fileName);
                restrictedDelete(file);
            }
        }

        /* Read the staged area. */
        File index = join(GITLET_DIR, "index");
        StagedArea stagedArea = readObject(index, StagedArea.class);

        /* Clear staged area. */
        stagedArea.addition.clear();
        stagedArea.removal.clear();
        writeObject(index, stagedArea);

        File headFile = join(GITLET_DIR, "head");
        writeContents(headFile, branchName);
    }


    public static void branch(String branchName) {
        /* new branch file. */
        File newBranchFile = join(BRANCH_DIR, branchName);
        if (newBranchFile.exists()) {
            System.out.println("A branch with that name already exists.");
            return;
        }

        /* Read branch from HEAD. */
        String currentBranch = readHeadBranch();

        /* Read commit of current branch. */
        File currentBranchFile = join(BRANCH_DIR, currentBranch);
        String currentCommitSha1 = readContentsAsString(currentBranchFile);

        /* Store sha1 of current commit to new branch file. */
        writeContents(newBranchFile, currentCommitSha1);
    }

    public static void rm_branch(String branchName){
        /* If a branch with the given name does not exist, aborts. */
        File rmBranchFile = join(BRANCH_DIR, branchName);
        if (!rmBranchFile.exists()) {
            System.out.println("A branch with that name does not exist.");
            return;
        }

        /* Read branch from HEAD. */
        String currentBranch = readHeadBranch();
        if (branchName.equals(currentBranch)) {
            System.out.println("Cannot remove the current branch.");
            return;
        }

        /* Remove branch. */
        rmBranchFile.delete();
    }

    private static String check_commitId(String commitId) {
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

    /* Find split point. */
    private static String findSplitPoint(String branchCommitSha1, String givenBranchCommitSha1) {
        HashMap<String, Integer> map = new HashMap<>();
        File branchCommitFile = join(COMMIT_DIR, branchCommitSha1);
        Commit branchCommit = readObject(branchCommitFile, Commit.class);

        while (branchCommit != null) {
            map.put(branchCommitSha1, 1);

            if (branchCommit.parent == null) { break; }
            branchCommitSha1 = branchCommit.parent.get(0);
            branchCommitFile = join(COMMIT_DIR, branchCommitSha1);
            branchCommit = readObject(branchCommitFile, Commit.class);
        }

        File givenBranchCommitFile = join(COMMIT_DIR, givenBranchCommitSha1);
        Commit givenBranchCommit = readObject(givenBranchCommitFile, Commit.class);

        while (givenBranchCommit != null) {
            if (map.containsKey(givenBranchCommitSha1)) {
                return givenBranchCommitSha1;
            }

            if (givenBranchCommit.parent == null) { break; }
            givenBranchCommitSha1 = givenBranchCommit.parent.get(0);
            givenBranchCommitFile = join(COMMIT_DIR, givenBranchCommitSha1);
            givenBranchCommit= readObject(givenBranchCommitFile, Commit.class);
        }

        return null;
    }

    private static String commitFileText(Commit commit, String fileName) {
        if (!commit.files.containsKey(fileName)) {
            return null;
        }

        /* Get sha1 of current commit about file. */
        String fileBlobSha1 = commit.files.get(fileName);

        /* Read text from current commit. */
        File fileBlob = join(BlOB_DIR, fileBlobSha1);
        Blobs blob = readObject(fileBlob, Blobs.class);
        return blob.text;
    }


    public static void merge(String givenBranch) {
        String branchCommitSha1= readHeadBranchCommitSha1();

        File givenBranchFile = join(BRANCH_DIR, givenBranch);
        String givenBranchCommitSha1 = readContentsAsString(givenBranchFile);

        /* Find split point. */
        String splitPointCommitSha1 = findSplitPoint(branchCommitSha1, givenBranchCommitSha1);

        assert splitPointCommitSha1 != null;
        if (splitPointCommitSha1.equals(givenBranchCommitSha1)) {
            System.out.println("Given branch is an ancestor of the current branch.");
            return;
        }

        if (splitPointCommitSha1.equals(branchCommitSha1)) {
            System.out.println("Current branch fast-forwarded.");
            return;
        }

        File splitPointCommitFile = join(COMMIT_DIR, splitPointCommitSha1);
        File branchCommitFile = join(COMMIT_DIR, branchCommitSha1);
        File givenBranchCommitFile = join(COMMIT_DIR, givenBranchCommitSha1);
        Commit splitPointCommit= readObject(splitPointCommitFile, Commit.class);
        Commit branchCommit = readObject(branchCommitFile, Commit.class);
        Commit givenBranchCommit = readObject(givenBranchCommitFile, Commit.class);

        HashSet<String> commitFiles = new HashSet<>();

        for (String fileName: splitPointCommit.files.keySet()) {
            commitFiles.add(fileName);
        }
        for (String fileName: branchCommit.files.keySet()) {
            commitFiles.add(fileName);
        }
        for (String fileName: givenBranchCommit.files.keySet()) {
            commitFiles.add(fileName);
        }

        for (String fileName: commitFiles) {
            String splitPointCommitText = commitFileText(splitPointCommit, fileName);
            String branchCommitText = commitFileText(branchCommit, fileName);
            String givenBranchCommitText = commitFileText(givenBranchCommit, fileName);

            /* 任何在分割点不存在且仅存在于当前分支中的文件都应保持原样 */
            if (splitPointCommitText == null && branchCommitText != null && givenBranchCommitText == null) {
                continue;
            }

            /* 任何在分割点不存在并且仅存在于给定分支中的文件都应被检出并暂存 */
            if (splitPointCommitText == null && branchCommitText == null && givenBranchCommitText != null) {
                checkout_commitId_fileName(givenBranchCommitSha1, fileName);
                add(fileName);
            }

            /* 任何存在于分割点、在当前分支中未修改、在给定分支中不存在的文件都应被删除（并且不被跟踪）*/
            if (splitPointCommitText != null && branchCommitText != null && givenBranchCommitText == null) {
                if (splitPointCommitText.equals(branchCommitText)) {
                    rm(fileName);
                } else {
                    File file = join(CWD, fileName);
                    writeContents(file, "<<<<<<< HEAD\n");
                    writeContents(file, branchCommitText);
                    writeContents(file, "\n=======");
                    writeContents(file, "\n>>>>>>>");
                }
            }

            /* 任何存在于分割点、在指定分支中未修改且在当前分支中不存在的文件都应保持不存在状态 */
            if (splitPointCommitText != null &&  branchCommitText == null && givenBranchCommitText != null ) {
                    if (!splitPointCommitText.equals(givenBranchCommitText)) {
                        File file = join(CWD, fileName);
                        writeContents(file, "<<<<<<< HEAD\n");
                        writeContents(file, givenBranchCommitText);
                        writeContents(file, "\n=======");
                        writeContents(file, "\n>>>>>>>");
                    }
            }

            /* 任何自分割点以来在给定分支中被修改过，但在当前分支中未被修改过的文件，
                都应更改为其在给定分支中的版本（从给定分支前端的提交签出）。
                然后，这些文件都将自动暂存。需要澄清的是，如果某个文件“自分割点以来在给定分支中被修改过”，
                则意味着该文件在给定分支前端提交中的版本与文件在分割点的版本内容不同。记住：blob 是内容可寻址的！
            */
            if (branchCommitText.equals(splitPointCommitText) && !givenBranchCommitText.equals(splitPointCommitText)) {
                checkout_commitId_fileName(givenBranchCommitSha1, fileName);
                add(fileName);
            }

            /* 任何在当前分支和指定分支中以相同方式修改的文件
            （例如，两个文件现在具有相同的内容或都被删除）在合并过程中保持不变。
            如果某个文件从当前分支和指定分支中都被删除，但工作目录中存在同名文件，
            则该文件将保持不变，并且在合并过程中仍然不可见（既不被跟踪也不被暂存）。
             */
            if (!branchCommitText.equals(splitPointCommitText) && !givenBranchCommitText.equals(splitPointCommitText)) {
                if (branchCommitText.equals(givenBranchCommitText)) {
                    continue;
                } else {
                    /* 处理冲突*/
                    File file = join(CWD, fileName);
                    writeContents(file, "<<<<<<< HEAD\n");
                    writeContents(file, branchCommitText);
                    writeContents(file, "\n=======");
                    writeContents(file, givenBranchCommitText);
                    writeContents(file, "\n>>>>>>>");
                }
            }
            /* 自分割点以来，在当前分支中已修改但在给定分支中未修改的任何文件都应保持原样 */
            if (!branchCommitText.equals(splitPointCommitText) && givenBranchCommitText.equals(splitPointCommitText)) {
                continue;
            }
        }
    }
}
