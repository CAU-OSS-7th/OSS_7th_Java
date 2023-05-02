/*
The MIT License

Copyright (c) 2015-2023 Valentyn Kolesnikov (https://github.com/javadev/file-manager)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */
package com.github.filemanager;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileSystemView;
import javax.swing.table.*;
import javax.swing.tree.*;

import org.apache.commons.io.FileUtils;

/**
 * A basic File Manager. Requires 1.6+ for the Desktop &amp; SwingWorker classes, amongst other
 * minor things.
 *
 * <p>Includes support classes FileTableModel &amp; FileTreeCellRenderer.
 *
 * <p>TODO Bugs
 *
 * <ul>
 *   <li>Still throws occasional AIOOBEs and NPEs, so some update on the EDT must have been missed.
 *   <li>Fix keyboard focus issues - especially when functions like rename/delete etc. are called
 *       that update nodes &amp; file lists.
 *   <li>Needs more testing in general.
 *       <p>TODO Functionality
 *   <li>Implement Read/Write/Execute checkboxes
 *   <li>Implement Copy
 *   <li>Extra prompt for directory delete (camickr suggestion)
 *   <li>Add File/Directory fields to FileTableModel
 *   <li>Double clicking a directory in the table, should update the tree
 *   <li>Move progress bar?
 *   <li>Add other file display modes (besides table) in CardLayout?
 *   <li>Menus + other cruft?
 *   <li>Implement history/back
 *   <li>Allow multiple selection
 *   <li>Add file search
 * </ul>
 */
public class FileManager {

    /**
     * Title of the application
     */
    public static final String APP_TITLE = "FileMan";
    /**
     * Used to open/edit/print files.
     */
    private Desktop desktop;
    /**
     * Provides nice icons and names for files.
     */
    private FileSystemView fileSystemView;

    /**
     * currently selected File.
     */
    private File currentFile;

    /**
     * Main GUI container
     */
    private JPanel gui;

    /**
     * File-system tree. Built Lazily
     */
    private JTree tree;

    private DefaultTreeModel treeModel;

    /**
     * Directory listing
     */
    private JTable table;

    private JProgressBar progressBar;
    /**
     * Table model for File[].
     */
    private FileTableModel fileTableModel;

    private ListSelectionListener listSelectionListener;
    private boolean cellSizesSet = false;
    private int rowIconPadding = 6;

    /* File controls. */
    private JButton openFile;
    private JButton printFile;
    private JButton editFile;
    private JButton deleteFile;
    private JButton newdeltefile;
    private JButton newFile;
    private JButton copyFile;

    /* 새로 추가한 git 버튼 */
    private JButton gitInitFile; // git init
    private JButton gitAddFile; // git add
    private JButton gitCommitFile; // git commit
    private JButton gitMvFile; // git mv
    private JButton gitRmFile; // git rm
    private JButton gitRmCachedFile; // git rm --cached
    private JButton gitRestoreFile; // git restore
    private JButton gitRestoreStagedFile; // git restore --staged


    /* File details. */
    private JLabel fileName;
    private JTextField path;
    private JLabel date;
    private JLabel size;
    private JCheckBox readable;
    private JCheckBox writable;
    private JCheckBox executable;
    private JRadioButton isDirectory;
    private JRadioButton isFile;

    /* GUI options/containers for new File/Directory creation.  Created lazily. */
    private JPanel newFilePanel;
    private JRadioButton newTypeFile;
    private JTextField name;

