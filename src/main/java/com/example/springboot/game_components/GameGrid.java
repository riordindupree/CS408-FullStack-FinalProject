package com.example.springboot.game_components;

/**
 * Stores data for game grid
 * 
 * GameGrid
 */
public class GameGrid {
    int current_x = 0;
    int current_y = 0;
    Room[][] gameGrid = new Room[5][5];
    
    public GameGrid(int x, int y, int opt1, int opt2, int opt3, int opt4){
        current_x = x;
        current_y = y;
    }

    // move up
    public void move_up(){
        if(5 > current_y){ return; }
        // increment current y
        current_y++;
    }

    // move down
    public void move_down(){
        if(0 < current_y){ return; }
        // increment current y
        current_y--;
    }

    // move right
    public void move_right(){
        if(5 > current_x){ return; }
        // increment current y
        current_x++;
    }

    // move left
    public void move_left(){
        if(0 < current_x){ return; }
        // increment current y
        current_x--;
    }

    // get data from current x and y value
    public int getCurrentData(int option){
        // retunrs content of current.
        return gameGrid[current_x][current_y].getRoom(option);
    }

    // get data from a given x and y value
    public int getData(int x, int y, int option){
        // retunrs content of current.
        return gameGrid[x][y].getRoom(option);
    }

}

/**
 * Stores individual room data.
 * Room
 */
class Room {
    // declare variables
    int[] options = new int[4];

    // constructor
    public Room(int opt1, int opt2, int opt3, int opt4){
        options[1] = opt1;
        options[2] = opt2;
        options[3] = opt3;
        options[4] = opt4;
    }

    // gets contents of room at i
    public int getRoom(int i){
        return options[i];
    }
}