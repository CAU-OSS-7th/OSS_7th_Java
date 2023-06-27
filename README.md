file-manager
============

[![Maven Central](https://img.shields.io/maven-central/v/com.github.javadev/filemanager.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.github.javadev%22%20AND%20a%3A%22filemanager%22)
[![Java CI](https://github.com/javadev/file-manager/actions/workflows/maven.yml/badge.svg)](https://github.com/javadev/file-manager/actions/workflows/maven.yml)
[![CodeQL](https://github.com/javadev/file-manager/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/javadev/file-manager/actions/workflows/codeql-analysis.yml)

A java/swing basic File Manager in MacOS. The original project is [here](https://github.com/javadev/file-manager).
[MIT License]

Before you use it, you should install Java 11 or higher version.
# How to execute
Clone this repository, and build the repository in your IDE(Intelij IDEA) or build the filemanager.jar by the command below.

"java -jar filemanager.jar"

(please only place the filemanager.jar file in the directory. If you place the filemanager.jar file in other position, the program will not work.)

![image](https://user-images.githubusercontent.com/53044069/236477124-1bc5c460-9df3-432d-a684-e2a01c44e65e.png)

# Make your git repository
After execute the program, click the directory on the left side, and open the directory by touch the "V" shape button or double click.
<img width="1154" alt="image" src="https://user-images.githubusercontent.com/53044069/236835158-82ceec8d-c876-4954-926d-20a95c2301e3.png">

Click the "new" button and make a new directory for git service.
<img width="1155" alt="image" src="https://user-images.githubusercontent.com/53044069/236835700-d3ff559a-7615-4441-ad64-31f5563411c3.png">

Click a new directory, click "git init" button and touch the "예(Y)". then you make a local git repository!
<img width="1155" alt="image" src="https://user-images.githubusercontent.com/53044069/236835897-efad5e4d-d39c-4670-9596-9f32f135f97d.png">

# Use your git services
You can use various git instructions(add, restore, restore --staged, mv, rm, rm --cached, commit) by the buttons and recognize the file's status by checking the colors.

(Red: untracked/new file, Green: staged, Brown: modified/unstaged)
<img width="1153" alt="image" src="https://user-images.githubusercontent.com/53044069/236837761-874567b2-e7b2-4f86-b6a8-37d6417cf13b.png">

By the "git commit" button, you can commit the files in the commit table.
<img width="1156" alt="image" src="https://user-images.githubusercontent.com/53044069/236838021-da3404b4-8478-4342-a825-f4af5a93d75c.png">


## Create your new branch
You can also use git branch services.
<img width="1333" alt="image" src="https://github.com/CAU-OSS-7th/OSS_7th_Java/assets/53044069/b888a3e1-d339-4572-a22c-7b7199f800a6">
You can check your current git branch by "Current Git Branch: " space.

<img width="1130" alt="image" src="https://github.com/CAU-OSS-7th/OSS_7th_Java/assets/53044069/3f12637a-5515-45e7-8052-5bfecf555758">
If you are in a git repository, you can create new branch by the "Git create branch" button.


## Manage your various branches
<img width="695" alt="image" src="https://github.com/CAU-OSS-7th/OSS_7th_Java/assets/53044069/e39ed845-a478-41b2-88f1-2d17a662da38">

Also you can rename, merge, delete, or checkout your branch by "Git Branch Manager" button.


<img width="301" alt="image" src="https://github.com/CAU-OSS-7th/OSS_7th_Java/assets/53044069/9e888e84-053b-449f-b847-632c05936fb6">
<img width="701" alt="image" src="https://github.com/CAU-OSS-7th/OSS_7th_Java/assets/53044069/0436b97a-6223-4048-b846-25a550072f76">

Touch a branch and change name by "Rename Branch" button. Then you can watch your branch's changed name.


<img width="695" alt="image" src="https://github.com/CAU-OSS-7th/OSS_7th_Java/assets/53044069/41fd2b21-1595-4bdf-8316-5e96e8012e13">

You can delete your branch by "Delete Branch" button.



<img width="696" alt="image" src="https://github.com/CAU-OSS-7th/OSS_7th_Java/assets/53044069/0b02e619-b915-47f6-b5de-90546cc58385">

You can checkout your branch by "Checkout Branch" button.



<img width="697" alt="image" src="https://github.com/CAU-OSS-7th/OSS_7th_Java/assets/53044069/62a78b35-23f4-4c8e-bd15-2f61d688feba">

And you can merge your current branch with target(selected) branch by "Merge Branch" button.


## Clone your git repository
<img width="502" alt="image" src="https://github.com/CAU-OSS-7th/OSS_7th_Java/assets/53044069/3f4e0ee3-4d75-48bb-86a8-de8666581723">

If you are in non-git repository, you can clone new git repository with repository address by "Git Clone" button.


<img width="495" alt="image" src="https://github.com/CAU-OSS-7th/OSS_7th_Java/assets/53044069/1fa7fbaa-78bc-4d3f-88ee-fb396c84b995">

But in private-repository, you must write you github ID and personal access token.
If you write your ID and token correctly and clone successfully, you can find them in IDToken.txt file.

## Analyzing your commit history
You can watch your commit graph by "Git commit history" button. (git log)

<img width="931" alt="image" src="https://github.com/CAU-OSS-7th/OSS_7th_Java/assets/53044069/6de1a7cf-a020-4616-b8fb-785d51a6a923">

if you click a commit, you can confirm its information.

<img width="928" alt="image" src="https://github.com/CAU-OSS-7th/OSS_7th_Java/assets/53044069/78284f68-e61a-479b-a177-e792006f2308">

You can also find the difference between the commit and its parent commit by "Show Diff" button.

We always welcome contribution :)
