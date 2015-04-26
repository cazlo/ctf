package ctf.agent;

import ctf.common.AgentAction;
import ctf.common.AgentEnvironment;

import java.lang.Override;
import java.util.*;

/**
 * Created by Andrew Paettie on 3/25/15.
 */
public class Agent007 extends Agent {

    public static enum NODE_TYPE{
        UNKOWN,
        OBSTACLE,
        ENEMY_BASE,
        FRIENDLY_BASE,
        ENTERABLE_SPACE,
        DEAD_END,
        KNOWN_MINE,
        PLAYER_LOCATION,
        ENEMY_LOCATION
    }

    public static enum DIRECTION{
        NORTH, SOUTH, EAST, WEST, NOCHANGE
    }

    public static enum ATTACKER_TARGET{
        ENEMY_FLAG, OUR_FLAG
    }

    public static enum ATTACK_MODE{
        SEEK_FLAG, CAPTURE_FLAG, DEFEND_FLAG
    }

    public static final int BOARD_SIZE = 10;
    public static final int FLAG_ROW = (BOARD_SIZE / 2);
    public static final int ENEMY_TIMEOUT = 5;

//    public static final int AGENT_0 = 0;
//    public static final int AGENT_1 = 1;
//    public static int agentCount = 0;
//    public int agentIdentifier;

    public static boolean initialMove = true;
    public static DIRECTION friendlyBaseWestOrEast = null;

    public DIRECTION friendlyInitialFlagDirection = null;

    public ArrayList<Node> path;

    public Node currentLocation;
    //public Node previousLocation;
    public Node initialLocation;
    private DIRECTION moveDirection = DIRECTION.NOCHANGE; //the move direction of the latest move made (used to update currentLocation)

    public static Node[][] nodes = new Node[BOARD_SIZE][BOARD_SIZE];
    public static ArrayList<Node> pendingMoves = new ArrayList<Node>();

    public Agent007(){
        path = new ArrayList<Node>();

        for (int row = 0; row < BOARD_SIZE; row++){
            for (int col = 0; col < BOARD_SIZE; col++){
                Node n = new Node();
                n.row = row;
                n.col = col;
                nodes[row][col] = n;
            }
        }

        initialMove = true;
    }

    @Override
    public int getMove( AgentEnvironment inEnvironment ){
        if (friendlyInitialFlagDirection == null){//do initial setup for this agent (non-static stuff)
            friendlyBaseWestOrEast = inEnvironment.isBaseEast(AgentEnvironment.ENEMY_TEAM, false) ? DIRECTION.WEST : DIRECTION.EAST;
            friendlyInitialFlagDirection = inEnvironment.isFlagSouth(AgentEnvironment.OUR_TEAM, false) ? DIRECTION.SOUTH : DIRECTION.NORTH;
            this.setupInitialLocation(inEnvironment);
            currentLocation = initialLocation;
        }
        if (initialMove){
            doInitialSetup(inEnvironment);//do initial setup for both agents (static stuff)
            initialMove = false;
        }

        if (pendingMoves.size() == 2){
            removePlayersFromAppropriateNodes(pendingMoves);
            pendingMoves = new ArrayList<Node>();
        }

        //check/update our location
        updateLocation(inEnvironment);
        //add node to path
        updatePath();
        //gather info about our surroundings
        updateSurroundingNodes(inEnvironment);
        //todo check for blocking another player
        if (blockingOurOtherAgent(inEnvironment)){
            //todo stop blocking another player somehow?
            System.out.println("GET OUT THE WAY!");
        }
        //check for dead end
        handleDeadEnd(inEnvironment);
        boolean willMove = false;
        //todo check if tagging conditions are met
        if (!(moveDirection = tagEnemy(inEnvironment)).equals(DIRECTION.NOCHANGE)){
            willMove = true;
        }else {
            //check if we are in attack mode or capture mode
            ATTACK_MODE attackMode = attackOrCapture(inEnvironment);
            //find best path to goal if not going to tag enemy(based on attack mode or capture mode)
            moveDirection = findBestPathToGoal(attackMode, inEnvironment);
            Node newNode = currentLocation;
            switch (moveDirection){

                case NORTH:
                    newNode = currentLocation.north;
                    break;
                case SOUTH:
                    newNode = currentLocation.south;

                    break;
                case EAST:
                    newNode = currentLocation.east;

                    break;
                case WEST:
                    newNode = currentLocation.west;

                    break;
            }
//            if (attackMode.equals(ATTACK_MODE.CAPTURE_FLAG) && conflictWithPendingMove(newNode, attackMode) && ! newNode.nodeTypes.contains(NODE_TYPE.FRIENDLY_BASE))
//                moveDirection = DIRECTION.NOCHANGE;
//            else if (attackMode.equals(ATTACK_MODE.CAPTURE_FLAG) && newNode.nodeTypes.contains(NODE_TYPE.FRIENDLY_BASE)){
//                return directionToAction(moveDirection);
//            }
            //moveDirection = findBestNonconflictingMove(inEnvironment, moveDirection);
            willMove = !moveDirection.equals(DIRECTION.NOCHANGE);
        }
        // use all the info above to make a move
        // before returning indicate in 'moveDirection' what direction we are moving in

        ATTACK_MODE attackMode = attackOrCapture(inEnvironment);
        //execute the move
        switch (moveDirection) {
            case NORTH:
                if (! conflictWithPendingMove( currentLocation.north, attackMode)){
                    pendingMoves.add(currentLocation.north);
                    currentLocation.north.addPlayerToNode();
                    return AgentAction.MOVE_NORTH;
                }else{
                    return findBetterMove( currentLocation.north, attackMode);
                }
            case SOUTH:
                if (! conflictWithPendingMove( currentLocation.south, attackMode)){
                    pendingMoves.add(currentLocation.south);
                    currentLocation.south.addPlayerToNode();
                    return AgentAction.MOVE_SOUTH;
                }else{
                    return findBetterMove( currentLocation.south, attackMode);
                }
            case EAST:
                if (! conflictWithPendingMove( currentLocation.east, attackMode)){
                    pendingMoves.add(currentLocation.east);
                    currentLocation.east.addPlayerToNode();
                    return AgentAction.MOVE_EAST;
                }else{
                    return findBetterMove( currentLocation.east, attackMode);
                }
            case WEST:
                if (! conflictWithPendingMove( currentLocation.west, attackMode)){
                    pendingMoves.add(currentLocation.west);
                    currentLocation.west.addPlayerToNode();
                    return AgentAction.MOVE_WEST;
                }else{
                    return findBetterMove( currentLocation.west, attackMode);
                }
            case NOCHANGE:
                if (! conflictWithPendingMove( currentLocation, attackOrCapture(inEnvironment)) || attackOrCapture(inEnvironment).equals(ATTACK_MODE.CAPTURE_FLAG)){//todo bad hack :(
                    pendingMoves.add(currentLocation);
                    currentLocation.addPlayerToNode();
                    return AgentAction.DO_NOTHING;
                }else{
                    return findBetterMove(currentLocation, attackMode);
                }
            default:
                return AgentAction.DO_NOTHING;
        }
    }

