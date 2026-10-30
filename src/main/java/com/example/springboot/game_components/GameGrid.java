package com.example.springboot.game_components;

import java.util.Objects;

/**
 * Stores data for game grid. For gameGrid, the first value represents the floor,
 * The second value represent the row, and the third value represents the column.
 * Notably, (0,0) is the top left of the board, while (4,4) is the bottom right.
 * 
 * For options:
 *  - 0 = top
 *  - 1 = left
 *  - 2 = right
 *  - 3 = bottom
 * 
 * GameGrid
 */
public class GameGrid {
    // x and y coordinate values
    private int current_x;
    private int current_y;
    // set current floor to default
    private int current_floor = 0;
    // declare gameGrid struct
    private Room[][][] gameGrid = new Room[7][5][5];
    private final ItemRepository itemRepository;

    // Game variables
    int steps = 0;
    
    // constructor
    public GameGrid(ItemRepository itemRepository){
        this.itemRepository = Objects.requireNonNull(itemRepository);
        // set starting x and y and current x and y to given x and y.
        current_x = 2;
        current_y = 2;

        // create preset floor layout
        defaultFloors();
    }

    /**
     * Function lets the user select an option (1 to 4).
     * The options will connect to different room functions
     * depending on the contents of the current room. 
     */
    public void roomSelection(int option){
        // find the character for option of current room.
        char action = gameGrid[current_floor][current_x][current_y].getRoom(option - 1);
        // if action is n, do nothing.
        if (action == 'n'){ return; }
        // increment steps
        steps ++;
        // convert character into game output.
        if (action == 'p'){
            // move
            move(option);
        } else if (action == 'd'){
            // descend
            decend();
        } else if (action == 'u'){
            // ascend
            ascend();
        } else if (action == 'e'){
            // call exit functions
            exit(true);
        } else if (action == 'i'){
            // call item function
            item();
        } else if (action == '1'){
            // call lock function with argument 1
            lock(1);
        } else if (action == '2'){
            // call lock function with arguement 2
            lock(2);
        } else if (action == 'o'){
            // call enemy functions and change game state to battle screen
        }
        // check if maximum number of steps has been reached
        if (steps == 99){
            exit(false);
        }
    }

