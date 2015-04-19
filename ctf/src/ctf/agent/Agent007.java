package ctf.agent;

import ctf.common.AgentAction;
import ctf.common.AgentEnvironment;

import java.lang.Override;
import java.util.ArrayList;

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
        PLAYER_LOCATION
    }

    public static enum DIRECTION{
        NORTH, SOUTH, EAST, WEST
    }

    public static enum ATTACKER_TARGET{
        ENEMY_FLAG, OUR_FLAG
    }

    public static final int BOARD_SIZE = 10;
    public static final int FLAG_ROW = (BOARD_SIZE / 2 + 1);

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
    private DIRECTION moveDirection = null; //the move direction of the latest move made (used to update currentLocation)

    public Node[][] nodes = new Node[BOARD_SIZE][BOARD_SIZE];

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
    }

    @Override
    public int getMove( AgentEnvironment inEnvironment ){
        if (initialMove){
            doInitialSetup(inEnvironment);//do initial setup for both agents (static stuff)
        }
        if (friendlyInitialFlagDirection == null){//do initial setup for this agent (non-static stuff)
            friendlyInitialFlagDirection = inEnvironment.isFlagSouth(AgentEnvironment.OUR_TEAM, false) ? DIRECTION.SOUTH : DIRECTION.NORTH;
            this.setupInitialLocation(inEnvironment);
            currentLocation = initialLocation;
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
        }
        //todo check for dead end
        handleDeadEnd(inEnvironment);
        boolean willMove = true;
        //todo check if tagging conditions are met
        //todo check if we are in attack mode or capture mode
        //todo find best path to goal if not going to tag enemy(based on attack mode or capture mode)
        //todo use all the info above to make a move
        //todo before returning indicate in 'moveDirection' what direction we are moving in

        //execute the move
        if (willMove) {
            switch (moveDirection) {

                case NORTH:
                    return AgentAction.MOVE_NORTH;
                case SOUTH:
                    return AgentAction.MOVE_SOUTH;
                case EAST:
                    return AgentAction.MOVE_EAST;
                case WEST:
                    return AgentAction.MOVE_WEST;
            }
        }
        return AgentAction.DO_NOTHING;
    }

    //can be used to test if the agent has returned to their initial location
    public boolean inInitialLocation(AgentEnvironment inEnvironment){
        if (friendlyInitialFlagDirection.equals(DIRECTION.SOUTH)){
            if (inEnvironment.isAgentSouth(AgentEnvironment.OUR_TEAM, false)
                    && ! inEnvironment.isAgentEast(AgentEnvironment.OUR_TEAM, false)
                    && ! inEnvironment.isAgentWest(AgentEnvironment.OUR_TEAM, false)){
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
            if (inEnvironment.isAgentNorth(AgentEnvironment.OUR_TEAM, false)
                    && ! inEnvironment.isAgentEast(AgentEnvironment.OUR_TEAM, false)
                    && ! inEnvironment.isAgentWest(AgentEnvironment.OUR_TEAM, false)){
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
        friendlyBaseWestOrEast = inEnvironment.isBaseEast(AgentEnvironment.ENEMY_TEAM, false) ? DIRECTION.EAST : DIRECTION.WEST;

        for (int row = 0; row < BOARD_SIZE; row++){
            for (int col = 0; col < BOARD_SIZE; col++) {
                Node n = nodes[row][col];

                //setup heuristics
                if (this.friendlyInitialFlagDirection.equals(DIRECTION.EAST)){
                    n.friendlyHeuristic = Math.abs(row - FLAG_ROW) + Math.abs(col - (BOARD_SIZE - 1));
                    n.enemyHeuristic = Math.abs(row - FLAG_ROW) + Math.abs(col - 0);
                }else{
                    n.enemyHeuristic = Math.abs(row - FLAG_ROW) + Math.abs(col - (BOARD_SIZE - 1));
                    n.friendlyHeuristic = Math.abs(row - FLAG_ROW) + Math.abs(col - 0);
                }

                //setup adjacencies
                if (row == 0 && col == 0){
                    n.top = null;
                    n.left = null;
                    n.right =  nodes[0][1];
                    n.bottom = nodes[1][0];
                }else if (row == 0 && col != BOARD_SIZE -1){
                    n.top = null;
                    n.left = nodes[row][col-1];
                    n.right =  nodes[row][col+1];
                    n.bottom = nodes[row+1][col];
                }else if (row == 0 && col == BOARD_SIZE -1){
                    n.top = null;
                    n.left = nodes[row][col-1];
                    n.right =  null;
                    n.bottom = nodes[row+1][col];
                }else if (row != 0 && row != BOARD_SIZE -1 && col == 0){
                    n.top = nodes[row-1][col];
                    n.left = null;
                    n.right =  nodes[row][col+1];
                    n.bottom = nodes[row+1][col];
                }else if (row != 0 && row != BOARD_SIZE -1 && col != BOARD_SIZE-1){
                    n.top = nodes[row-1][col];
                    n.left = nodes[row][col-1];
                    n.right =  nodes[row][col+1];
                    n.bottom = nodes[row+1][col];
                }else if (row != 0 && row != BOARD_SIZE -1 && col == BOARD_SIZE-1){
                    n.top = nodes[row-1][col];
                    n.left = nodes[row][col-1];
                    n.right =  null;
                    n.bottom = nodes[row+1][col];
                }else if (row == BOARD_SIZE -1 && col == 0){
                    n.top = nodes[row-1][col];
                    n.left = null;
                    n.right =  nodes[row][col+1];
                    n.bottom = null;
                }else if (row == BOARD_SIZE -1 && col != BOARD_SIZE-1 && col != 0){
                    n.top = nodes[row-1][col];
                    n.left = nodes[row][col-1];
                    n.right =  nodes[row][col+1];
                    n.bottom = null;
                }else if (row == BOARD_SIZE -1 && col == BOARD_SIZE-1){
                    n.top = nodes[row-1][col];
                    n.left = nodes[row][col-1];
                    n.right =  null;
                    n.bottom = null;
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

        if (this.friendlyInitialFlagDirection.equals(DIRECTION.EAST)){
            this.nodes[(BOARD_SIZE / 2) + 1][BOARD_SIZE-1].nodeTypes.add(NODE_TYPE.FRIENDLY_BASE);
            this.nodes[(BOARD_SIZE / 2) + 1][0].nodeTypes.add(NODE_TYPE.ENEMY_BASE);
        }else{
            this.nodes[(BOARD_SIZE / 2) + 1][BOARD_SIZE-1].nodeTypes.add(NODE_TYPE.ENEMY_BASE);
            this.nodes[(BOARD_SIZE / 2) + 1][0].nodeTypes.add(NODE_TYPE.FRIENDLY_BASE);
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
        //search for obstacles north
        if (inEnvironment.isObstacleNorthImmediate()){
            if (currentLocation.top != null) {
                //currentLocation.top.updateNode(NODE_TYPE.OBSTACLE);
                currentLocation.top = null;
            }
        }else {
            //if not obstacle it is enterable space
            currentLocation.top.updateNode(NODE_TYPE.ENTERABLE_SPACE);
        }
        //search for obstacles east
        if (inEnvironment.isObstacleEastImmediate()){
            if (currentLocation.right != null) {
                //currentLocation.right.updateNode(NODE_TYPE.OBSTACLE);
                currentLocation.right = null;
            }
        }else {
            //if not obstacle it is enterable space
            currentLocation.right.updateNode(NODE_TYPE.ENTERABLE_SPACE);
        }
        //search for obstacles west
        if (inEnvironment.isObstacleWestImmediate()){
            if (currentLocation.left != null) {
                //currentLocation.left.updateNode(NODE_TYPE.OBSTACLE);
                currentLocation.left = null;
            }
        }else {
            //if not obstacle it is enterable space
            currentLocation.left.updateNode(NODE_TYPE.ENTERABLE_SPACE);
        }
        //search for obstacles south
        if (inEnvironment.isObstacleSouthImmediate()){
            if (currentLocation.bottom != null) {
                //currentLocation.bottom.updateNode(NODE_TYPE.OBSTACLE);
                currentLocation.bottom = null;
            }
        }else {
            //if not obstacle it is enterable space
            currentLocation.bottom.updateNode(NODE_TYPE.ENTERABLE_SPACE);
        }
    }

    //update location and add new location ot the path
    public void updateLocation(AgentEnvironment inEnvironment){
        if (inInitialLocation(inEnvironment)){
            currentLocation = initialLocation;
        }else{//if we are not in the initial position, then our move was successful
            switch (moveDirection){
                case NORTH:
                    //previousLocation = currentLocation;
                    currentLocation = currentLocation.top;
                    break;
                case SOUTH:
                    //previousLocation = currentLocation;
                    currentLocation = currentLocation.bottom;
                    break;
                case EAST:
                    //previousLocation = currentLocation;
                    currentLocation = currentLocation.right;
                    break;
                case WEST:
                    //previousLocation = currentLocation;
                    currentLocation = currentLocation.left;
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
        }else if (!path.contains(currentLocation)){
            path.add(currentLocation);
        }
    }

    public boolean blockingOurOtherAgent(AgentEnvironment inEnvironment){
        //TODO!!!
        return false;
    }

    public void handleDeadEnd(AgentEnvironment inEnvironment){
        ArrayList<Node> possibleMoves = new ArrayList<Node>();
        if (! inEnvironment.isObstacleNorthImmediate() && currentLocation.top != null)
            possibleMoves.add(currentLocation.top);
        if (! inEnvironment.isObstacleSouthImmediate() && currentLocation.bottom != null)
            possibleMoves.add(currentLocation.bottom);
        if (! inEnvironment.isObstacleEastImmediate() && currentLocation.right != null)
            possibleMoves.add(currentLocation.right);
        if (! inEnvironment.isObstacleWestImmediate() && currentLocation.left != null)
            possibleMoves.add(currentLocation.left);
        if (possibleMoves.size() == 1){
            if (path.get(path.size()-1) == possibleMoves.get(0)){
                //dead end if the only move is back to where we came from
                currentLocation.updateNode(NODE_TYPE.DEAD_END);
                //previousLocation = path.get(path.size()-1);
            }
        }
    }

    private class Node{
        Node left;
        Node right;
        Node top;
        Node bottom;
        int row,col;

        int enemyHeuristic,
            friendlyHeuristic; //heuristic for going to friendly base (cappping flag)

        ArrayList<NODE_TYPE> nodeTypes;

        public Node(){

        }

        public Node(Node left, Node right, Node top, Node bottom){
            this.left = left;
            this.right = right;
            this.top = top;
            this.bottom = bottom;
            this.nodeTypes = new ArrayList<NODE_TYPE>();
        }

        //removes unkown node type and adds n if it is not already in the node type list
        public void updateNode(NODE_TYPE n){
            if (nodeTypes.contains(NODE_TYPE.UNKOWN)){
                nodeTypes.remove(NODE_TYPE.UNKOWN);
            }
            if (! nodeTypes.contains(n)){
                nodeTypes.add(n);
            }
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

        //todo ^^^ make these things ^^^
    }
}