    private void removePlayersFromAppropriateNodes(ArrayList<Node> pendingMoves){
        for (int row = 0; row < BOARD_SIZE; row++){
            for (int col = 0; col < BOARD_SIZE; col++){
                Node node = nodes[row][col];
                if (node != null && !pendingMoves.contains(node)){
                    node.removePlayerFromNode();
                }
                if (node != null && node.enemyLastSeen > ENEMY_TIMEOUT){
                    node.removeEnemyPlayer();
                }else if (node != null){
                    node.enemyLastSeen++;
                }
            }
        }
    }

    private boolean conflictWithPendingMove(Node newLocation, ATTACK_MODE attack_mode){
        if (newLocation == null)
            return true;
        if (pendingMoves.contains(newLocation) || !newLocation.isEnterable(attack_mode))
            return true;
        return false;
    }

    private int findBetterMove(Node newLocation, ATTACK_MODE attack_mode){
        HashMap<Node, DIRECTION> possibleMoves = new HashMap<Node, DIRECTION>();
        if (currentLocation.north != null && !currentLocation.north.equals(newLocation))
            possibleMoves.put(currentLocation.north, DIRECTION.NORTH);
        if ( currentLocation.south != null && !currentLocation.south.equals(newLocation))
            possibleMoves.put(currentLocation.south, DIRECTION.SOUTH);
        if ( currentLocation.east != null && !currentLocation.east.equals(newLocation))
            possibleMoves.put(currentLocation.east, DIRECTION.EAST);
        if ( currentLocation.west != null && !currentLocation.west.equals(newLocation))
            possibleMoves.put(currentLocation.west, DIRECTION.WEST);

        if (possibleMoves.size() == 0) {
            currentLocation.addPlayerToNode();
            return AgentAction.DO_NOTHING;
        }
        else{
            for (Node newNode : possibleMoves.keySet()) {
                if (!conflictWithPendingMove(newNode, attack_mode)) {
                    DIRECTION newDirection = possibleMoves.get(newNode);
                    switch (newDirection) {
                        case NORTH:
                            pendingMoves.add(currentLocation.north);
                            moveDirection = DIRECTION.NORTH;
                            currentLocation.north.addPlayerToNode();
                            return AgentAction.MOVE_NORTH;
                        case SOUTH:
                            pendingMoves.add(currentLocation.south);
                            moveDirection = DIRECTION.SOUTH;
                            currentLocation.south.addPlayerToNode();
                            return AgentAction.MOVE_SOUTH;
                        case EAST:
                            pendingMoves.add(currentLocation.east);
                            moveDirection = DIRECTION.EAST;
                            currentLocation.east.addPlayerToNode();
                            return AgentAction.MOVE_EAST;
                        case WEST:
                            pendingMoves.add(currentLocation.west);
                            moveDirection = DIRECTION.WEST;
                            currentLocation.west.addPlayerToNode();
                            return AgentAction.MOVE_WEST;
                        case NOCHANGE:
                        default:
                            moveDirection = DIRECTION.NOCHANGE;
                            pendingMoves.add(currentLocation);
                            currentLocation.addPlayerToNode();
                            return AgentAction.DO_NOTHING;
                    }
                }
            }
            return AgentAction.DO_NOTHING;
        }
    }

    private int directionToAction(DIRECTION direction){
        switch (direction){

            case NORTH:
                return AgentAction.MOVE_NORTH;
//                break;
            case SOUTH:
                return AgentAction.MOVE_SOUTH;
//                break;
            case EAST:
                return AgentAction.MOVE_EAST;
//                break;
            case WEST:
                return AgentAction.MOVE_WEST;
//                break;
            case NOCHANGE:
            default:
                return AgentAction.DO_NOTHING;
//                break;
        }
    }

    //can be used to test if the agent has returned to their initial location
    public boolean inInitialLocation(AgentEnvironment inEnvironment){
        if (friendlyInitialFlagDirection.equals(DIRECTION.SOUTH)){
            if (inEnvironment.isBaseSouth(AgentEnvironment.OUR_TEAM, false)
                    && ! inEnvironment.isBaseEast(AgentEnvironment.OUR_TEAM, false)
                    && ! inEnvironment.isBaseWest(AgentEnvironment.OUR_TEAM, false)){
                if(friendlyBaseWestOrEast.equals(DIRECTION.WEST)){
                    if (inEnvironment.isObstacleNorthImmediate() && inEnvironment.isObstacleWestImmediate())
                        return true;
                    else return false;
                }else{
                    if (inEnvironment.isObstacleNorthImmediate() && inEnvironment.isObstacleEastImmediate())
                        return true;
                    else return false;
                }
            }else return false;
        }else{
            if (inEnvironment.isBaseNorth(AgentEnvironment.OUR_TEAM, false)
                    && ! inEnvironment.isBaseEast(AgentEnvironment.OUR_TEAM, false)
                    && ! inEnvironment.isBaseWest(AgentEnvironment.OUR_TEAM, false)){
                if(friendlyBaseWestOrEast.equals(DIRECTION.WEST)){
                    if (inEnvironment.isObstacleSouthImmediate() && inEnvironment.isObstacleWestImmediate())
                        return true;
                    else return false;
                }else{
                    if (inEnvironment.isObstacleSouthImmediate() && inEnvironment.isObstacleEastImmediate())
                        return true;
                    else return false;
                }
            }else return false;
        }
    }

