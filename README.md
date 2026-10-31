# 99 Step and 7 Falls - Video Game App
### by Riordin Dupree

## Prerequisites

Before attempting to start running the application locally, you will need to ensure that your system has the following installed
- **Java 17** (or newer), including `java` and `javac`
- **curl or wget**
- **unzip or tar**
- **PostgreSQL 17** (or newer) along with psql

The files curl/wget and unzip/tar are used within start.sh, on the first run, to download the Maven dependencies and wrapper, and to open the wrapper respectively

## Running the Program Locally

To run the program locally, pull the repository from github, and cd into the directory through a terminal. Inside this directory is the `start.sh` and `setup-local-db.sh` files.

Prior to running the setup files, a path must be connected between PostgreSQL's bin folder to PATH. To attach the two files, you can use the following command. Note, the following command assumes that the files are stored within `Program Files` and that the installed version is 17. The statement may need to be changed to reflect the filepath where PostgreSQL, or the version of PostgreSQL that you have installed.

```
    export PATH="$PATH:/c/Program Files/PostgreSQL/17/bin"
```

To ensure that PostgreSQL's bin directory was properly added to the PATH, you can type the following.

```
    psql --version
```

This will let you know which version of PostgreSQL you have installed, as well as if it is installed or reachable from the current directory.

After this, you can start the database using the following command.

```
    ./setup-local-db.sh
```

The program will prompt you to insert a password. The password will be whatever password you had created when installing PostgreSQL. This will create a `.env.local` file which is automatically excluded by the gitignore.

The setup script creates the local database and user, initializes the schema once, and refreshes the sample monster/item and test-user data each time it is run. It removes the matching seeded rows before re-inserting them; unrelated player and run records are left in place.

After that is complete, start the application using:

```
 ./start.sh
```

This will have the server start running the application locally on port 3000.

To view the webpage, enter into the browser, and in the searchbar, type `http://localhost:3000/` to see the application.

## Project Description

The project creates a basic layout for my application.  All the pages have been placed in as a simple default layout, styled using Bootstraps. Currently, all of the pages lack functionality.

The project plan can be found at `https://docs.google.com/document/d/1fUvD8a-E7638cirqe-HS2MEyEfPe-9G5X58lvsiYLhM/edit?tab=t.0`

To view the project after it is after the last update to the EC2 server, go to the following webpage: `http://44.251.58.31/`

The version found on EC2 is currently running off of commit `https://github.com/riordindupree/CS408-FullStack-FinalProject/commit/bb074224e7c20397cb438b57d507ec4d16fad594`