    public Container getGui() {
        if (gui == null) {
            gui = new JPanel(new BorderLayout(3, 3));
            gui.setBorder(new EmptyBorder(5, 5, 5, 5));

            fileSystemView = FileSystemView.getFileSystemView();
            desktop = Desktop.getDesktop();

            JPanel detailView = new JPanel(new BorderLayout(3, 3));
            // fileTableModel = new FileTableModel();

            table = new JTable();
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.setAutoCreateRowSorter(true);
            table.setShowVerticalLines(false);

            listSelectionListener = new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent lse) {
                    int row = table.getSelectionModel().getLeadSelectionIndex();
                    setFileDetails(((FileTableModel) table.getModel()).getFile(row));
                }
            };
            table.getSelectionModel().addListSelectionListener(listSelectionListener);
            JScrollPane tableScroll = new JScrollPane(table);
            Dimension d = tableScroll.getPreferredSize();
            tableScroll.setPreferredSize(new Dimension((int) d.getWidth(), (int) d.getHeight() / 2));
            detailView.add(tableScroll, BorderLayout.CENTER);

            // the File tree
            DefaultMutableTreeNode root = new DefaultMutableTreeNode();
            treeModel = new DefaultTreeModel(root);

            TreeSelectionListener treeSelectionListener = new TreeSelectionListener() {
                public void valueChanged(TreeSelectionEvent tse) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) tse.getPath().getLastPathComponent();
                    showChildren(node);
                    setFileDetails((File) node.getUserObject());
                }
            };

            // show the file system roots.
            File[] roots = fileSystemView.getRoots();
            for (File fileSystemRoot : roots) {
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(fileSystemRoot);
                root.add(node);
                // showChildren(node);
                //
                File[] files = fileSystemView.getFiles(fileSystemRoot, true);
                for (File file : files) {
                    if (file.isDirectory()) {
                        node.add(new DefaultMutableTreeNode(file));
                    }
                }
                //
            }


            tree = new JTree(treeModel);
            tree.setRootVisible(false);
            tree.addTreeSelectionListener(treeSelectionListener);
            tree.setCellRenderer(new FileTreeCellRenderer());
            tree.expandRow(0);
            JScrollPane treeScroll = new JScrollPane(tree);

            // as per trashgod tip
            tree.setVisibleRowCount(15);

            Dimension preferredSize = treeScroll.getPreferredSize();
            Dimension widePreferred = new Dimension(200, (int) preferredSize.getHeight());
            treeScroll.setPreferredSize(widePreferred);

            // details for a File
            JPanel fileMainDetails = new JPanel(new BorderLayout(4, 2));
            fileMainDetails.setBorder(new EmptyBorder(0, 6, 0, 6));

            JPanel fileDetailsLabels = new JPanel(new GridLayout(0, 1, 2, 2));
            fileMainDetails.add(fileDetailsLabels, BorderLayout.WEST);

            JPanel fileDetailsValues = new JPanel(new GridLayout(0, 1, 2, 2));
            fileMainDetails.add(fileDetailsValues, BorderLayout.CENTER);

            fileDetailsLabels.add(new JLabel("File", JLabel.TRAILING));
            fileName = new JLabel();
            fileDetailsValues.add(fileName);
            fileDetailsLabels.add(new JLabel("Path/name", JLabel.TRAILING));
            path = new JTextField(5);
            path.setEditable(false);
            fileDetailsValues.add(path);
            fileDetailsLabels.add(new JLabel("Last Modified", JLabel.TRAILING));
            date = new JLabel();
            fileDetailsValues.add(date);
            fileDetailsLabels.add(new JLabel("File size", JLabel.TRAILING));
            size = new JLabel();
            fileDetailsValues.add(size);
            fileDetailsLabels.add(new JLabel("Type", JLabel.TRAILING));

            JPanel flags = new JPanel(new FlowLayout(FlowLayout.LEADING, 4, 0));
            isDirectory = new JRadioButton("Directory");
            isDirectory.setEnabled(false);
            flags.add(isDirectory);

            isFile = new JRadioButton("File");
            isFile.setEnabled(false);
            flags.add(isFile);
            fileDetailsValues.add(flags);

            int count = fileDetailsLabels.getComponentCount();
            for (int ii = 0; ii < count; ii++) {
                fileDetailsLabels.getComponent(ii).setEnabled(false);
            }

            JToolBar toolBar = new JToolBar();
            // mnemonics stop working in a floated toolbar
            toolBar.setFloatable(false);

            openFile = new JButton("Open");
            openFile.setMnemonic('o');

            openFile.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    try {
                        desktop.open(currentFile);
                    } catch (Throwable t) {
                        showThrowable(t);
                    }
                    gui.repaint();
                }
            });
            toolBar.add(openFile);

            editFile = new JButton("Edit");
            editFile.setMnemonic('e');
            editFile.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    try {
                        desktop.edit(currentFile);
                    } catch (Throwable t) {
                        showThrowable(t);
                    }
                }
            });
            toolBar.add(editFile);

            printFile = new JButton("Print");
            printFile.setMnemonic('p');
            printFile.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    try {
                        desktop.print(currentFile);
                    } catch (Throwable t) {
                        showThrowable(t);
                    }
                }
            });
            toolBar.add(printFile);

            // Check the actions are supported on this platform!
            openFile.setEnabled(desktop.isSupported(Desktop.Action.OPEN));
            editFile.setEnabled(desktop.isSupported(Desktop.Action.EDIT));
            printFile.setEnabled(desktop.isSupported(Desktop.Action.PRINT));

            toolBar.addSeparator();

            newFile = new JButton("New");
            newFile.setMnemonic('n');
            newFile.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    newFile();
                }
            });
            toolBar.add(newFile);

            copyFile = new JButton("Copy");
            copyFile.setMnemonic('c');
            copyFile.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    showErrorMessage("'Copy' not implemented.", "Not implemented.");
                }
            });
            toolBar.add(copyFile);

            JButton renameFile = new JButton("Rename");
            renameFile.setMnemonic('r');
            renameFile.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    renameFile();
                }
            });
            toolBar.add(renameFile);

            deleteFile = new JButton("Delete");
            deleteFile.setMnemonic('d');
            deleteFile.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    deleteFile();
                }
            });
            toolBar.add(deleteFile);

            /* git 버튼 객체 생성 로직 */

            // 1. init 버튼
            gitInitFile = new JButton("Git init");
            gitInitFile.setMnemonic('I');
            gitInitFile.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    gitInitFile();
                }
            });
            toolBar.add(gitInitFile);

            // 2. add 버튼
            gitAddFile = new JButton("Git add");
            gitAddFile.setMnemonic('A');
            gitAddFile.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    gitAddFile();
                }
            });
            toolBar.add(gitAddFile);

            // 3. commit 버튼
            gitCommitFile = new JButton("Git commit");
            gitCommitFile.setMnemonic('C');
            gitCommitFile.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    // 여기에다가 commit 로직 추가
                }
            });
            toolBar.add(gitCommitFile);

            // 4. mv 버튼
            gitMvFile = new JButton("Git mv");
            gitMvFile.setMnemonic('M');
            gitMvFile.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    // 여기에다가 mv 로직 추가
                }
            });
            toolBar.add(gitMvFile);

            // 5. rm 버튼
            gitRmFile = new JButton("Git rm");
            gitRmFile.setMnemonic('R');
            gitRmFile.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    // 여기에다가 rm 로직 추가
                }
            });
            toolBar.add(gitRmFile);

            // 6. rm --cached 버튼
            gitRmCachedFile = new JButton("Git rm --cached");
            gitRmCachedFile.setMnemonic('R');
            gitRmCachedFile.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    // 여기에다가 rm --cached 로직 추가
                }
            });
            toolBar.add(gitRmCachedFile);

            // 7. restore 버튼
            gitRestoreFile = new JButton("Git restore");
            gitRestoreFile.setMnemonic('R');
            gitRestoreFile.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    // 여기에다가 restore 로직 추가
                }
            });
            toolBar.add(gitRestoreFile);

            // 8. restore --staged 버튼
            gitRestoreStagedFile = new JButton("Git restore --staged");
            gitRestoreStagedFile.setMnemonic('R');
            gitRestoreStagedFile.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    // 여기에다가 restore --staged 로직 추가
                }
            });
            toolBar.add(gitRestoreStagedFile);

            toolBar.addSeparator();

            readable = new JCheckBox("Read  ");
            readable.setMnemonic('a');
            // readable.setEnabled(false);
            toolBar.add(readable);

            writable = new JCheckBox("Write  ");
            writable.setMnemonic('w');
            // writable.setEnabled(false);
            toolBar.add(writable);

            executable = new JCheckBox("Execute");
            executable.setMnemonic('x');
            // executable.setEnabled(false);
            toolBar.add(executable);

            JPanel fileView = new JPanel(new BorderLayout(3, 3));

            fileView.add(toolBar, BorderLayout.NORTH);
            fileView.add(fileMainDetails, BorderLayout.CENTER);

            detailView.add(fileView, BorderLayout.SOUTH);

            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScroll, detailView);
            gui.add(splitPane, BorderLayout.CENTER);

            JPanel simpleOutput = new JPanel(new BorderLayout(3, 3));
            progressBar = new JProgressBar();
            simpleOutput.add(progressBar, BorderLayout.EAST);
            progressBar.setVisible(false);

            gui.add(simpleOutput, BorderLayout.SOUTH);
        }
        return gui;
    }

    public void showRootFile() {
        // ensure the main files are displayed
        tree.setSelectionInterval(0, 0);
    }

    private TreePath findTreePath(File find) {
        for (int ii = 0; ii < tree.getRowCount(); ii++) {
            TreePath treePath = tree.getPathForRow(ii);
            Object object = treePath.getLastPathComponent();
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) object;
            File nodeFile = (File) node.getUserObject();

            if (nodeFile.equals(find)) {
                return treePath;
            }
        }
        // not found!
        return null;
    }

    private void renameFile() {
        if (currentFile == null) {
            showErrorMessage("No file selected to rename.", "Select File");
            return;
        }

        String renameTo = JOptionPane.showInputDialog(gui, "New Name");
        if (renameTo != null) {
            try {
                boolean directory = currentFile.isDirectory();
                TreePath parentPath = findTreePath(currentFile.getParentFile());
                DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) parentPath.getLastPathComponent();

                boolean renamed = currentFile.renameTo(new File(currentFile.getParentFile(), renameTo));
                if (renamed) {
                    if (directory) {
                        // rename the node..

                        // delete the current node..
                        TreePath currentPath = findTreePath(currentFile);
                        System.out.println(currentPath);
                        DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode) currentPath.getLastPathComponent();

                        treeModel.removeNodeFromParent(currentNode);

                        // add a new node..
                    }

                    showChildren(parentNode);
                } else {
                    String msg = "The file '" + currentFile + "' could not be renamed.";
                    showErrorMessage(msg, "Rename Failed");
                }
            } catch (Throwable t) {
                showThrowable(t);
            }
        }
        gui.repaint();
    }

    private void deleteFile() {
        if (currentFile == null) {
            showErrorMessage("No file selected for deletion.", "Select File");
            return;
        }

        int result = JOptionPane.showConfirmDialog(gui, "Are you sure you want to delete this file?", "Delete File", JOptionPane.ERROR_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            try {
                System.out.println("currentFile: " + currentFile);
                TreePath parentPath = findTreePath(currentFile.getParentFile());
                System.out.println("parentPath: " + parentPath);
                DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) parentPath.getLastPathComponent();
                System.out.println("parentNode: " + parentNode);

                boolean directory = currentFile.isDirectory();
                if (FileUtils.deleteQuietly(currentFile)) {
                    if (directory) {
                        // delete the node..
                        TreePath currentPath = findTreePath(currentFile);
                        System.out.println(currentPath);
                        DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode) currentPath.getLastPathComponent();

                        treeModel.removeNodeFromParent(currentNode);
                    }

                    showChildren(parentNode);
                } else {
                    String msg = "The file '" + currentFile + "' could not be deleted.";
                    showErrorMessage(msg, "Delete Failed");
                }
            } catch (Throwable t) {
                showThrowable(t);
            }
        }
        gui.repaint();
    }

    private void newFile() {
        if (currentFile == null) {
            showErrorMessage("No location selected for new file.", "Select Location");
            return;
        }

        if (newFilePanel == null) {
            newFilePanel = new JPanel(new BorderLayout(3, 3));

            JPanel southRadio = new JPanel(new GridLayout(1, 0, 2, 2));
            newTypeFile = new JRadioButton("File", true);
            JRadioButton newTypeDirectory = new JRadioButton("Directory");
            ButtonGroup bg = new ButtonGroup();
            bg.add(newTypeFile);
            bg.add(newTypeDirectory);
            southRadio.add(newTypeFile);
            southRadio.add(newTypeDirectory);

            name = new JTextField(15);

            newFilePanel.add(new JLabel("Name"), BorderLayout.WEST);
            newFilePanel.add(name);
            newFilePanel.add(southRadio, BorderLayout.SOUTH);
        }

        int result = JOptionPane.showConfirmDialog(gui, newFilePanel, "Create File", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                boolean created;
                File parentFile = currentFile;
                if (!parentFile.isDirectory()) {
                    parentFile = parentFile.getParentFile();
                }
                File file = new File(parentFile, name.getText());
                if (newTypeFile.isSelected()) {
                    created = file.createNewFile();
                } else {
                    created = file.mkdir();
                }
                if (created) {

                    TreePath parentPath = findTreePath(parentFile);
                    DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) parentPath.getLastPathComponent();

                    if (file.isDirectory()) {
                        // add the new node..
                        DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(file);

                        TreePath currentPath = findTreePath(currentFile);
                        DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode) currentPath.getLastPathComponent();

                        treeModel.insertNodeInto(newNode, parentNode, parentNode.getChildCount());
                    }

                    showChildren(parentNode);
                } else {
                    String msg = "The file '" + file + "' could not be created.";
                    showErrorMessage(msg, "Create Failed");
                }
            } catch (Throwable t) {
                showThrowable(t);
            }
        }
        gui.repaint();
    }

    private boolean isInGitRepository() { // 현재 directory가 git repository인지 판정하는 함수, 이 조건을 충족한 뒤 git 명령어를 실행해야 한다.
        if (currentFile == null) {
            showErrorMessage("No location selected for new file.", "Select Location");
            return false;
        }

        File[] files = currentFile.listFiles();
        for (File file : files) {
            if (file.getName().equals(".git")) { // 현재 디렉토리에 .git이 존재하는지 판정
                return true;
            }
        }
        return false;
    }

    private void gitInitFile() { // git init 명령어을 실행하는 함수
        if (currentFile == null) {
            showErrorMessage("No location selected for new file.", "Select Location");
            return;
        }

        try {
            File[] files = currentFile.listFiles();
            for (File file : files) {
                if (file.getName().equals(".git")) { // 현재 디렉토리에 .git이 존재하는지 판정
                    showErrorMessage("이 directory는 이미 git Repository입니다.", "Select Location");
                    return;
                }
            }

            int result = JOptionPane.showConfirmDialog(gui, "이 directory를 Git Repository로 등록하시겠습니까? '예'를 누르면 등록됩니다.", "git init", JOptionPane.ERROR_MESSAGE);
            // git init을 진짜 실행할건지 묻는 메시지 창
            if (result == JOptionPane.OK_OPTION) { // "예" 클릭 시 git init 명령어 실행
                String[] command = {"git", "init"};
                ProcessBuilder processBuilder = new ProcessBuilder(command);
                processBuilder.directory(currentFile);
                Process process = processBuilder.start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
                int exitCode = process.waitFor();
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 위의 isInGitRepostiroy()의 경우 단일 파일 선택 시 NullPointerException이 발생하므로 git status 명령어 실행을 통한
     * .git 존재 여부를 판단하는 새로운 isFileInGitRepository() 함수 선언
     */

    private boolean isFileInGitRepository(){
        String[] gitCheckCommand = {"git", "status"}; //Git status 명령어를 통해 간접적으로 .git 폴더 유무 확인
        ProcessBuilder processBuilder = new ProcessBuilder(gitCheckCommand);
        processBuilder.directory(currentFile.getParentFile()); //선택한 파일의 경로 반환
        Process process;
        int gitStatus = -1;

        try{
            process = processBuilder.start(); //git status 명령어를 선택한 파일의 경로에서 실행하여 git이 있는지 확인
            gitStatus = process.waitFor(); //만일 git status가 잘 실행됐다면 git repository에 있다는 것이고, 아니라면 에러코드를 반환
            if (gitStatus == 0){
                return true;
            }else{
                return false;
            }
        }catch (IOException | InterruptedException e){
            e.printStackTrace();
        }
        return false;
    }

    private void gitAddFile() { //선택한 파일을 stage하는 git add로직. "git add" 버튼을 누르면 이 로직이 실행된다.
        if (currentFile == null) {
            showErrorMessage("No location selected for new file.", "Select Location");
            return;
        }

        /** ----------------------------------------확인 바람------------------------------------------
         * 파일을 선택할 경우, directory가 아니므로 기존 isInGitRepository() 실행 시 Not a directory Error 발생.
         * 따라서, git status를 통해 선택한 파일이 git repository에 있는지 확인 후 add 실행.
         * git status는 parent directory까지 자동으로 탐색하므로 번거로운 일이 줄어들 것으로 예상.
         * 후에 필요할 시, 별도의 boolean 함수로 전환할 예정. => isFileInGitRepository로 함수화 완료. (5/2)
         * 파일을 직접 선택할 경우, processBuilder.directory()의 인자에는 Directory가 들어가야 하므로
         * currentFile.getparentFile() 을 통해, 파일이 존재하는 경로를 반환해야 함.
         */

        if (isFileInGitRepository()) { //현재 디렉토리에 .git이 있는 경우에만 add 실행가능하게 함.
            try{
                int result = JOptionPane.showConfirmDialog(gui, "해당 파일을 stage 하시겠습니까? '예'를 누르면 등록됩니다.", "git add", JOptionPane.ERROR_MESSAGE);

                if (result == JOptionPane.OK_OPTION) { // "예" 클릭 시 git add 명령어 실행
                    String[] gitAddCommand = {"git", "add", currentFile.getAbsolutePath()};
                    ProcessBuilder processBuilder = new ProcessBuilder(gitAddCommand);
                    processBuilder.directory(currentFile.getParentFile());
                    Process process = processBuilder.start();
                    int addStatus = process.waitFor(); //git add 명령어 정상 실행 여부

                    if (addStatus == 0){ // git add 명령어가 정상적으로 실행되어 status가 0일 경우
                        JOptionPane.showMessageDialog(gui, "성공적으로 파일을 stage 했습니다.");
                        System.out.println(currentFile);
                        System.out.println("staged");
                    }else{ //git add 명령어가 정상적으로 실행되지 않았을 경우
                        showErrorMessage("파일을 Stage하는 과정에서 오류가 발생했습니다.","git add error");
                    }
                }
            } catch (InterruptedException | IOException e){
                e.printStackTrace();
            }
        }else{ //.git이 존재하지 않는 경우 (git status 명령어가 실패했을 경우)
            showErrorMessage("선택한 파일은 git repository에 존재하지 않습니다.","git Add error");
        }
    }


    private void showErrorMessage(String errorMessage, String errorTitle) {
        JOptionPane.showMessageDialog(gui, errorMessage, errorTitle, JOptionPane.ERROR_MESSAGE);
    }

    private void showThrowable(Throwable t) {
        t.printStackTrace();
        JOptionPane.showMessageDialog(gui, t.toString(), t.getMessage(), JOptionPane.ERROR_MESSAGE);
        gui.repaint();
    }

    /**
     * Update the table on the EDT
     */
    private void setTableData(final File[] files) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (fileTableModel == null) {
                    fileTableModel = new FileTableModel();
                    table.setModel(fileTableModel);
                }
                table.getSelectionModel().removeListSelectionListener(listSelectionListener);
                fileTableModel.setFiles(files);
                table.getSelectionModel().addListSelectionListener(listSelectionListener);
                if (!cellSizesSet) {
                    Icon icon = fileSystemView.getSystemIcon(files[0]);

                    // size adjustment to better account for icons
                    table.setRowHeight(icon.getIconHeight() + rowIconPadding);

                    setColumnWidth(0, -1);
                    setColumnWidth(3, 60);
                    table.getColumnModel().getColumn(3).setMaxWidth(120);
                    setColumnWidth(4, -1);
                    setColumnWidth(5, -1);
                    setColumnWidth(6, -1);
                    setColumnWidth(7, -1);
                    setColumnWidth(8, -1);
                    setColumnWidth(9, -1);

                    cellSizesSet = true;
                }
            }
        });
    }

    private void setColumnWidth(int column, int width) {
        TableColumn tableColumn = table.getColumnModel().getColumn(column);
        if (width < 0) {
            // use the preferred width of the header..
            JLabel label = new JLabel((String) tableColumn.getHeaderValue());
            Dimension preferred = label.getPreferredSize();
            // altered 10->14 as per camickr comment.
            width = (int) preferred.getWidth() + 14;
        }
        tableColumn.setPreferredWidth(width);
        tableColumn.setMaxWidth(width);
        tableColumn.setMinWidth(width);
    }

    /**
     * Add the files that are contained within the directory of this node. Thanks to Hovercraft Full
     * Of Eels.
     */
    private void showChildren(final DefaultMutableTreeNode node) {
        tree.setEnabled(false);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);

        SwingWorker<Void, File> worker = new SwingWorker<Void, File>() {
            @Override
            public Void doInBackground() {
                File file = (File) node.getUserObject();
                if (file.isDirectory()) {
                    File[] files = fileSystemView.getFiles(file, true); // !!
                    if (node.isLeaf()) {
                        for (File child : files) {
                            if (child.isDirectory()) {
                                publish(child);
                            }
                        }
                    }
                    setTableData(files);
                }
                return null;
            }

            @Override
            protected void process(List<File> chunks) {
                for (File child : chunks) {
                    node.add(new DefaultMutableTreeNode(child));
                }
            }

            @Override
            protected void done() {
                progressBar.setIndeterminate(false);
                progressBar.setVisible(false);
                tree.setEnabled(true);
            }
        };
        worker.execute();
    }

    /**
     * Update the File details view with the details of this File.
     */
    private void setFileDetails(File file) {
        currentFile = file;
        Icon icon = fileSystemView.getSystemIcon(file);
        fileName.setIcon(icon);
        fileName.setText(fileSystemView.getSystemDisplayName(file));
        path.setText(file.getPath());
        date.setText(new Date(file.lastModified()).toString());
        size.setText(file.length() + " bytes");
        readable.setSelected(file.canRead());
        writable.setSelected(file.canWrite());
        executable.setSelected(file.canExecute());
        isDirectory.setSelected(file.isDirectory());

        isFile.setSelected(file.isFile());

        JFrame f = (JFrame) gui.getTopLevelAncestor();
        if (f != null) {
            f.setTitle(APP_TITLE + " :: " + fileSystemView.getSystemDisplayName(file));
        }

        gui.repaint();
    }

    public static boolean copyFile(File from, File to) throws IOException {

        boolean created = to.createNewFile();

        if (created) {
            FileChannel fromChannel = null;
            FileChannel toChannel = null;
            try {
                fromChannel = new FileInputStream(from).getChannel();
                toChannel = new FileOutputStream(to).getChannel();

                toChannel.transferFrom(fromChannel, 0, fromChannel.size());

                // set the flags of the to the same as the from
                to.setReadable(from.canRead());
                to.setWritable(from.canWrite());
                to.setExecutable(from.canExecute());
            } finally {
                if (fromChannel != null) {
                    fromChannel.close();
                }
                if (toChannel != null) {
                    toChannel.close();
                }
                return false;
            }
        }
        return created;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    // Significantly improves the look of the output in
                    // terms of the file names returned by FileSystemView!
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception weTried) {
                }
                JFrame f = new JFrame(APP_TITLE);
                f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                FileManager fileManager = new FileManager();
                f.setContentPane(fileManager.getGui());

                try {
                    URL urlBig = fileManager.getClass().getResource("fm-icon-32x32.png");
                    URL urlSmall = fileManager.getClass().getResource("fm-icon-16x16.png");
                    ArrayList<Image> images = new ArrayList<Image>();
                    images.add(ImageIO.read(urlBig));
                    images.add(ImageIO.read(urlSmall));
                    f.setIconImages(images);
                } catch (Exception weTried) {
                }

                f.pack();
                f.setLocationByPlatform(true);
                f.setMinimumSize(f.getSize());
                f.setVisible(true);

                fileManager.showRootFile();
            }
        });
    }
}