    public void doInitialSetup(AgentEnvironment inEnvironment){
        //friendlyBaseWestOrEast = inEnvironment.isBaseEast(AgentEnvironment.ENEMY_TEAM, false) ? DIRECTION.EAST : DIRECTION.WEST;

        for (int row = 0; row < BOARD_SIZE; row++){
            for (int col = 0; col < BOARD_SIZE; col++) {
                Node n = nodes[row][col];

                //setup heuristics
                if (this.friendlyBaseWestOrEast.equals(DIRECTION.EAST)){
                    n.friendlyHeuristic = Math.abs(row - FLAG_ROW) + Math.abs(col - (BOARD_SIZE - 1));
                    n.enemyHeuristic = Math.abs(row - FLAG_ROW) + Math.abs(col - 0);
                }else{
                    n.enemyHeuristic = Math.abs(row - FLAG_ROW) + Math.abs(col - (BOARD_SIZE - 1));
                    n.friendlyHeuristic = Math.abs(row - FLAG_ROW) + Math.abs(col - 0);
                }

                //setup adjacencies
                if (row == 0 && col == 0){
                    n.north = null;
                    n.west = null;
                    n.east =  nodes[0][1];
                    n.south = nodes[1][0];
                }else if (row == 0 && col != BOARD_SIZE -1){
                    n.north = null;
                    n.west = nodes[row][col-1];
                    n.east =  nodes[row][col+1];
                    n.south = nodes[row+1][col];
                }else if (row == 0 && col == BOARD_SIZE -1){
                    n.north = null;
                    n.west = nodes[row][col-1];
                    n.east =  null;
                    n.south = nodes[row+1][col];
                }else if (row != 0 && row != BOARD_SIZE -1 && col == 0){
                    n.north = nodes[row-1][col];
                    n.west = null;
                    n.east =  nodes[row][col+1];
                    n.south = nodes[row+1][col];
                }else if (row != 0 && row != BOARD_SIZE -1 && col != BOARD_SIZE-1){
                    n.north = nodes[row-1][col];
                    n.west = nodes[row][col-1];
                    n.east =  nodes[row][col+1];
                    n.south = nodes[row+1][col];
                }else if (row != 0 && row != BOARD_SIZE -1 && col == BOARD_SIZE-1){
                    n.north = nodes[row-1][col];
                    n.west = nodes[row][col-1];
                    n.east =  null;
                    n.south = nodes[row+1][col];
                }else if (row == BOARD_SIZE -1 && col == 0){
                    n.north = nodes[row-1][col];
                    n.west = null;
                    n.east =  nodes[row][col+1];
                    n.south = null;
                }else if (row == BOARD_SIZE -1 && col != BOARD_SIZE-1 && col != 0){
                    n.north = nodes[row-1][col];
                    n.west = nodes[row][col-1];
                    n.east =  nodes[row][col+1];
                    n.south = null;
                }else if (row == BOARD_SIZE -1 && col == BOARD_SIZE-1){
                    n.north = nodes[row-1][col];
                    n.west = nodes[row][col-1];
                    n.east =  null;
                    n.south = null;
                }

                //setup node types
                if (col == 0 || col == BOARD_SIZE-1){
                    n.nodeTypes.add(NODE_TYPE.ENTERABLE_SPACE);
                }else{
                    n.nodeTypes.add(NODE_TYPE.UNKOWN);
                }

                nodes[row][col] = n;
            }
        }

        if (this.friendlyBaseWestOrEast.equals(DIRECTION.EAST)){
            this.nodes[(BOARD_SIZE / 2) ][BOARD_SIZE-1].nodeTypes.add(NODE_TYPE.FRIENDLY_BASE);
            this.nodes[(BOARD_SIZE / 2) ][0].nodeTypes.add(NODE_TYPE.ENEMY_BASE);
        }else{
            this.nodes[(BOARD_SIZE / 2) ][BOARD_SIZE-1].nodeTypes.add(NODE_TYPE.ENEMY_BASE);
            this.nodes[(BOARD_SIZE / 2) ][0].nodeTypes.add(NODE_TYPE.FRIENDLY_BASE);
        }

        //todo maybe set player locations

    }

    public void setupInitialLocation(AgentEnvironment inEnvironment){
        if (friendlyInitialFlagDirection.equals(DIRECTION.SOUTH)){
            if(friendlyBaseWestOrEast.equals(DIRECTION.WEST)){
                initialLocation = nodes[0][0];
            }else{
                initialLocation = nodes[0][BOARD_SIZE-1];
            }
        }else{
            if(friendlyBaseWestOrEast.equals(DIRECTION.WEST)){
                initialLocation = nodes[BOARD_SIZE-1][0];
            }else{
                initialLocation = nodes[BOARD_SIZE-1][BOARD_SIZE-1];
            }
        }
    }

