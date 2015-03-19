from collections import deque
from math import fabs

__author__ = 'Andrew Paettie'

'''
Place to put the datastructure specific stuff;  Sorry to name it datastructure, but trying to keep terminology same as
in slide for generic search algorithm
'''

class DataStructure(object):
    states = deque([])
    expandedStates = []
    mode = None

    '''
    Here mode can be the following values:
    DFS -> stack
    BFS -> queue
    UCS (withCost = False) -> priority queue sorted on g = num moves so far (to get to a specific node in question)
    UCS (withCost = True)  -> priority queue sorted on g = how many tiles are jumped to make move
    GS                     -> priority queue sorted on h = num tiles out of place
    A-star                 -> priority queue sorted on g + h (g and h are as defined above)
    '''
    def __init__(self, mode, withCost):
        if mode[0] == 'D':
            self.mode = 'D'
        elif mode[0] == 'B':
            self.mode = 'B'
        elif mode[0] == 'U' and not withCost:
            self.mode = 'U'
        elif mode[0] == 'U' and withCost:
            self.mode = 'UC'
        elif mode[0] == 'G':
            self.mode = 'G'
        elif mode[0] == 'A' and not withCost:
            self.mode = 'A'
        elif mode[0] == 'A' and withCost:
            self.mode = 'AC'
        else:
            print("ERROR: Invalid mode specified")
            exit(1)

    def add(self, state):
        self.states.append(state)
        if self.mode == "U" or self.mode == "UC" or self.mode == "G" or self.mode == "A" or self.mode == "AC":
            self.states = deque(sorted(list(self.states), cmp=self.stateComparator(), reverse=False)) # sort states based on metric defined in stateComparator

    def addList(self, stateList):
        for state in stateList:
            self.states.append(state)
        if self.mode == "U" or self.mode == "UC" or self.mode == "G" or self.mode == "A" or self.mode == "AC":
            self.states = deque(sorted(list(self.states), cmp=self.stateComparator(), reverse=False))

    def remove(self):
        if self.mode == "D":
            return self.states.pop()
        else:
            return self.states.popleft()

    def stateComparator(self):
        def compare(x, y):
            xMetric = None
            yMetric = None
            if self.mode == "UC" or self.mode == "U":
                xMetric = x.g  # the appropriate g will be assigned in State's constructor
                yMetric = y.g
            elif self.mode == "G":
                xMetric = x.h
                yMetric = y.h
            elif self.mode == "A" or self.mode == "AC":
                xMetric = x.g + x.h
                yMetric = y.g + y.h

            if xMetric < yMetric:
                return -1
            elif yMetric < xMetric:
                return 1
            else:
                return 0
        return compare


class State(object):
    stateContent = None
    parentState = None
    moveIndex = None  # what was moved to get to this state
    g = None
    h = None
    useCost = False

    def __init__(self, state, parent, moveIndex, useCost):
        self.stateContent = state
        self.parentState = parent
        self.moveIndex = moveIndex
        self.h = self.getH()
        if useCost is None or useCost is False:
            self.g = self.getG()
            self.useCost = False
        else:
            parentG = 0 if self.parentState is None else self.parentState.g
            self.g = self.getGWithCostFlag() + parentG  # add parent's g to this or this metric is useless because its not cumulative
            self.useCost = True

    # g = number of moves to get to this state
    def getG(self):
        if self.parentState is None:
            return 0
        numMoves = 0
        parent = self
        while parent is not None:
            parent = parent.parentState
            if parent is not None:
                numMoves += 1
        return numMoves

    # g = number tiles skipped over to move from the parent state to this state
    def getGWithCostFlag(self):
        if self.parentState is None:
            return 0
        parentX = self.parentState.stateContent.find('X')
        return fabs(parentX - self.moveIndex)

    # h = num tiles out of place
    def getH(self):
        outOfPlace = 0
        len = self.stateContent.__len__()
        xIndex = self.stateContent.find('X')
        if (xIndex == -1):
            print("ERROR: X not found in getH()")
            exit(1)
        # check only Bs on left of X
        for index in range(xIndex):
            if self.stateContent[index] != 'B':
                outOfPlace += 1
        # check only Ws on right of X
        for index in range(xIndex+1, len):
            if self.stateContent[index] != 'X':
                outOfPlace += 1
        return outOfPlace

    # returns a list of States which are successors to this function
    def getSuccessors(self):
        s = []
        xIndex = self.stateContent.find('X')
        if (xIndex == -1):
            print("ERROR: X not found in getSuccessors()")
            exit(1)
        for index in range(self.stateContent.__len__()):
            if (index != xIndex):
                newState = list(self.stateContent)
                newState[xIndex] = newState[index]
                newState[index] = 'X'
                s.append(State(''.join(newState), self, index, self.useCost))
        return s