import copy
import sys

__author__ = 'Andrew Paettie'

from constraints import Constraint, Variable

class solver(object):
    constraints = []
    variables = []
    consistencyEnforcing = "none"
    stepNumber = 1
    def __init__(self, variableFilename, constraintFilename, consistencyEnforcing):
        self.variables = self.readVariableFile(variableFilename)
        self.constraints = self.readConstraintFile(constraintFilename)
        self.consistencyEnforcing = consistencyEnforcing

    def solve(self):
        initialState = self.SearchState(self.variables, None)
        self.recursiveBacktracking(initialState)

    def recursiveBacktracking(self, currentState):
        currentState = copy.deepcopy(currentState)
        if self.allVarsAssigned(currentState):
            #currentState.isFailure=False
            self.printState(currentState)
            return currentState
        elif currentState.isFailure:
            self.printState(currentState)
        varIndexOfInterest = self.pickVariable(currentState)
        while self.pickValue(varIndexOfInterest, currentState) is not None:
            valueIndex = self.pickValue(varIndexOfInterest, currentState)
            if self.valueIsConsistent(varIndexOfInterest, valueIndex, currentState):
                assignmentValue = currentState.variables[varIndexOfInterest].domain[valueIndex]
                currentState.variables[varIndexOfInterest].assignment = assignmentValue
                oldDomain = currentState.variables[varIndexOfInterest].domain
                currentState.variables[varIndexOfInterest].domain = [assignmentValue]
                if self.consistencyEnforcing == "none":
                    newState = self.recursiveBacktracking(currentState)
                else:
                    newState = self.forwardChecking(currentState, varIndexOfInterest)
                    if not newState.isFailure:
                        newState = self.recursiveBacktracking(newState)
                    else:
                        self.printState(newState)
                    # if self.forwardCheckingSuccess(currentState, varIndexOfInterest):
                    #     newState = self.recursiveBacktracking(currentState)
                    # else:
                    #     newState = copy.deepcopy(currentState)
                    #     newState.isFailure = True
                if not newState.isFailure:
                    return newState
                currentState.variables[varIndexOfInterest].domain = oldDomain
                del currentState.variables[varIndexOfInterest].domain[valueIndex]
                currentState.variables[varIndexOfInterest].assignment = None
            else:
                newState = copy.deepcopy(currentState)
                newState.isFailure = True
                assignmentValue = newState.variables[varIndexOfInterest].domain[valueIndex]
                newState.variables[varIndexOfInterest].assignment = assignmentValue
                self.printState(newState)
                del currentState.variables[varIndexOfInterest].domain[valueIndex]
        currentState.isFailure = True
        return currentState


    def pickVariable(self, state):
        # find the var with the least remaining values
        remainingValues = []
        for var in state.variables:
            remainingValues.append(var.domain.__len__())
        min = sys.maxint
        for idx, val in enumerate(remainingValues):
            if val < min and state.variables[idx].assignment is None:
                min = val
        minVarIndexes = []
        for idx, val in enumerate(remainingValues):
            if val == min and state.variables[idx].assignment is None:
                minVarIndexes.append(idx)
        if minVarIndexes.__len__() == 1:
            return minVarIndexes[0]
        # tie breaker is most constraining variable (the most constraints on remaining variables)
        constraintCounts = [0 for _ in range(minVarIndexes.__len__())]
        for constraint in self.constraints:
            for minVarIndex, varIndex in enumerate(minVarIndexes):
                if constraint.lhs == varIndex:
                    if state.variables[constraint.rhs].assignment is None: # only look at unassigned variables
                        constraintCounts[minVarIndex] += 1
                elif constraint.rhs == varIndex:
                    if state.variables[constraint.lhs].assignment is None: # only look at unassigned variables
                        constraintCounts[minVarIndex] += 1
                #     if state.variables[constraint.rhs].domain.__len__() > 1:# this should be on remaining variables only
                #         constraintCounts[minVarIndex] += 1
                # elif constraint.rhs == varIndex:
                #     if state.variables[constraint.lhs].domain.__len__() > 1:
                #         constraintCounts[minVarIndex] += 1
        max = -sys.maxint
        for val in constraintCounts:
            if val > max:
                max = val
        maxConstraintIndexes = []
        for idx, val in enumerate(constraintCounts):
            if val == max:
                maxConstraintIndexes.append(idx)
        # variables are sorted in alphabetical order, so it is always safe to just return the first
        # one encountered, even if there is a tie here (alphabetical is final tie breaker)
        return minVarIndexes[maxConstraintIndexes[0]]
        # if maxConstraintIndexes.__len__() == 1:
        #     return minVarIndexes[maxConstraintIndexes[0]]
        # else:
        #     # final tie breaker is alphabetical
        #     varNames = []
        #     for idx in range(maxConstraintIndexes.__len__()):
        #         varNames.append(self.variables[minVarIndexes[maxConstraintIndexes[idx]]])
        #     varNames.sort()
        #     return self.findVarIndexFromName(varNames[0])

    # returns index of value
    def pickValue(self, variableIndex, state):
        # if no values left to pick, return none
        if state.variables[variableIndex].domain.__len__() == 0:
            return None

        # if only 1 value left to pick, just pick that one to avoid looping below
        if state.variables[variableIndex].domain.__len__() == 1:
            return 0

        # value picked by least constraining value (value which rules out least values)
        valueIndexToNumberValuesRuledOut = [0 for _ in state.variables[variableIndex].domain]
        for constraint in self.constraints:
            if constraint.lhs == variableIndex or constraint.rhs == variableIndex:
                for thisVarIdx, thisVarVal in enumerate(state.variables[variableIndex].domain):
                    if constraint.lhs == variableIndex and state.variables[constraint.rhs].assignment is None:
                        for otherVarVal in state.variables[constraint.rhs].domain:
                            if not self.evaluateConstraint(thisVarVal, otherVarVal, constraint):
                                valueIndexToNumberValuesRuledOut[thisVarIdx] += 1
                    elif constraint.rhs == variableIndex and state.variables[constraint.lhs].assignment is None:
                        for otherVarVal in state.variables[constraint.lhs].domain:
                            if not self.evaluateConstraint(otherVarVal, thisVarVal, constraint):
                                valueIndexToNumberValuesRuledOut[thisVarIdx] += 1
        # tie breaker by preferring small values
        min = sys.maxint
        for numRuledOut in valueIndexToNumberValuesRuledOut:
            if numRuledOut < min:
                min = numRuledOut
        bestValIndexes = []
        for idx, numRuledOut in enumerate(valueIndexToNumberValuesRuledOut):
            if numRuledOut == min:
                bestValIndexes.append(idx)
        # variable values are sorted in increasing order, so even if there is more than one
        # , can still just return the first one (since tie breaker is smaller values)
        return bestValIndexes[0]

    def valueIsConsistent(self, varIndex, valueIndex, state):
        for constraint in self.constraints:
            valid = True
            if constraint.lhs == varIndex:
                valid = self.evaluateConstraint(state.variables[varIndex].domain[valueIndex], state.variables[constraint.rhs].assignment, constraint)
            elif constraint.rhs == varIndex:
                valid = self.evaluateConstraint(state.variables[constraint.lhs].assignment, state.variables[varIndex].domain[valueIndex], constraint)
            if not valid:
                return False
        return True

    # lhs and rhs need to be actual values, not indices
    def evaluateConstraint(self, lhs, rhs, constraint):
        if lhs is None or rhs is None:
            return True
        if constraint.op == '<':
            return lhs < rhs
        if constraint.op == '>':
            return lhs > rhs
        if constraint.op == '!':
            return lhs != rhs
        if constraint.op == '=':
            return lhs == rhs

    def allVarsAssigned(self, currentState):
        for var in currentState.variables:
            if var.assignment is None:
                return False
        return True

    def printState(self, currState):
        msg = str(self.stepNumber)+". "
        for varIndex, var in enumerate(self.variables):
            if currState.variables[varIndex].assignment is not None:
                msg = msg + var.name + "=" + str(currState.variables[varIndex].assignment)
                if varIndex != self.variables.__len__()-1:
                    msg = msg + ", "
        msg = msg + "   " + ("failure" if currState.isFailure else "solution")
        print msg
        self.stepNumber += 1

    # returns true if forward checking leaves legal values for all variables, false otherwise
    # return state
    def forwardChecking(self, state, varIndex):
        tempState = copy.deepcopy(state)
        varsWhichChangedDomain = []
        for constraint in self.constraints:
            if varIndex == constraint.lhs:
                deletedIndexs = []
                for otherVarValIdx, otherVarVal in enumerate(tempState.variables[constraint.rhs].domain):
                    if not self.evaluateConstraint(tempState.variables[varIndex].assignment, otherVarVal, constraint):
                        deletedIndexs.append(otherVarValIdx)
                        if varsWhichChangedDomain.count(constraint.rhs) == 0:
                            varsWhichChangedDomain.append(constraint.rhs)
                if tempState.variables[constraint.rhs].domain.__len__() == deletedIndexs.__len__():
                    tempState.isFailure = True
                    return tempState
                    #return False
                for index in reversed(deletedIndexs):
                    del tempState.variables[constraint.rhs].domain[index]
            elif varIndex == constraint.rhs:
                deletedIndexs = []
                for otherVarValIdx, otherVarVal in enumerate(tempState.variables[constraint.lhs].domain):
                    if not self.evaluateConstraint(otherVarVal, tempState.variables[varIndex].assignment, constraint):
                        deletedIndexs.append(otherVarValIdx)
                        if varsWhichChangedDomain.count(constraint.lhs) == 0:
                            varsWhichChangedDomain.append(constraint.lhs)
                if tempState.variables[constraint.lhs].domain.__len__() == deletedIndexs.__len__():
                    tempState.isFailure = True
                    return tempState
                    #return False
                for index in reversed(deletedIndexs):
                    del tempState.variables[constraint.lhs].domain[index]
        # below is an implementation of constraint propagation.  This is not being used in this project
        # while varsWhichChangedDomain.__len__() > 0:
        #     varIndex = varsWhichChangedDomain[0]
        #     for constraint in self.constraints:
        #         if varIndex == constraint.lhs:
        #             for otherVarValIdx, otherVarVal in enumerate(tempState.variables[constraint.rhs].domain):
        #                 if not self.evaluateConstraint(tempState.variables[varIndex].assignment, otherVarVal, constraint):
        #                     del tempState.variables[constraint.rhs].domain[otherVarValIdx]
        #                     if varsWhichChangedDomain.count(constraint.rhs) == 0:
        #                         varsWhichChangedDomain.append(constraint.rhs)
        #             if tempState.variables[constraint.rhs].domain.__len__() == 0:
        #                 return False
        #         elif varIndex == constraint.rhs:
        #             for otherVarValIdx, otherVarVal in enumerate(tempState.variables[constraint.lhs].domain):
        #                 if not self.evaluateConstraint(otherVarVal, tempState.variables[varIndex].assignment, constraint):
        #                     del tempState.variables[constraint.lhs].domain[otherVarValIdx]
        #                     if varsWhichChangedDomain.count(constraint.lhs) == 0:
        #                         varsWhichChangedDomain.append(constraint.lhs)
        #             if tempState.variables[constraint.lhs].domain.__len__() == 0:
        #                 return False
        #     del varsWhichChangedDomain[0]
        tempState.isFailure = False
        return tempState
        #return True

    '''
        File reading stuff
    '''
    def readVariableFile(self, filename):
        f = open(filename, 'r')
        variableList = []
        for line in f:
            name, sep, domain = line.partition(':')
            if domain.__len__() == 0:
                print "Warning: did not find ':' in line'"+line+"'"
                continue
            else:
                domain = domain.strip()
                domainList = domain.split(' ')
                numericalDomainList = []
                for d in domainList:
                    numericalDomainList.append(int(d))
                numericalDomainList.sort() # arrange values into increasing order
                name = name.strip().upper()
                variableList.append(Variable(name, numericalDomainList))
        return sorted(variableList, key=lambda var:var.name)

    def readConstraintFile(self, filename):
        f = open(filename, 'r')
        constraintList = []
        for line in f:
            if line.find('<') != -1:
                lhs, rhs = self.getLhsRhs(line.split('<'))
                constraintList.append(Constraint(lhs, rhs, '<'))
            elif line.find('>') != -1:
                lhs, rhs = self.getLhsRhs(line.split('>'))
                constraintList.append(Constraint(lhs, rhs, '>'))
            elif line.find('!') != -1:
                lhs, rhs = self.getLhsRhs(line.split('!'))
                constraintList.append(Constraint(lhs, rhs, '!'))
            elif line.find('=') != -1:
                lhs, rhs = self.getLhsRhs(line.split('='))
                constraintList.append(Constraint(lhs, rhs, '='))
            else:
                print "Encountered unknown operator in line: '"+line+"'"
        return constraintList

    def getLhsRhs(self, lineSplitOnOp):
        lhs = self.findVarIndexFromName(lineSplitOnOp[0].strip().upper())
        rhs = self.findVarIndexFromName(lineSplitOnOp[1].strip().upper())
        return lhs, rhs

    def findVarIndexFromName(self, name):
        for index, var in enumerate(self.variables):
            if var.name == name:
                return index
        return -1

    '''
        class to hold stuff for search state
    '''
    class SearchState(object):
        def __init__(self, variables, parentState, isFailure=False):
            self.variables = variables
            self.parentState = parentState
            self.isFailure=isFailure