    public void updateSurroundingNodes(AgentEnvironment inEnvironment){
        //obstacle search
        //search for obstacles north
        if (inEnvironment.isObstacleNorthImmediate()){
            if (currentLocation.north != null) {
                //currentLocation.north.updateNode(NODE_TYPE.OBSTACLE);
                nodes[currentLocation.north.row][currentLocation.north.col] = null;
                currentLocation.north = null;
            }
        }else {
            //if not obstacle it is enterable space
            currentLocation.north.updateNode(NODE_TYPE.ENTERABLE_SPACE);
        }
        //search for obstacles east
        if (inEnvironment.isObstacleEastImmediate()){
            if (currentLocation.east != null) {
                //currentLocation.east.updateNode(NODE_TYPE.OBSTACLE);
                nodes[currentLocation.east.row][currentLocation.east.col] = null;
                currentLocation.east = null;
            }
        }else {
            //if not obstacle it is enterable space
            currentLocation.east.updateNode(NODE_TYPE.ENTERABLE_SPACE);
        }
        //search for obstacles west
        if (inEnvironment.isObstacleWestImmediate()){
            if (currentLocation.west != null) {
                //currentLocation.west.updateNode(NODE_TYPE.OBSTACLE);
                nodes[currentLocation.west.row][currentLocation.west.col] = null;
                currentLocation.west = null;
            }
        }else {
            //if not obstacle it is enterable space
            currentLocation.west.updateNode(NODE_TYPE.ENTERABLE_SPACE);
        }
        //search for obstacles south
        if (inEnvironment.isObstacleSouthImmediate()){
            if (currentLocation.south != null) {
                //currentLocation.south.updateNode(NODE_TYPE.OBSTACLE);
                nodes[currentLocation.south.row][currentLocation.south.col] = null;
                currentLocation.south = null;
            }
        }else {
            //if not obstacle it is enterable space
            currentLocation.south.updateNode(NODE_TYPE.ENTERABLE_SPACE);
        }

        //todo enemy search
        //search for enemy north
        if (inEnvironment.isAgentNorth(AgentEnvironment.ENEMY_TEAM, true)){
            if (currentLocation.north != null) {
                //currentLocation.north.updateNode(NODE_TYPE.OBSTACLE);
                nodes[currentLocation.north.row][currentLocation.north.col].updateNode(NODE_TYPE.ENEMY_LOCATION);
            }
        }else {
            //currentLocation.north.updateNode(NODE_TYPE.ENEMY_LOCATION);
        }
        if (inEnvironment.isAgentSouth(AgentEnvironment.ENEMY_TEAM, true)) {
            if (currentLocation.south != null) {
                //currentLocation.north.updateNode(NODE_TYPE.OBSTACLE);
                nodes[currentLocation.south.row][currentLocation.south.col].updateNode(NODE_TYPE.ENEMY_LOCATION);
            }
        }
        if (inEnvironment.isAgentEast(AgentEnvironment.ENEMY_TEAM, true)) {
            if (currentLocation.east != null) {
                //currentLocation.north.updateNode(NODE_TYPE.OBSTACLE);
                nodes[currentLocation.east.row][currentLocation.east.col].updateNode(NODE_TYPE.ENEMY_LOCATION);
            }
        }
        if (inEnvironment.isAgentWest(AgentEnvironment.ENEMY_TEAM, true)) {
            if (currentLocation.west != null) {
                //currentLocation.north.updateNode(NODE_TYPE.OBSTACLE);
                nodes[currentLocation.west.row][currentLocation.west.col].updateNode(NODE_TYPE.ENEMY_LOCATION);
            }
        }
//        //search for obstacles east
//        if (inEnvironment.isObstacleEastImmediate()){
//            if (currentLocation.east != null) {
//                //currentLocation.east.updateNode(NODE_TYPE.OBSTACLE);
//                nodes[currentLocation.east.row][currentLocation.east.col] = null;
//                currentLocation.east = null;
//            }
//        }else {
//            //if not obstacle it is enterable space
//            currentL//        Node ourBase = friendlyBaseWestOrEast.equals(DIRECTION.WEST) ? nodes[BOARD_SIZE/2][0] : nodes[BOARD_SIZE/2][BOARD_SIZE-1];
//        if (inEnvironment.hasFlag(AgentEnvironment.ENEMY_TEAM)){
//            //make our base enterable if enemy has our flag
//            ourBase.updateNode(NODE_TYPE.ENTERABLE_SPACE);
//        }else{
//            //our base is not enterable otherwise
//            if (ourBase.nodeTypes.contains(NODE_TYPE.ENTERABLE_SPACE))
//                ourBase.nodeTypes.remove(NODE_TYPE.ENTERABLE_SPACE);
//        }ocation.east.updateNode(NODE_TYPE.ENTERABLE_SPACE);
//        }
//        //search for obstacles west
//        if (inEnvironment.isObstacleWestImmediate()){
//            if (currentLocation.west != null) {
//                //currentLocation.west.updateNode(NODE_TYPE.OBSTACLE);
//                nodes[currentLocation.west.row][currentLocation.west.col] = null;
//                currentLocation.west = null;
//            }
//        }else {
//            //if not obstacle it is enterable space
//            currentLocation.west.updateNode(NODE_TYPE.ENTERABLE_SPACE);
//        }
//        //search for obstacles south
//        if (inEnvironment.isObstacleSouthImmediate()){
//            if (currentLocation.south != null) {
//                //currentLocation.south.updateNode(NODE_TYPE.OBSTACLE);
//                nodes[currentLocation.south.row][currentLocation.south.col] = null;
//                currentLocation.south = null;
//            }
//        }else {
//            //if not obstacle it is enterable space
//            currentLocation.south.updateNode(NODE_TYPE.ENTERABLE_SPACE);
//        }
//

        //update freidnly base enterability based on whether or not enemy has flag
        Node ourBase = friendlyBaseWestOrEast.equals(DIRECTION.WEST) ? nodes[BOARD_SIZE/2][0] : nodes[BOARD_SIZE/2][BOARD_SIZE-1];
        if (inEnvironment.hasFlag(AgentEnvironment.ENEMY_TEAM)){
            //make our base enterable if enemy has our flag
            ourBase.updateNode(NODE_TYPE.ENTERABLE_SPACE);
        }else{
            //our base is not enterable otherwise
            if (ourBase.nodeTypes.contains(NODE_TYPE.ENTERABLE_SPACE))
                ourBase.nodeTypes.remove(NODE_TYPE.ENTERABLE_SPACE);
        }
    }