    /**
     * Function creates the default floor layout. Used prior to random functionality
     */
    private void defaultFloors(){
        /** 
         * Floor 1         
        */
        gameGrid[0][0][1] = new Room('n', 'n', 'p', 'p');
        gameGrid[0][0][2] = new Room('n', 'p', 'n', 'p');

        gameGrid[0][1][0] = new Room(new Action('n'), new Action('o', 4), new Action ('p'), new Action('n')); // quapok
        gameGrid[0][1][1] = new Room('p', 'p', 'n', 'p');
        gameGrid[0][1][2] = new Room('p', 'n', 'p', 'p');
        gameGrid[0][1][3] = new Room('n', 'p', 'd', 'n'); // next floor

        gameGrid[0][2][1] = new Room('p', 'n', 'p', 'n');
        gameGrid[0][2][2] = new Room('1', 'p', 'p', 'e'); // starting room
        gameGrid[0][2][3] = new Room('n', 'p', 'n', 'p');
        
        gameGrid[0][3][3] = new Room('p', 'n', 'p', '1');
        gameGrid[0][3][4] = new Room(new Action('n'), new Action ('p'), new Action('n'), new Action('i', 4, itemRepository));

        gameGrid[0][4][3] = new Room(new Action ('p'), new Action('i', 1, itemRepository), new Action('i', 2, itemRepository), new Action('i', 3, itemRepository)); // temptation room

        /** 
         * Floor 2 
         */
        gameGrid[1][0][0] = new Room('n', 'd', 'n', 'p'); // next floor: dead end

        gameGrid[1][1][0] = new Room('p', 'n', 'p', 'p');
        gameGrid[1][1][1] = new Room(new Action ('o', 5), new Action ('p'), new Action ('p'), new Action ('n'));
        gameGrid[1][1][2] = new Room('n', 'p', 'p', 'p');
        gameGrid[1][1][3] = new Room('n', 'p', 'u', 'n'); // starting room

        gameGrid[1][2][0] = new Room('p', 'n', 'p', 'p');
        gameGrid[1][2][1] = new Room(new Action ('o', 4), new Action ('p'), new Action ('n'), new Action ('i', 5, itemRepository));
        gameGrid[1][2][2] = new Room('p', 'n', 'n', 'p');

        gameGrid[1][3][0] = new Room('p', 'n', 'p', 'n');
        gameGrid[1][3][1] = new Room('n', 'p', 'p', 'n');
        gameGrid[1][3][2] = new Room('p', 'p', 'd', 'p'); // next floor

        gameGrid[1][4][2] = new Room(new Action ('p'), new Action ('n'), new Action ('n'), new Action ('i', 2, itemRepository));

        /** 
         * Floor 3 
        */
        // side path
        gameGrid[2][0][0] = new Room('n', 'u', 'p', 'p'); // starting room: dead end
        gameGrid[2][0][1] = new Room(new Action ('i', 1, itemRepository), new Action ('p'), new Action ('n'), new Action ('o', 6));

        gameGrid[2][1][0] = new Room(new Action ('p'), new Action ('n'), new Action ('n'), new Action ('i', 4, itemRepository));

        // main path
        gameGrid[2][0][3] = new Room(new Action ('n'), new Action ('i', 3, itemRepository), new Action ('p'), new Action ('p'));
        gameGrid[2][0][4] = new Room(new Action ('i', 5, itemRepository), new Action ('p'), new Action ('n'), new Action ('p'));
        
        gameGrid[2][1][2] = new Room(new Action ('o', 1), new Action ('n'), new Action ('n'), new Action ('p'));
        gameGrid[2][1][3] = new Room('n', 'p', 'p', 'p');
        gameGrid[2][1][4] = new Room(new Action ('p'), new Action ('p'), new Action ('n'), new Action ('i', 2, itemRepository));

        gameGrid[2][2][2] = new Room('p', 'n', 'p', 'p');
        gameGrid[2][2][3] = new Room('p', 'p', 'n', 'n');

        gameGrid[2][3][2] = new Room('p', 'd', 'u', '1'); // starting room & next floor

        gameGrid[2][4][2] = new Room(new Action ('p'), new Action ('n'), new Action ('o', 5), new Action ('i', 5, itemRepository));
        
        /** 
         * Floor 4 
        */
        gameGrid[3][0][2] = new Room('d', 'n', 'n', 'p'); // next floor

        gameGrid[3][1][2] = new Room('2', 'n', 'n', 'p');

        gameGrid[3][2][2] = new Room('p', 'n', 'p', 'p');
        gameGrid[3][2][3] = new Room(new Action ('o', 5), new Action ('p'), new Action ('i', 3, itemRepository), new Action ('i', 1, itemRepository));
        
        gameGrid[3][3][2] = new Room('1', 'u', 'n', 'p'); // starting room

        gameGrid[3][4][1] = new Room('n', 'e', 'p', 'n'); // Late Game Exit
        gameGrid[3][4][2] = new Room('p', '2', 'n', 'n');

        /** 
         * Floor 5 
        */
        // room set entrance
        gameGrid[4][0][1] = new Room('n', 'd', 'p', 'n');
        gameGrid[4][0][2] = new Room('u', 'p', 'p', 'p'); // starting room
        gameGrid[4][0][3] = new Room('n', 'p', 'd', 'n');

        gameGrid[4][1][2] = new Room('p', 'n', 'd', 'n');

        // room set Lappup
        gameGrid[4][2][1] = new Room(new Action ('o', 3), new Action ('i', 5, itemRepository), new Action ('i', 5, itemRepository), new Action ('d'));

        // room set Quapoks
        gameGrid[4][3][2] = new Room('n', 'n', 'd', 'p');

        gameGrid[4][4][2] = new Room('p', 'd', '2', 'n'); // enter
        gameGrid[4][4][3] = new Room(new Action ('o', 4), new Action ('p'), new Action ('o', 4), new Action ('o', 4)); // quapok room

        // room set long
        gameGrid[4][1][4] = new Room('d', 'n', 'n', 'p'); // enter
        gameGrid[4][2][4] = new Room('p', 'n', 'n', 'p');
        gameGrid[4][3][4] = new Room('p', 'n', 'n', 'p');
        gameGrid[4][4][4] = new Room('p', 'n', 'd', 'n'); // exit
        
        /**
         * Floor 6 
        */
        // room set molynx
        gameGrid[5][0][1] = new Room('n', 'u', 'n', 'p'); // enter

        gameGrid[5][1][0] = new Room(new Action ('n'), new Action ('o', 1), new Action ('p'), new Action ('n'));
        gameGrid[5][1][1] = new Room('p', 'p', 'n', 'p');

        gameGrid[5][2][1] = new Room('p', 'n', 'n', 'u'); // exit

        // room set final
        gameGrid[5][0][2] = new Room(new Action ('i', 4, itemRepository), new Action('n'), new Action ('p'), new Action ('i', 1, itemRepository));
        gameGrid[5][0][3] = new Room('n', 'p', 'u', 'p');

        gameGrid[5][1][2] = new Room('n', 'n', 'u', '1');
        gameGrid[5][1][3] = new Room(new Action ('p'), new Action ('o', 2), new Action ('p'), new Action ('n'));
        gameGrid[5][1][4] = new Room('u', 'p', 'n', 'p');

        gameGrid[5][2][2] = new Room('p', 'n', 'p', '2');
        gameGrid[5][2][3] = new Room('n', 'p', 'p', 'n');
        gameGrid[5][2][4] = new Room('p', 'p', 'n', 'n');

        gameGrid[5][3][2] = new Room('p', 'n', 'u', 'd');

        // room set detour
        gameGrid[5][4][2] = new Room('n', 'u', 'p', 'n');
        gameGrid[5][4][2] = new Room('n', 'p', 'p', 'n');
        gameGrid[5][4][2] = new Room('n', 'p', 'u', 'n');

        /**
         * Floor 7 
         * 
         * Compose of only two rooms. 
         * 1) The starting room has stairs for ascending, and a locked 1 door.
         * 2) The second room only has a passage way back to the previous room, and the final boss.
        */
        gameGrid[6][3][2] = new Room('p', 'n', 'n', 'u'); // starting room
        gameGrid[6][2][2] = new Room(new Action('o', 7), new Action ('n'), new Action('n'), new Action('p'));
    }

