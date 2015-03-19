__author__ = 'Andrew Paettie'

import sys
from datastructure import DataStructure
from datastructure import State


def printUsage():
    print("Usage: python main.py <BFS|DFS|UCS|GS|A-star> <inputfile> [-cost]")
    print("   note that cost is optional and will only affect UCS and A-star")


def main(argv):
    if argv.__len__() == 3:
        print ("Starting search...")
    elif argv.__len__() == 4:
        print ("Starting search...")
    else:
        printUsage()
        sys.exit(1)
    searchType = argv[1]
    inputFile = argv[2]
    costFlag = False
    if argv.__len__()== 4:
        if searchType.upper()[0] == 'U' or searchType.upper()[0] == 'A':
            costFlag = True  # only use costFlag if it is applicable to the search (importing for output formatting)
    initialState = readFile(inputFile)
    doSearch(initialState.upper(), searchType.upper(), costFlag)


def readFile(filename):
    f = open(filename, 'r')
    for line in f:
        line = line.strip()
        if line.__len__() != 0:
            return line
    return None


def doSearch(state, mode, useCost):
    datastructure = DataStructure(mode, useCost)
    datastructure.add(State(state, None, None, useCost))
    visitedStates = []
    stepNum = 0

    while datastructure.states.__len__() != 0:
        node = datastructure.remove()
        while visitedStates.count(node.stateContent) != 0: # don't expand already expanded states
            node = datastructure.remove()
        move = "" if node.moveIndex is None else "move " + str(node.moveIndex)
        if useCost:  # need to display cost only of the cost flag is set and it is an applicable search
            print "Step {0}: {1} {2} (c={3})".format(stepNum, move, node.stateContent, node.g)
        else:
            print "Step {0}: {1} {2} ".format(stepNum, move, node.stateContent)
        if goalTest(node.stateContent):
            print "Goal found. Stopping search"
            # now lets display a path to the goal so we can check that the program is sane
            parent = node
            path = []
            while parent is not None:
                msg = "Move {0} -> {1}".format(parent.moveIndex, parent.stateContent) if parent.moveIndex is not None else str(parent.stateContent)
                path.append(msg)
                parent = parent.parentState
            path.reverse()
            print "Path to goal:"
            for msg in enumerate(path):  # enumerate so we can see step numbers
                print msg

            exit(0)
        datastructure.addList(node.getSuccessors())
        stepNum += 1
        visitedStates.append(node.stateContent)

    print "Datastructure empty and exhausted, but never found goal state :("
    exit(0)

# this should probably be in datastructure, but the program is working, so why refactor?
def goalTest(state):
    len = state.__len__()
    xIndex = state.find('X')
    if (xIndex == -1):
        print("ERROR: X not found")
        exit(1)
    # check only Bs on left of X
    for index in range(xIndex):
        if state [index] != 'B':
            return False
    # check only Ws on right of X
    for index in range(xIndex+1, len):
        if state [index] != 'W':
            return False
    return True



if __name__ == "__main__":
    main(sys.argv)