    //update location and add new location ot the path
    public void updateLocation(AgentEnvironment inEnvironment){
        if (inInitialLocation(inEnvironment)){
            currentLocation.removePlayerFromNode();
            currentLocation = initialLocation;
            currentLocation.addPlayerToNode();
            path = new ArrayList<Node>();//new path on tag
        }else{//if we are not in the initial position, then our move was successful
            switch (moveDirection){
                case NORTH:
                    //previousLocation = currentLocation;
                    if (!inEnvironment.isAgentNorth(AgentEnvironment.OUR_TEAM, true))
                        currentLocation.removePlayerFromNode();
                    //todo check for there is not an agent in opposite direction of move, remove player from node
                    if (currentLocation.isDeadEnd()) {
                        Node newLocation = currentLocation.north;
                        nodes[currentLocation.row][currentLocation.col] = null;
                        currentLocation = newLocation;
                    }else {
                        currentLocation = currentLocation.north == null ? currentLocation : currentLocation.north;
                    }
                    currentLocation.addPlayerToNode();
                    break;
                case SOUTH:
                    //previousLocation = currentLocation;
                    if (!inEnvironment.isAgentSouth(AgentEnvironment.OUR_TEAM, true))
                        currentLocation.removePlayerFromNode();
                    if (currentLocation.isDeadEnd()) {
                        Node newLocation = currentLocation.south;
                        nodes[currentLocation.row][currentLocation.col] = null;
                        currentLocation = newLocation;
                    }else {
                        currentLocation = currentLocation.south == null ? currentLocation : currentLocation.south;
                    }
                    currentLocation.addPlayerToNode();
                    break;
                case EAST:
                    //previousLocation = currentLocation;
                    if (!inEnvironment.isAgentEast(AgentEnvironment.OUR_TEAM, true))
                        currentLocation.removePlayerFromNode();
                    if (currentLocation.isDeadEnd()) {
                        Node newLocation = currentLocation.east;
                        nodes[currentLocation.row][currentLocation.col] = null;
                        currentLocation = newLocation;
                    }else {
                        currentLocation = currentLocation.east == null ? currentLocation : currentLocation.east;
                    }
                    currentLocation.addPlayerToNode();
                    break;
                case WEST:
                    //previousLocation = currentLocation;
                    if (!inEnvironment.isAgentWest(AgentEnvironment.OUR_TEAM, true))
                        currentLocation.removePlayerFromNode();
                    if (currentLocation.isDeadEnd()) {
                        Node newLocation = currentLocation.west;
                        nodes[currentLocation.row][currentLocation.col] = null;
                        currentLocation = newLocation;
                    }else {
                        currentLocation = currentLocation.west == null ? currentLocation : currentLocation.west;
                    }
                    currentLocation.addPlayerToNode();
                    break;
            }
            //path.add(currentLocation);
        }
    }

    //updates path, handling if last node visited was dead end
    public void updatePath(){
        if (path.size() > 0 && path.get(path.size()-1).isDeadEnd()){
            Node deadEndNode = path.get(path.size()-1);
            path.remove(deadEndNode);
            nodes[deadEndNode.row][deadEndNode.col] = null;
        }else if (path.size() > 0 && !path.get(path.size()-1).equals(currentLocation)) {
//
//        }else {//if (!path.contains(currentLocation)){
//
            path.add(currentLocation);

        }else if (path.size() > 0 && path.get(path.size()-1).equals(currentLocation)) {
            //do nothing

        }

        else{
            path.add(currentLocation);

        }
    }

    //todo: this is not complete
    public boolean blockingOurOtherAgent(AgentEnvironment inEnvironment){
        ArrayList<DIRECTION> possibleMoves = new ArrayList<DIRECTION>();
        if (! inEnvironment.isObstacleNorthImmediate() && currentLocation.north != null)
            possibleMoves.add(DIRECTION.NORTH);
        if (! inEnvironment.isObstacleSouthImmediate() && currentLocation.south != null)
            possibleMoves.add(DIRECTION.SOUTH);
        if (! inEnvironment.isObstacleEastImmediate() && currentLocation.east != null)
            possibleMoves.add(DIRECTION.EAST);
        if (! inEnvironment.isObstacleWestImmediate() && currentLocation.west != null)
            possibleMoves.add(DIRECTION.WEST);

        boolean isBlocking = false;
        for (DIRECTION direction : possibleMoves){
            switch (direction){

                case NORTH:
                    if (inEnvironment.isAgentNorth(AgentEnvironment.OUR_TEAM, true))
                        isBlocking = true;
                    break;
                case SOUTH:
                    if (inEnvironment.isAgentSouth(AgentEnvironment.OUR_TEAM, true))
                        isBlocking = true;
                    break;
                case EAST:
                    if (inEnvironment.isAgentEast(AgentEnvironment.OUR_TEAM, true))
                        isBlocking = true;
                    break;
                case WEST:
                    if (inEnvironment.isAgentWest(AgentEnvironment.OUR_TEAM, true))
                        isBlocking = true;
                    break;
            }
        }

        return isBlocking;
    }

//    public DIRECTION findBestNonconflictingMove(AgentEnvironment inEnvironment, DIRECTION pendingDirection){
//        switch (pendingDirection) {
//
//            case NORTH:
//                if (currentLocation.north.isEnterable())
//                    return pendingDirection;
//                break;
//            case SOUTH:
//                if (currentLocation.south.isEnterable())
//                    return pendingDirection;
//                break;
//            case EAST:
//                if (! inEnvironment.isAgentEast(AgentEnvironment.OUR_TEAM, true))
//                    return pendingDirection;
//                break;
//            case WEST:
//                if (! inEnvironment.isAgentWest(AgentEnvironment.OUR_TEAM, true))
//                    return pendingDirection;
//                break;
//        }
//
//        //DIRECTION newDirection  = DIRECTION.NOCHANGE;
//
//        ArrayList<DIRECTION> possibleMoves = new ArrayList<DIRECTION>();
//        if (! inEnvironment.isObstacleNorthImmediate() && currentLocation.north != null)
//            possibleMoves.add(DIRECTION.NORTH);
//        if (! inEnvironment.isObstacleSouthImmediate() && currentLocation.south != null)
//            possibleMoves.add(DIRECTION.SOUTH);
//        if (! inEnvironment.isObstacleEastImmediate() && currentLocation.east != null)
//            possibleMoves.add(DIRECTION.EAST);
//        if (! inEnvironment.isObstacleWestImmediate() && currentLocation.west != null)
//            possibleMoves.add(DIRECTION.WEST);
//
//        for (DIRECTION possibleDirection : possibleMoves) {
//            switch (possibleDirection) {
//
//                case NORTH:
//                    if (! inEnvironment.isAgentNorth(AgentEnvironment.OUR_TEAM, true))
//                        return DIRECTION.NORTH;
//                case SOUTH:
//                    if (! inEnvironment.isAgentSouth(AgentEnvironment.OUR_TEAM, true))
//                        return DIRECTION.SOUTH;
//                case EAST:
//                    if (! inEnvironment.isAgentEast(AgentEnvironment.OUR_TEAM, true))
//                        return DIRECTION.EAST;
//                case WEST:
//                    if (! inEnvironment.isAgentWest(AgentEnvironment.OUR_TEAM, true))
//                        return DIRECTION.WEST;
//            }
//        }
//        return DIRECTION.NOCHANGE;
//    }