    /** 
     * Function called in the roomSelection method.
     * Calls other move functions based on i.
     */ 
    private void move(int i){
        if (i == 1){
            move_up();
        } else if (i == 2){
            move_left();
        } else if (i == 3){
            move_right();
        } else if (i == 4){
            move_down();
        }
    }

    // move up
    private void move_up(){
        if(4 >= current_y){ return; }
        // increment current y
        current_y++;
    }

    // move down
    private void move_down(){
        if(0 <= current_y){ return; }
        // increment current y
        current_y--;
    }

    // move right
    private void move_right(){
        if(4 >= current_x){ return; }
        // increment current y
        current_x++;
    }

    // move left
    private void move_left(){
        if(0 <= current_x){ return; }
        // increment current y
        current_x--;
    }

    // ascend
    private void ascend(){
        // decrement current_floor
        current_floor--;
    }

    // descend
    private void decend(){
        // increment current_floor
        current_floor++;
    }

    // locked door function
    private void lock(int i){
        // check if player has key for i
            // if they do, set action to 'p'
    }

    // exit function. True if success, False if ran out of steps.
    private void exit(boolean success){
        // TODO
        return;
    }

    // item function
    private void item(){
        // TODO
        // set action to 'n'
    }

    // returns number of steps
    public int getSteps(){
        return steps;
    }

    // get data from current x and y value
    public int getCurrentData(int option){
        // retunrs content of current.
        return gameGrid[current_floor][current_x][current_y].getRoom(option);
    }