/**
 * A TableModel to hold File[].
 */
class FileTableModel extends AbstractTableModel {

    private File[] files;
    private FileSystemView fileSystemView = FileSystemView.getFileSystemView();
    private String[] columns = {"Icon", "File", "Path/name", "Size", "Last Modified", "R", "W", "E", "D", "F",};

    FileTableModel() {
        this(new File[0]);
    }

    FileTableModel(File[] files) {
        this.files = files;
    }

    public Object getValueAt(int row, int column) {
        File file = files[row];
        switch (column) {
            case 0:
                return fileSystemView.getSystemIcon(file);
            case 1:
                return fileSystemView.getSystemDisplayName(file);
            case 2:
                return file.getPath();
            case 3:
                return file.length();
            case 4:
                return file.lastModified();
            case 5:
                return file.canRead();
            case 6:
                return file.canWrite();
            case 7:
                return file.canExecute();
            case 8:
                return file.isDirectory();
            case 9:
                return file.isFile();
            default:
                System.err.println("Logic Error");
        }
        return "";
    }

    public int getColumnCount() {
        return columns.length;
    }

    public Class<?> getColumnClass(int column) {
        switch (column) {
            case 0:
                return ImageIcon.class;
            case 3:
                return Long.class;
            case 4:
                return Date.class;
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
                return Boolean.class;
        }
        return String.class;
    }

    public String getColumnName(int column) {
        return columns[column];
    }

    public int getRowCount() {
        return files.length;
    }

    public File getFile(int row) {
        return files[row];
    }

    public void setFiles(File[] files) {
        this.files = files;
        fireTableDataChanged();
    }
}

/**
 * A TreeCellRenderer for a File.
 */
class FileTreeCellRenderer extends DefaultTreeCellRenderer {

    private FileSystemView fileSystemView;

    private JLabel label;

    FileTreeCellRenderer() {
        label = new JLabel();
        label.setOpaque(true);
        fileSystemView = FileSystemView.getFileSystemView();
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        File file = (File) node.getUserObject();
        label.setIcon(fileSystemView.getSystemIcon(file));
        label.setText(fileSystemView.getSystemDisplayName(file));
        label.setToolTipText(file.getPath());

        if (selected) {
            label.setBackground(backgroundSelectionColor);
            label.setForeground(textSelectionColor);
        } else {
            label.setBackground(backgroundNonSelectionColor);
            label.setForeground(textNonSelectionColor);
        }

        return label;
    }
}