    public void handleDeadEnd(AgentEnvironment inEnvironment){
        ArrayList<Node> possibleMoves = new ArrayList<Node>();
        boolean capFlag = attackOrCapture(inEnvironment).equals(ATTACK_MODE.CAPTURE_FLAG);
        if (! inEnvironment.isObstacleNorthImmediate() && currentLocation.north != null
                && (capFlag && !inEnvironment.isAgentNorth(AgentEnvironment.ENEMY_TEAM, true)))
            possibleMoves.add(currentLocation.north);
        if (! inEnvironment.isObstacleSouthImmediate() && currentLocation.south != null
                && (capFlag && !inEnvironment.isAgentSouth(AgentEnvironment.ENEMY_TEAM, true)))
            possibleMoves.add(currentLocation.south);
        if (! inEnvironment.isObstacleEastImmediate() && currentLocation.east != null
                && (capFlag && !inEnvironment.isAgentEast(AgentEnvironment.ENEMY_TEAM, true)))
            possibleMoves.add(currentLocation.east);
        if (! inEnvironment.isObstacleWestImmediate() && currentLocation.west != null
                && (capFlag && !inEnvironment.isAgentWest(AgentEnvironment.ENEMY_TEAM, true)))
            possibleMoves.add(currentLocation.west);
        if (possibleMoves.size() == 1){
            if (path.size() >= 2 && path.get(path.size()-2) == possibleMoves.get(0)){
                //dead end if the only move is back to where we came from
                currentLocation.updateNode(NODE_TYPE.DEAD_END);
                //previousLocation = path.get(path.size()-1);
            }
        }
    }

    //returns the direction of the enemy to tag or NOCHANGE if not tagging
    private DIRECTION tagEnemy(AgentEnvironment inEnvironment){
        //TODO!
        return DIRECTION.NOCHANGE;
    }

    private ATTACK_MODE attackOrCapture(AgentEnvironment inEnvironment){
        //TODO!
        if (inEnvironment.hasFlag())
            return ATTACK_MODE.CAPTURE_FLAG;
        return ATTACK_MODE.SEEK_FLAG;
    }

