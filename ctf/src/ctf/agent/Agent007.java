package ctf.agent;

import ctf.common.AgentAction;
import ctf.common.AgentEnvironment;

import java.lang.Override;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;

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
        NORTH, SOUTH, EAST, WEST, NOCHANGE
    }

    public static enum ATTACKER_TARGET{
        ENEMY_FLAG, OUR_FLAG
    }

    public static enum ATTACK_MODE{
        SEEK_FLAG, CAPTURE_FLAG
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
            if (currentLocation.north != null) {
                //currentLocation.north.updateNode(NODE_TYPE.OBSTACLE);
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
                currentLocation.south = null;
            }
        }else {
            //if not obstacle it is enterable space
            currentLocation.south.updateNode(NODE_TYPE.ENTERABLE_SPACE);
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
                    currentLocation = currentLocation.north;
                    break;
                case SOUTH:
                    //previousLocation = currentLocation;
                    currentLocation = currentLocation.south;
                    break;
                case EAST:
                    //previousLocation = currentLocation;
                    currentLocation = currentLocation.east;
                    break;
                case WEST:
                    //previousLocation = currentLocation;
                    currentLocation = currentLocation.west;
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
        if (! inEnvironment.isObstacleNorthImmediate() && currentLocation.north != null)
            possibleMoves.add(currentLocation.north);
        if (! inEnvironment.isObstacleSouthImmediate() && currentLocation.south != null)
            possibleMoves.add(currentLocation.south);
        if (! inEnvironment.isObstacleEastImmediate() && currentLocation.east != null)
            possibleMoves.add(currentLocation.east);
        if (! inEnvironment.isObstacleWestImmediate() && currentLocation.west != null)
            possibleMoves.add(currentLocation.west);
        if (possibleMoves.size() == 1){
            if (path.get(path.size()-1) == possibleMoves.get(0)){
                //dead end if the only move is back to where we came from
                currentLocation.updateNode(NODE_TYPE.DEAD_END);
                //previousLocation = path.get(path.size()-1);
            }
        }
    }

    private DIRECTION findBestPathToGoal(final ATTACK_MODE attackMode){
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
        while(!nodeQueue.isEmpty()){
            SearchNode searchNode = nodeQueue.remove();
            if (nodeIsGoal(searchNode, attackMode)){
                SearchNode currNode = searchNode;
                while (!currNode.parentNode.equals(parentSearchNode))
                    currNode = currNode.parentNode;
                return currNode.directionFromParent;
            }
            if (searchNode.thisNode.north != null)
                nodeQueue.add(new SearchNode(searchNode.thisNode.north, searchNode, 1+searchNode.cumulativeDistance, DIRECTION.NORTH));
            if (searchNode.thisNode.south != null)
                nodeQueue.add(new SearchNode(searchNode.thisNode.south, searchNode, 1+searchNode.cumulativeDistance, DIRECTION.SOUTH));
            if (searchNode.thisNode.east != null)
                nodeQueue.add(new SearchNode(searchNode.thisNode.east, searchNode, 1+searchNode.cumulativeDistance, DIRECTION.EAST));
            if (searchNode.thisNode.west != null)
                nodeQueue.add(new SearchNode(searchNode.thisNode.west, searchNode, 1+searchNode.cumulativeDistance, DIRECTION.WEST));
        }
        return DIRECTION.NOCHANGE;
    }

    private boolean nodeIsGoal(SearchNode node, ATTACK_MODE attackMode){
        return (attackMode.equals(ATTACK_MODE.SEEK_FLAG)) ?
                node.thisNode.nodeTypes.contains(NODE_TYPE.ENEMY_BASE) :
                node.thisNode.nodeTypes.contains(NODE_TYPE.FRIENDLY_BASE);
    }

    private class Node{
        Node west;
        Node east;
        Node north;
        Node south;
        int row,col;

        int enemyHeuristic,
            friendlyHeuristic; //heuristic for going to friendly base (cappping flag)

        ArrayList<NODE_TYPE> nodeTypes;

        public Node(){

        }

        public Node(Node west, Node east, Node north, Node south){
            this.west = west;
            this.east = east;
            this.north = north;
            this.south = south;
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
    }
}
