package com.example.springboot.game_components;

import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

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
 *  For Inventory:
 *  - An Item Will Be Represented By A POSITIVE NUMBER (# > 0)
 *  - A Monster Will Be Represented By A NEGATIVE NUMBER (# < 0)
 * 
 * It is important that a difference is established since the two entities
 * are in seperate tables, and require different Repositories.
 * 
 * GameGrid
 */
@Component
@SessionScope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class GameGrid {
    // x and y coordinate values
    private int current_x;
    private int current_y;
    // set current floor to default
    private int current_floor = 0;
    // declare gameGrid struct
    private Room[][][] gameGrid = new Room[7][5][5];
    private final ItemRepository itemRepository;
    private final MonsterRepository monsterRepository;
    // Tracks whether the current session is showing an encounter or a victory choice.
    private boolean fighting;
    private boolean victoryPending;
    private boolean runCompleted;
    private boolean runWon;
    private boolean runSaved;
    private int completedScore;
    // The enemy action remains in its room so fleeing preserves its current state.
    private Action activeMonster;
    private int activeMonsterOption = -1;

    // Game variables
    private int steps = 0;
    private int score = 0;
    private int[] p_inv = {0, 0, 0, 0};
    
    // constructor
    @Autowired
    public GameGrid(ItemRepository itemRepository, MonsterRepository monsterRepository){
        this.itemRepository = Objects.requireNonNull(itemRepository);
        this.monsterRepository = Objects.requireNonNull(monsterRepository);
        // set starting x and y and current x and y to given x and y.
        current_x = 2;
        current_y = 2;

        // create preset floor layout
        defaultFloors();
    }

    // continue from previous run constructor
    public GameGrid( ItemRepository itemRepository, MonsterRepository monsterRepository, int i1, int i2, int i3, int i4 ){
        // run default constructor
        this(itemRepository, monsterRepository);

        // set p_item values to given integer values
        p_inv = new int[]{i1, i2, i3, i4};
    }

    /* Game Specific Functions */

    /**
     * Function lets the user select an option (1 to 4).
     * The options will connect to different room functions
     * depending on the contents of the current room. 
     */
    public String roomSelection(int option){
        if (runCompleted) {
            return "This run has already ended.";
        }
        String retval;
        if (gameGrid[current_floor][current_x][current_y] == null) {
            return "The current room is unavailable.";
        }
        // find the character for option of current room.
        char action = gameGrid[current_floor][current_x][current_y].getRoom(option);
        // if action is n, do nothing.
        if (action == 'n'){ return "You Cannot Go That Way."; }

        // Conditional Functions
        if (action == 'i'){ 
            // call item function
            retval = item(option);
        } else if (action == '1'){
            // call lock function with argument 1
            retval = unlock(option, 1);
        } else if (action == '2'){
            // call lock function with arguement 2
            retval = unlock(option, 2);
        } else if (action == 'o'){
            // Entering combat only opens the fight screen; it does not consume a step.
            retval = fight(option);
        } else if (action == 'e'){
            // Reaching the exit completes the run without spending another step.
            retval = exit(true);
        } else{ // Handle room actions that consume a step.
            steps++;
            // convert character into game output.
            if (action == 'p'){
                // move
                retval = move(option);
            } else if (action == 'd'){
                // descend
                retval = decend();
            } else if (action == 'u'){
                // ascend
                retval = ascend();
            } else{
                retval = "Warning: Something Unexpected Happened.";
            }
        }
        // check if maximum number of steps has been reached
        if (steps >= 99 && !runCompleted){
            exit(false);
        }
        return retval;
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

        gameGrid[0][1][0] = new Room(new Action('n'), new Action('o', 4, monsterRepository), new Action ('p'), new Action('n')); // quapok
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
        gameGrid[1][1][1] = new Room(new Action ('o', 5, monsterRepository), new Action ('p'), new Action ('p'), new Action ('n'));
        gameGrid[1][1][2] = new Room('n', 'p', 'p', 'p');
        gameGrid[1][1][3] = new Room('n', 'p', 'u', 'n'); // starting room

        gameGrid[1][2][0] = new Room('p', 'n', 'p', 'p');
        gameGrid[1][2][1] = new Room(new Action ('o', 4, monsterRepository), new Action ('p'), new Action ('n'), new Action ('i', 5, itemRepository));
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
        gameGrid[2][0][1] = new Room(new Action ('i', 1, itemRepository), new Action ('p'), new Action ('n'), new Action ('o', 6, monsterRepository));

        gameGrid[2][1][0] = new Room(new Action ('p'), new Action ('n'), new Action ('n'), new Action ('i', 4, itemRepository));

        // main path
        gameGrid[2][0][3] = new Room(new Action ('n'), new Action ('i', 3, itemRepository), new Action ('p'), new Action ('p'));
        gameGrid[2][0][4] = new Room(new Action ('i', 5, itemRepository), new Action ('p'), new Action ('n'), new Action ('p'));
        
        gameGrid[2][1][2] = new Room(new Action ('o', 1, monsterRepository), new Action ('n'), new Action ('n'), new Action ('p'));
        gameGrid[2][1][3] = new Room('p', 'n', 'p', 'p');
        gameGrid[2][1][4] = new Room(new Action ('p'), new Action ('p'), new Action ('n'), new Action ('i', 2, itemRepository));

        gameGrid[2][2][2] = new Room('p', 'n', 'p', 'p');
        gameGrid[2][2][3] = new Room('p', 'p', 'n', 'n');

        gameGrid[2][3][2] = new Room('p', 'd', 'u', '1'); // starting room & next floor

        gameGrid[2][4][2] = new Room(new Action ('p'), new Action ('n'), new Action ('o', 5, monsterRepository), new Action ('i', 5, itemRepository));
        
        /** 
         * Floor 4 
        */
        gameGrid[3][0][2] = new Room('d', 'n', 'n', 'p'); // next floor

        gameGrid[3][1][2] = new Room('2', 'n', 'n', 'p');

        gameGrid[3][2][2] = new Room('p', 'n', 'p', 'p');
        gameGrid[3][2][3] = new Room(new Action ('o', 5, monsterRepository), new Action ('p'), new Action ('i', 3, itemRepository), new Action ('i', 1, itemRepository));
        
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
        gameGrid[4][2][1] = new Room(new Action ('o', 3, monsterRepository), new Action ('i', 5, itemRepository), new Action ('i', 5, itemRepository), new Action ('d'));

        // room set Quapoks
        gameGrid[4][3][2] = new Room('n', 'n', 'd', 'p');

        gameGrid[4][4][2] = new Room('p', 'd', '2', 'n'); // enter
        gameGrid[4][4][3] = new Room(new Action ('o', 4, monsterRepository), new Action ('p'), new Action ('o', 4, monsterRepository), new Action ('o', 4, monsterRepository)); // quapok room

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

        gameGrid[5][1][0] = new Room(new Action ('n'), new Action ('o', 1, monsterRepository), new Action ('p'), new Action ('n'));
        gameGrid[5][1][1] = new Room('p', 'p', 'n', 'p');

        gameGrid[5][2][1] = new Room('p', 'n', 'n', 'u'); // exit

        // room set final
        gameGrid[5][0][2] = new Room(new Action ('i', 4, itemRepository), new Action('n'), new Action ('p'), new Action ('i', 1, itemRepository));
        gameGrid[5][0][3] = new Room('n', 'p', 'u', 'p');

        gameGrid[5][1][2] = new Room('n', 'n', 'u', '1');
        gameGrid[5][1][3] = new Room(new Action ('p'), new Action ('o', 2, monsterRepository), new Action ('p'), new Action ('n'));
        gameGrid[5][1][4] = new Room('u', 'p', 'n', 'p');

        gameGrid[5][2][2] = new Room('p', 'n', 'p', '2');
        gameGrid[5][2][3] = new Room('n', 'p', 'p', 'n');
        gameGrid[5][2][4] = new Room('p', 'p', 'n', 'n');

        gameGrid[5][3][2] = new Room('p', 'n', 'u', 'd');

        // room set detour
        gameGrid[5][4][2] = new Room('n', 'u', 'p', 'n');
        gameGrid[5][4][3] = new Room('n', 'p', 'p', 'n');
        gameGrid[5][4][4] = new Room('n', 'p', 'u', 'n');

        /**
         * Floor 7 
         * 
         * Compose of only two rooms. 
         * 1) The starting room has stairs for ascending, and a locked 1 door.
         * 2) The second room only has a passage way back to the previous room, and the final boss.
        */
        gameGrid[6][3][2] = new Room('p', 'n', 'n', 'u'); // starting room
        gameGrid[6][2][2] = new Room(new Action('o', 7, monsterRepository), new Action ('n'), new Action('n'), new Action('p'));
    }

    // Reinitializes all per-run state and rebuilds the default dungeon rooms.
    public void reset(){
        current_x = 2;
        current_y = 2;
        current_floor = 0;
        gameGrid = new Room[7][5][5];
        steps = 0;
        score = 0;
        p_inv = new int[]{0, 0, 0, 0};
        fighting = false;
        victoryPending = false;
        runCompleted = false;
        runWon = false;
        runSaved = false;
        completedScore = 0;
        activeMonster = null;
        activeMonsterOption = -1;
        defaultFloors();
    }

    /* --- Private Action Functions --- */

    // generic return statement
    public String generic(){ return "Coordinates: (" + (current_x + 1) + ", " + (current_y + 1) + ")\nFloor: " + (current_floor + 1);}

    /** 
     * Function called in the roomSelection method.
     * Calls other move functions based on i.
     */ 
    private String move(int i){
        if (i == 0){
            move_up();
        } else if (i == 1){
            move_left();
        } else if (i == 2){
            move_right();
        } else if (i == 3){
            move_down();
        }
        return generic();
    }

    // move up
    private void move_up(){
        if (current_x > 0 && gameGrid[current_floor][current_x - 1][current_y] != null){
            current_x--;
        }
    }

    // move down
    private void move_down(){
        if (current_x < 4 && gameGrid[current_floor][current_x + 1][current_y] != null){
            current_x++;
        }
    }

    // move right
    private void move_right(){
        if (current_y < 4 && gameGrid[current_floor][current_x][current_y + 1] != null){
            current_y++;
        }
    }

    // move left
    private void move_left(){
        if (current_y > 0 && gameGrid[current_floor][current_x][current_y - 1] != null){
            current_y--;
        }
    }

    // ascend
    private String ascend(){
        // decrement current_floor
        current_floor--;
        return generic();
    }

    // descend
    private String decend(){
        // increment current_floor
        current_floor++;
        return generic();
    }

    // locked door function
    private String unlock(int i, int l){
        // check if player has key for i
        for(int j = 0; j < 4; j++){
            // if they do, set action to 'p'
            if ( (l == 1 && p_inv[j] == 4) || (l == 2 && p_inv[j] == 5) ){ 
                // rewrite lock to passageway
                gameGrid[current_floor][current_x][current_y].rewriteAction(i, 'p'); 
                // toss the key
                tossInventory(j);
                // increment steps
                steps ++;
                // send success message
                return "The Door Was Unlocked!";
            }
        }
        return "The Door Is Locked.";
    }

    // exit function. True if success, False if ran out of steps.
    private String exit(boolean successfulExit){
        int total = score + 99 - steps;
        runWon = hasCollectedKorokorobi();
        if (!successfulExit){ total -= 10000; }
        runCompleted = true;
        completedScore = total;
        return "" + total;
    }

    // Checks to see if the player has Korokorobi in their inventory at the end of their run.
    private boolean hasCollectedKorokorobi(){
        for (int inventoryId : p_inv) {
            if (inventoryId == -7) {
                return true;
            }
        }
        return false;
    }

    // item function. Adds item to inventory, increments score, and increases step count on succes.
    private String item(int i){
        // check to see if inventory is full
        for(int j = 0; j < 4; j++){
            if (p_inv[j] == 0){
                int id = gameGrid[current_floor][current_x][current_y].getID(i);
                // set open p_inv value to
                p_inv[j] = id;
                // set action to 'n'
                gameGrid[current_floor][current_x][current_y].rewriteAction(i, 'n');
                // increment step count
                steps++;
                // increase score
                score += itemRepository.findScoreById(id);
                // return message to player regarding success
                return "Obtained " + itemRepository.findNameById(id) + "!";
            }
        }
        // return message to player stating inventory is full
        return "Your Inventory Is Full.";
    }

    /* --- Fight Action Functions --- */

    // default output string for fights
    String f_d = "Nothing Happened.";

    // enemy function
    private String fight(int i){
        // Keep a reference to the room's monster action so its state survives fleeing.
        activeMonster = gameGrid[current_floor][current_x][current_y].getAction(i);
        activeMonsterOption = i;
        fighting = true;
        victoryPending = activeMonster.getMonsterState() == '$';
        // A previously defeated monster returns directly to the collect-or-run choice.
        return victoryPending
            ? activeMonster.getVictoryDescription()
            : activeMonster.getFightDescription();
    }

    // Tells the controller whether to render the combat screen.
    public boolean isFighting(){
        return fighting;
    }

    public boolean isRunComplete(){
        return runCompleted;
    }

    public boolean isRunWon(){
        return runWon;
    }

    public boolean isRunSaved(){
        return runSaved;
    }

    public void markRunSaved(){
        if (!runCompleted) {
            throw new IllegalStateException("A run must be complete before it can be saved.");
        }
        runSaved = true;
    }

    public int getCompletedScore(){
        if (!runCompleted) {
            throw new IllegalStateException("The current run has not ended.");
        }
        return completedScore;
    }

    public int[] getInventoryIds(){
        return p_inv.clone();
    }

    public int getCompletedStepsRemaining(){
        return 99 - steps;
    }

    // Tells the template whether to show the victory actions instead of combat items.
    public boolean isVictoryPending(){
        return victoryPending;
    }

    // Lets the template disable collection when all inventory slots are occupied.
    public boolean hasFreeInventorySlot(){
        return findEmptyInventorySlot(-1) >= 0;
    }

    // Returns the board image for the monster in the active encounter.
    public String getFightMonsterIcon(){
        return fighting ? activeMonster.getMonsterIcon() : "";
    }

    // Returns the player character symbol for the bottom of the combat board.
    public String getFightPlayerIcon(){
        return "ก";
    }

    // Shows which item or previously collected monster is in a combat choice slot.
    public String getFightOption(int slot){
        validateInventorySlot(slot);
        int inventoryId = p_inv[slot];
        if (inventoryId == 0) {
            return "Empty";
        }
        if (inventoryId > 0) {
            return itemRepository.findNameById(inventoryId)
                + " [ " + itemRepository.findIconById(inventoryId) + " ]";
        }
        int monsterId = -inventoryId;
        return monsterRepository.findNameById(monsterId)
            + " [ " + firstLine(monsterRepository.findIconById(monsterId)) + " ]";
    }

    // Applies the database interaction for the selected inventory slot.
    public String fightSelection(int slot){
        if (!fighting || victoryPending || activeMonster == null) {
            return f_d;
        }
        validateInventorySlot(slot);
        int inventoryId = p_inv[slot];
        if (inventoryId == 0) {
            // Empty inventory choices use the same response as a missing interaction rule.
            return f_d;
        }

        Optional<MonsterRepository.Interaction> interaction = inventoryId > 0
            ? monsterRepository.findItemInteraction(
                activeMonster.getId(),
                inventoryId,
                activeMonster.getMonsterState()
            )
            : monsterRepository.findMonsterInteraction(
                activeMonster.getId(),
                -inventoryId,
                activeMonster.getMonsterState()
            );
        if (interaction.isEmpty()) {
            return f_d;
        }

        // The interaction's lose flag consumes the selected item or companion.
        MonsterRepository.Interaction outcome = interaction.get();
        if (outcome.lose()) {
            p_inv[slot] = 0;
        }
        // A non-null state returned by the table becomes the enemy's next state.
        if (outcome.newState() != null) {
            activeMonster.setMonsterState(outcome.newState());
        }
        steps++;
        if (steps >= 99) {
            leaveFight();
            exit(false);
            return outcome.result() == null ? f_d : outcome.result();
        }

        if (outcome.newState() != null && outcome.newState() == '$') {
            // Victory waits for the player to choose Run or Collect.
            victoryPending = true;
        } else if (outcome.newState() != null && outcome.newState() == 'x') {
            // The table says the enemy escaped or was defeated, so remove it from this room.
            gameGrid[current_floor][current_x][current_y]
                .rewriteAction(activeMonsterOption, 'n');
            leaveFight();
        }
        return outcome.result() == null ? f_d : outcome.result();
    }

    // Leaves the encounter without changing the monster's room action or state.
    public String runFromFight(){
        if (!fighting || activeMonster == null) {
            return f_d;
        }
        leaveFight();
        return "You ran away!";
    }

    // Adds a defeated monster to an empty slot and removes it from the room.
    public String collectDefeatedMonster(){
        if (!fighting || !victoryPending || activeMonster == null) {
            return f_d;
        }
        int captureSlot = findEmptyInventorySlot(-1);
        if (captureSlot < 0) {
            return "(Your inventory is full. Toss an item before collecting this monster.)";
        }
        int monsterId = activeMonster.getId();
        p_inv[captureSlot] = -monsterId;
        score += monsterRepository.findScoreById(monsterId);
        gameGrid[current_floor][current_x][current_y]
            .rewriteAction(activeMonsterOption, 'n');
        leaveFight();
        return "Collected " + monsterRepository.findNameById(monsterId) + "!";
    }

    // Clears only session combat pointers; the room action itself is handled by each choice.
    private void leaveFight(){
        fighting = false;
        victoryPending = false;
        activeMonster = null;
        activeMonsterOption = -1;
    }

    // Finds an empty inventory slot, optionally preferring a slot freed by an interaction.
    private int findEmptyInventorySlot(int preferredSlot){
        if (preferredSlot >= 0 && p_inv[preferredSlot] == 0) {
            return preferredSlot;
        }
        for (int i = 0; i < p_inv.length; i++) {
            if (p_inv[i] == 0) {
                return i;
            }
        }
        return -1;
    }

    // Rejects indexes outside the four supported inventory positions.
    private void validateInventorySlot(int slot){
        if (slot < 0 || slot >= p_inv.length) {
            throw new IllegalArgumentException("Inventory slot must be between 0 and 3.");
        }
    }

    /* --- Get and To String Functions --- */

    // returns number of steps
    public int getSteps(){
        return 99 - steps;
    }

    // Returns the current floor using the one-based numbering shown on the map.
    public int getCurrentFloor(){
        return current_floor + 1;
    }

    // get data from option at current position
    public String getCurrentData(int option){
        // returns content of current.
        if (gameGrid[current_floor][current_x][current_y] == null) {
            return "?";
        }
        return gameGrid[current_floor][current_x][current_y].getImage(option);
    }

    // get the message associated with a specific action at a given option
    public String getGameMessage(int option){
        // returns content of current.
        if (gameGrid[current_floor][current_x][current_y] == null) {
            return "Unavailable";
        }
        return gameGrid[current_floor][current_x][current_y].getMessage(option);
    }

    // get data from a given x and y value
    public String getData(int x, int y, int option){
        // check if grid space has data
        if (gameGrid[current_floor][x][y] == null){ return ""; }
        // retunrs content of current.
        return gameGrid[current_floor][x][y].getImage(option);
    }

    // gets icons for items in player invetory at a given index
    public String getInventory(int i){
        validateInventorySlot(i);
        int temp = p_inv[i];
        if (temp == 0) {
            return "XXX";
        }
        if (temp > 0) {
            return itemRepository.findIconById(temp);
        }
        return firstLine(monsterRepository.findIconById(-temp));
    }

    // Keep multi-line monster art from overflowing its single-row inventory slot.
    private String firstLine(String icon){
        int newlineIndex = icon.indexOf('\n');
        return newlineIndex < 0 ? icon : icon.substring(0, newlineIndex);
    }

    // removes an item from the players inventory at a given index
    public void tossInventory(int i){
        validateInventorySlot(i);
        p_inv[i] = 0;
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
        retval += gameGrid[floor][x][y].getImageMap(0) + "\n";
        retval += gameGrid[floor][x][y].getImageMap(1);
        // check if current tile has player
        if (floor == current_floor && x == current_x && y == current_y){
            // add player to tile
            retval += "\tก\t";
        } else {
            // display generic floor data
            retval += "\t\t";
        }
        // get last two values
        retval += gameGrid[floor][x][y].getImageMap(2) + "\n";
        retval += gameGrid[floor][x][y].getImageMap(3);

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

    // rewrites the char value of a given action. (Used by lock, item, and enemy)
    public void rewriteAction(int i, char newAction){
        options[i].setAction(newAction);
    }

    // gets contents of room at i
    public char getRoom(int i){
        return options[i].getAction();
    }

    // returns the image for a given Action. Item image shows icon accurately
    public String getImage(int i){
        return options[i].getIcon(true);
    }

    // returns the image for a given Action while hiding item icons on the map.
    public String getImageMap(int i){
        return options[i].getIcon(false);
    }

    // returns the message for a given Action
    public String getMessage(int i){
        return options[i].getMessage();
    }

    public int getID(int i){
        return options[i].getId();
    }

    public Action getAction(int i){
        return options[i];
    }
}

/**
 * Defines individual actions to be stored within the Gamegrid. Also returns characters defined in guide.
 * Action
 */
class Action {
    private char action;
    private int act_id = 0;
    // item files
    private final ItemRepository itemRepository;
    // monster files
    private final MonsterRepository monsRepository;
    private char monster_state;
    private String desc;

    // generic action constructor
    public Action(char type){
        action = type;
        itemRepository = null;
        monsRepository = null;
    }

    // monster/item constructor
    public Action(char type, int id){
        act_id = id;
        action = type;
        itemRepository = null;
        monsRepository = null;
    }

    public Action(char type, int id, ItemRepository itemRepository){
        act_id = id;
        action = type;
        this.itemRepository = itemRepository;
        monsRepository = null;
    }

    public Action(char type, int id, MonsterRepository monRepository){
        act_id = id;
        action = type;
        this.monsRepository = monRepository;
        monster_state = 'a';
        desc = null;
        itemRepository = null;
    }

    public String getFightDescription(){
        desc = "You Stand Before A Wild " + monsRepository.findNameById(act_id) + "!";
        return desc;
    }

    public String getMonsterIcon(){
        if (monsRepository == null) {
            throw new IllegalStateException("Monster actions require a MonsterRepository.");
        }
        return monsRepository.findIconById(act_id);
    }

    public char getMonsterState(){
        return monster_state;
    }

    public void setMonsterState(char state){
        monster_state = state;
    }

    public String getVictoryDescription(){
        return "You defeated " + monsRepository.findNameById(act_id) + "!";
    }

    // returns the icon of a given Action
    public String getIcon(boolean showItem){
        // if item, get item image from database
        if (action == 'i' && showItem){
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
        return "⍰";
    }

    // returns the icon of a given Action
    public String getMessage(){
        // if item, get item image from database
        if (action == 'i'){
            if (itemRepository == null) {
                throw new IllegalStateException("Item actions require an ItemRepository.");
            }
            return itemRepository.findIconById(act_id);
        }
        // wall
        if (action == 'n'){return "X";}
        // exit
        if (action == 'e'){ return "EXIT"; }
        // passage way
        if (action == 'p'){ return "MOVE"; }
        // stairs down
        if (action == 'd'){ return "DESCEND"; }
        // stairs up
        if (action == 'u'){ return "ASCEND"; }
        // enemy
        if (action == 'o'){ return "FIGHT"; }
        // locked doors
        if (action == '1' || action == '2'){ return "UNLOCK"; }
        // unimplemented character
        return "⍰";
    }

    // return char representing action type
    public char getAction(){
        return action;
    }

    // sets a given action type. (Used by lock, item, and enemy)
    public void setAction(char newAction){
        action = newAction;
    }

    // return id of action
    public int getId(){
        return act_id;
    }
}