    //a* for pathfinding
    private DIRECTION findBestPathToGoal(final ATTACK_MODE attackMode, AgentEnvironment inEnvironment){
        PriorityQueue<SearchNode> nodeQueue = new PriorityQueue<SearchNode>(BOARD_SIZE * BOARD_SIZE, new Comparator<SearchNode>() {
            @Override
            public int compare(SearchNode node, SearchNode t1) {
                int nodeH = attackMode.equals(ATTACK_MODE.SEEK_FLAG) ? node.thisNode.enemyHeuristic : node.thisNode.friendlyHeuristic,
                    t1H = attackMode.equals(ATTACK_MODE.SEEK_FLAG) ? t1.thisNode.enemyHeuristic : t1.thisNode.friendlyHeuristic;
                return Integer.compare((nodeH + node.cumulativeDistance),(t1H + t1.cumulativeDistance));
            }
        });
        SearchNode parentSearchNode = new SearchNode(currentLocation, null, 0, DIRECTION.NOCHANGE);
        nodeQueue.add(parentSearchNode);
        ArrayList<Node> expandedNodes = new ArrayList<Node>();
        while(!nodeQueue.isEmpty()){
            SearchNode searchNode = nodeQueue.remove();
            while (nodeAlreadyExpanded(searchNode.thisNode, expandedNodes) && !nodeQueue.isEmpty())
                searchNode = nodeQueue.remove();
            expandedNodes.add(searchNode.thisNode);
            if (nodeIsGoal(searchNode, attackMode)){
                SearchNode currNode = searchNode;
                if (currNode.parentNode == null)//we've reached our destination already
                    return DIRECTION.NOCHANGE;
                while (!currNode.parentNode.equals(parentSearchNode))
                    currNode = currNode.parentNode;
                return currNode.directionFromParent;
            }
            if (searchNode.thisNode.north != null && searchNode.thisNode.north.isEnterable(attackMode)
                    ) {// && nodeCanBeOccupied(inEnvironment, DIRECTION.NORTH, searchNode.thisNode.north))// && !inEnvironment.isAgentNorth(AgentEnvironment.OUR_TEAM, true))
                SearchNode northNode = new SearchNode(searchNode.thisNode.north, searchNode, 1 + searchNode.cumulativeDistance, DIRECTION.NORTH);
                //if (!expandedNodes.contains(northNode.thisNode.north))
                if (!nodeAlreadyExpanded(searchNode.thisNode.north, expandedNodes)) {
                    if (attackMode.equals(ATTACK_MODE.CAPTURE_FLAG) && northNode.thisNode.hasEnemy()){
                        //dont expand node
                    }else {
                        nodeQueue.add(northNode);
                    }
                }
            }
            if (searchNode.thisNode.south != null && searchNode.thisNode.south.isEnterable(attackMode)
                    ) {//&& nodeCanBeOccupied(inEnvironment, DIRECTION.SOUTH, searchNode.thisNode.south))// && !inEnvironment.isAgentSouth(AgentEnvironment.OUR_TEAM, true))
                SearchNode southNode = new SearchNode(searchNode.thisNode.south, searchNode, 1 + searchNode.cumulativeDistance, DIRECTION.SOUTH);
                //if (!expandedNodes.contains(southNode.thisNode.south))
                if (!nodeAlreadyExpanded(searchNode.thisNode.south, expandedNodes)){
                    if (attackMode.equals(ATTACK_MODE.CAPTURE_FLAG) && southNode.thisNode.hasEnemy()){
                        //dont expand node
                    }else {
                        nodeQueue.add(southNode);
                    }
                }
            }
            if (searchNode.thisNode.east != null && searchNode.thisNode.east.isEnterable(attackMode)
                    ) {// && nodeCanBeOccupied(inEnvironment, DIRECTION.EAST, searchNode.thisNode.east))// && !inEnvironment.isAgentEast(AgentEnvironment.OUR_TEAM, true))
                SearchNode eastNode = new SearchNode(searchNode.thisNode.east, searchNode, 1 + searchNode.cumulativeDistance, DIRECTION.EAST);
                //if (!expandedNodes.contains(eastNode.thisNode.east))
                if (!nodeAlreadyExpanded(searchNode.thisNode.east, expandedNodes)){
                    if (attackMode.equals(ATTACK_MODE.CAPTURE_FLAG) && eastNode.thisNode.hasEnemy()){
                        //dont expand node
                    }else {
                        nodeQueue.add(eastNode);
                    }
                }
            }
            if (searchNode.thisNode.west != null && searchNode.thisNode.west.isEnterable(attackMode)
                    ) {// && nodeCanBeOccupied(inEnvironment, DIRECTION.WEST, searchNode.thisNode.west))// && !inEnvironment.isAgentWest(AgentEnvironment.OUR_TEAM, true))
                SearchNode westNode = new SearchNode(searchNode.thisNode.west, searchNode, 1 + searchNode.cumulativeDistance, DIRECTION.WEST);
                //if (!expandedNodes.contains(westNode.thisNode.west))
                if (!nodeAlreadyExpanded(searchNode.thisNode.west, expandedNodes)){
                    if (attackMode.equals(ATTACK_MODE.CAPTURE_FLAG) && westNode.thisNode.hasEnemy()){
                        //dont expand node
                    }else {
                        nodeQueue.add(westNode);
                    }
                }
            }
        }
        //hack because this code is broken :(
//        if (attackMode.equals(ATTACK_MODE.CAPTURE_FLAG)){
//            DIRECTION pathDirection = followPathBackHome();
//            //switch (pathDirection)
//            return pathDirection;
//        }

        return DIRECTION.NOCHANGE;//case when pathfinding fails
    }

    private boolean nodeIsGoal(SearchNode node, ATTACK_MODE attackMode){
        return (attackMode.equals(ATTACK_MODE.SEEK_FLAG)) ?
                node.thisNode.nodeTypes.contains(NODE_TYPE.ENEMY_BASE) :
                node.thisNode.nodeTypes.contains(NODE_TYPE.FRIENDLY_BASE);
    }

    private DIRECTION followPathBackHome(){
        int homeCol = friendlyBaseWestOrEast.equals(DIRECTION.EAST) ? BOARD_SIZE-1 : 0;
        if (currentLocation.col == homeCol)
            return directionPrevToCurrentNode(currentLocation, nodes[BOARD_SIZE/2][homeCol]);
        while (currentLocation.equals(path.get(path.size()-1))){
            path.remove(path.size()-1);
        }
        return directionPrevToCurrentNode(currentLocation, path.get(path.size()-1));
    }

    private DIRECTION directionPrevToCurrentNode(Node current, Node prevNode){
        if (current.row > prevNode.row)
            return DIRECTION.NORTH;
        if (current.row < prevNode.row)
            return DIRECTION.SOUTH;
        if (current.col < prevNode.col)
            return DIRECTION.EAST;
        if (current.col > prevNode.col)
            return DIRECTION.WEST;
        return DIRECTION.NOCHANGE;
    }

