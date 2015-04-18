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
        NORTH,
        SOUTH,
        EAST,
        WEST
    }

    public static int BOARD_SIZE = 10;
    public static int FLAG_ROW = (BOARD_SIZE / 2 + 1);

    public static final int AGENT_0 = 0;

    public static final int AGENT_1 = 1;

    public static int agentCount = 0;

    public int agentIdentifier;
    public static boolean initialMove = true;

    public DIRECTION friendlyInitialFlagDirection = null;
    public static DIRECTION friendlyBaseWestOrEast = null;

    public ArrayList<Node> path;

    public Node currentLocation;

    public Node[][] nodes = new Node[BOARD_SIZE][BOARD_SIZE];

    public Agent007(){
        path = new ArrayList<Node>();
        for (int row = 0; row < BOARD_SIZE; row++){//todo move this to doInitialSetup()
            for (int col = 0; col < BOARD_SIZE; col++) {
                nodes[row][col] = new Node();
            }
        }
        for (int row = 0; row < BOARD_SIZE; row++){
            for (int col = 0; col < BOARD_SIZE; col++){
                if (row == 0 && col == 0){
                    Node n = nodes[row][col];
                    n.top = null;
                    n.left = null;
                    n.right =  nodes[0][1];
                    n.bottom = nodes[1][0];
                    nodes[row][col] = n;
                }else if (row == 0 && col != BOARD_SIZE -1){
                    Node n = nodes[row][col];
                    n.top = null;
                    n.left = nodes[row][col-1];
                    n.right =  nodes[row][col+1];
                    n.bottom = nodes[row+1][col];
                    nodes[row][col] = n;
                }else if (row == 0 && col == BOARD_SIZE -1){
                    Node n = nodes[row][col];
                    n.top = null;
                    n.left = nodes[row][col-1];
                    n.right =  null;
                    n.bottom = nodes[row+1][col];
                    nodes[row][col] = n;
                }else if (row != 0 && row != BOARD_SIZE -1 && col == 0){
                    Node n = nodes[row][col];
                    n.top = nodes[row-1][col];
                    n.left = null;
                    n.right =  nodes[row][col+1];
                    n.bottom = nodes[row+1][col];
                    nodes[row][col] = n;
                }else if (row != 0 && row != BOARD_SIZE -1 && col != BOARD_SIZE-1){
                    Node n = nodes[row][col];
                    n.top = nodes[row-1][col];
                    n.left = nodes[row][col-1];
                    n.right =  nodes[row][col+1];
                    n.bottom = nodes[row+1][col];
                    nodes[row][col] = n;
                }else if (row != 0 && row != BOARD_SIZE -1 && col == BOARD_SIZE-1){
                    Node n = nodes[row][col];
                    n.top = nodes[row-1][col];
                    n.left = nodes[row][col-1];
                    n.right =  null;
                    n.bottom = nodes[row+1][col];
                    nodes[row][col] = n;
                }else if (row == BOARD_SIZE -1 && col == 0){
                    Node n = nodes[row][col];
                    n.top = nodes[row-1][col];
                    n.left = null;
                    n.right =  nodes[row][col+1];
                    n.bottom = null;
                    nodes[row][col] = n;
                }else if (row == BOARD_SIZE -1 && col != BOARD_SIZE-1 && col != 0){
                    Node n = nodes[row][col];
                    n.top = nodes[row-1][col];
                    n.left = nodes[row][col-1];
                    n.right =  nodes[row][col+1];
                    n.bottom = null;
                    nodes[row][col] = n;
                }else if (row == BOARD_SIZE -1 && col == BOARD_SIZE-1){
                    Node n = nodes[row][col];
                    n.top = nodes[row-1][col];
                    n.left = nodes[row][col-1];
                    n.right =  null;
                    n.bottom = null;
                    nodes[row][col] = n;
                }
            }
        }
    }

    @Override
    public int getMove( AgentEnvironment inEnvironment ){
        if (initialMove){
            doInitialSetup(inEnvironment);
        }
        if (friendlyInitialFlagDirection == null){
            friendlyInitialFlagDirection = inEnvironment.isFlagSouth(AgentEnvironment.OUR_TEAM, false) ? DIRECTION.SOUTH : DIRECTION.NORTH;
        }
        return AgentAction.DO_NOTHING;//todo
    }

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

        if (this.friendlyInitialFlagDirection.equals(DIRECTION.EAST)){
            this.nodes[(BOARD_SIZE / 2) + 1][BOARD_SIZE-1].nodeTypes.add(NODE_TYPE.FRIENDLY_BASE);
            this.nodes[(BOARD_SIZE / 2) + 1][0].nodeTypes.add(NODE_TYPE.ENEMY_BASE);
        }else{
            this.nodes[(BOARD_SIZE / 2) + 1][BOARD_SIZE-1].nodeTypes.add(NODE_TYPE.ENEMY_BASE);
            this.nodes[(BOARD_SIZE / 2) + 1][0].nodeTypes.add(NODE_TYPE.FRIENDLY_BASE);
        }

        for (int row = 0; row < BOARD_SIZE; row++){
            for (int col = 0; col < BOARD_SIZE; col++) {
                Node n = nodes[row][col];
                if (this.friendlyInitialFlagDirection.equals(DIRECTION.EAST)){
                    n.friendlyHeuristic = Math.abs(row - FLAG_ROW) + Math.abs(col - (BOARD_SIZE - 1));
                    n.enemyHeuristic = Math.abs(row - FLAG_ROW) + Math.abs(col - 0);
                }else{
                    n.enemyHeuristic = Math.abs(row - FLAG_ROW) + Math.abs(col - (BOARD_SIZE - 1));
                    n.friendlyHeuristic = Math.abs(row - FLAG_ROW) + Math.abs(col - 0);
                }
                nodes[row][col] = n;
            }
        }
        //todo maybe set player locations

    }


    private class Node{
        Node left;
        Node right;
        Node top;
        Node bottom;

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

        public boolean isMine(){
            for (NODE_TYPE n : nodeTypes)
                if (n.equals(NODE_TYPE.KNOWN_MINE))
                    return true;
            return false;
        }

        //todo ^^^ make these things ^^^
    }
}