    // get data from a given x and y value
    public String getData(int x, int y, int option){
        // check if grid space has data
        if (gameGrid[current_floor][x][y] == null){ return ""; }
        // retunrs content of current.
        return gameGrid[current_floor][x][y].getImage(option);
    }

    // return string format for a tile in GameGrid
    public String tileToString(int floor, int x, int y){
        // check to see if given data is valid
        if (floor < 0 || floor >= gameGrid.length || x < 0 || x >= gameGrid[floor].length || y < 0 || y >= gameGrid[floor][x].length) {
            throw new IndexOutOfBoundsException("Map coordinates are outside the game grid.");
        }
        // check if grid space has data
        if (gameGrid[floor][x][y] == null){ return "?"; }
        // fill retval string with values in format for map page
        String retval = "";
        // get first two values
        retval += gameGrid[floor][x][y].getImage(0) + "\n";
        retval += gameGrid[floor][x][y].getImage(1);
        // check if current tile has player
        if (floor == current_floor && x == current_x && y == current_y){
            // add player to tile
            retval += "\tก\t";
        } else {
            // display generic floor data
            retval += "\t\t";
        }
        // get last two values
        retval += gameGrid[floor][x][y].getImage(2) + "\n";
        retval += gameGrid[floor][x][y].getImage(3);

        return retval;
    }

}

/**
 * Stores individual room data.
 *  - n = null
 *  - e = exit
 *  - p = passage
 *  - d = stairs down
 *  - u = stairs up
 *  - o = enemy (opponent/omega)
 *  - 1 = lock 1
 *  - 2 = lock 2
 * Room
 */
class Room {
    // declare variables
    private Action[] options = new Action[4];

    // default constructor
    public Room(char opt1, char opt2, char opt3, char opt4){
        options[0] = new Action(opt1);
        options[1] = new Action(opt2);
        options[2] = new Action(opt3);
        options[3] = new Action(opt4);
    }

    // item/monster constructor
    public Room(Action act1, Action act2, Action act3, Action act4){
        options[0] = act1;
        options[1] = act2;
        options[2] = act3;
        options[3] = act4;
    }

    // gets contents of room at i
    public char getRoom(int i){
        return options[i].getAction();
    }

    // returns the image for a given Action
    public String getImage(int i){
        return options[i].getIcon();
    }
}

/**
 * Defines individual actions to be stored within the Gamegrid. Also returns characters defined in guide.
 * Action
 */
class Action {
    private int act_id = 0;
    private char action;
    private final ItemRepository itemRepository;

    // generic action constructor
    public Action(char type){
        action = type;
        itemRepository = null;
    }

    // monster/item constructor
    public Action(char type, int id){
        act_id = id;
        action = type;
        itemRepository = null;
    }

    public Action(char type, int id, ItemRepository itemRepository){
        act_id = id;
        action = type;
        this.itemRepository = itemRepository;
    }

    // returns the icon of a given Action
    public String getIcon(){
        // if item, get item image from database
        if (action == 'i'){
            if (itemRepository == null) {
                throw new IllegalStateException("Item actions require an ItemRepository.");
            }
            return itemRepository.findIconById(act_id);
        }
        // wall
        if (action == 'n'){return "  ";}
        // exit
        if (action == 'e'){ return "𒁱"; }
        // passage way
        if (action == 'p'){ return "Ⅱ"; }
        // stairs down
        if (action == 'd'){ return "↙▟"; }
        // stairs up
        if (action == 'u'){ return "↗▟"; }
        // enemy
        if (action == 'o'){ return "Ω"; }
        // locked door 1
        if (action == '1'){ return "⌧"; }
        // locked door 2
        if (action == '2'){ return "⮹"; }
        // unimplemented character
        return "𐙚";
    }

    // return char representing action type
    public char getAction(){
        return action;
    }

    // sets a given action type. (Used by lock and item)
    public void setAction(char newAction){
        action = newAction;
    }

    // return id of action
    public int getId(){
        return act_id;
    }
}