    private boolean nodeAlreadyExpanded(Node node, ArrayList<Node> expandedNodes){
        boolean alreadyExpanded = false;

        for (Node n: expandedNodes){
            if (n.row == node.row && n.col == node.col)
                alreadyExpanded = true;
        }

        return alreadyExpanded;
    }

//    private boolean nodeCanBeOccupied(AgentEnvironment inEnvironment, DIRECTION direction, Node node){
//        boolean canBeOccupied = true;
//        switch(direction){
//            case NORTH:
//                if (inEnvironment.isAgentNorth(AgentEnvironment.OUR_TEAM, true) ||
//                    (inEnvironment.isBaseNorth(AgentEnvironment.OUR_TEAM, true) && !inEnvironment.hasFlag(AgentEnvironment.ENEMY_TEAM)) &&
//                    node.isEnterable())
//                    canBeOccupied = false;
//                break;
//            case SOUTH:
//                if (inEnvironment.isAgentSouth(AgentEnvironment.OUR_TEAM, true) ||
//                    (inEnvironment.isBaseSouth(AgentEnvironment.OUR_TEAM, true) && !inEnvironment.hasFlag(AgentEnvironment.ENEMY_TEAM)) &&
//                    node.isEnterable())
//                    canBeOccupied = false;
//                break;
//            case EAST:
//                if (inEnvironment.isAgentEast(AgentEnvironment.OUR_TEAM, true) ||
//                    (inEnvironment.isBaseEast(AgentEnvironment.OUR_TEAM, true) && !inEnvironment.hasFlag(AgentEnvironment.ENEMY_TEAM)) &&
//                     node.isEnterable())
//                    canBeOccupied = false;
//                break;
//            case WEST:
//                if (inEnvironment.isAgentWest(AgentEnvironment.OUR_TEAM, true) ||
//                    (inEnvironment.isBaseWest(AgentEnvironment.OUR_TEAM, true) && !inEnvironment.hasFlag(AgentEnvironment.ENEMY_TEAM)) &&
//                    node.isEnterable())
//                    canBeOccupied = false;
//                break;
//        }
//        return canBeOccupied;
//    }

    private class Node{
        Node west;
        Node east;
        Node north;
        Node south;
        int row,col;

        int enemyHeuristic,
            friendlyHeuristic; //heuristic for going to friendly base (cappping flag)
        int enemyLastSeen = -1;

        ArrayList<NODE_TYPE> nodeTypes;

        public Node(){
            this.nodeTypes = new ArrayList<NODE_TYPE>();
        }

        public Node(Node west, Node east, Node north, Node south){
            this.west = west;
            this.east = east;
            this.north = north;
            this.south = south;
            this.nodeTypes = new ArrayList<NODE_TYPE>();
        }

        //removes unkown node type and adds n if it is not already in the node type list
        public void updateNode(NODE_TYPE n) {
            if (nodeTypes.contains(NODE_TYPE.UNKOWN)) {
                nodeTypes.remove(NODE_TYPE.UNKOWN);
            }
            if (!nodeTypes.contains(n)) {
                nodeTypes.add(n);
            }
            if (n.equals(NODE_TYPE.ENTERABLE_SPACE) && nodeTypes.contains(NODE_TYPE.PLAYER_LOCATION)){
                nodeTypes.remove(NODE_TYPE.ENTERABLE_SPACE);
            }
            if (nodeTypes.contains(NODE_TYPE.FRIENDLY_BASE) && n.equals(NODE_TYPE.ENTERABLE_SPACE))
                nodeTypes.remove(NODE_TYPE.ENTERABLE_SPACE);
            if (n.equals(NODE_TYPE.ENEMY_LOCATION))
                this.enemyLastSeen = 0;
        }

        public void addPlayerToNode(){
            if (nodeTypes.contains(NODE_TYPE.ENTERABLE_SPACE) && !nodeTypes.contains(NODE_TYPE.ENEMY_BASE))
                nodeTypes.remove(NODE_TYPE.ENTERABLE_SPACE);
            if (!nodeTypes.contains(NODE_TYPE.PLAYER_LOCATION))
                nodeTypes.add(NODE_TYPE.PLAYER_LOCATION);
        }

        public void removeEnemyPlayer() {
            nodeTypes.remove(NODE_TYPE.ENEMY_LOCATION);
            enemyLastSeen = -1;
        }

        public void removePlayerFromNode(){
            if (!nodeTypes.contains(NODE_TYPE.ENTERABLE_SPACE))
                nodeTypes.add(NODE_TYPE.ENTERABLE_SPACE);
            if (nodeTypes.contains(NODE_TYPE.PLAYER_LOCATION))
                nodeTypes.remove(NODE_TYPE.PLAYER_LOCATION);
        }

        public boolean isMine(){
            for (NODE_TYPE n : nodeTypes)
                if (n.equals(NODE_TYPE.KNOWN_MINE))
                    return true;
            return false;
        }

        public boolean isDeadEnd(){
            for (NODE_TYPE n : nodeTypes)
                if (n.equals(NODE_TYPE.DEAD_END))
                    return true;
            return false;
        }

        public boolean isEnterable(ATTACK_MODE attack_mode){
            if (nodeTypes.contains(NODE_TYPE.DEAD_END))
                return false;
            return nodeTypes.contains(NODE_TYPE.ENTERABLE_SPACE) || nodeTypes.contains(NODE_TYPE.UNKOWN) ||
                    (nodeTypes.contains(NODE_TYPE.FRIENDLY_BASE) && attack_mode.equals(ATTACK_MODE.CAPTURE_FLAG) ||
                    (nodeTypes.contains(NODE_TYPE.ENEMY_LOCATION) && !attack_mode.equals(ATTACK_MODE.CAPTURE_FLAG))) ;
        }
        public boolean hasEnemy(){
            return nodeTypes.contains(NODE_TYPE.ENEMY_LOCATION);
        }


        //todo ^^^ make these things ^^^

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Node node = (Node) o;

            if (col != node.col) return false;
            if (row != node.row) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = row;
            result = 31 * result + col;
            return result;
        }
    }

    private class SearchNode{
        Node thisNode;
        SearchNode parentNode;
        int cumulativeDistance;
        DIRECTION directionFromParent;

        public SearchNode(Node thisNode, SearchNode parentNode, int cumulativeDistance, DIRECTION directionFromParent) {
            this.thisNode = thisNode;
            this.parentNode = parentNode;
            this.cumulativeDistance = cumulativeDistance;
            this.directionFromParent = directionFromParent;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            SearchNode that = (SearchNode) o;

            if (!thisNode.equals(that.thisNode)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return thisNode.hashCode();
        }
    }
}
