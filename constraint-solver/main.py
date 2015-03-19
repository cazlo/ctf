import sys
__author__ = 'Andrew Paettie'

from constraintSolver import solver

def printUsage():
    print("Usage: python main.py <variable file> <constraint file> <consistency enforcing>")
    print("      where: the variable file consists of variables and their domains")
    print("             the constraint file lists constraints using =, !, >, or < operators")
    print("             the consistency enforcing is either 'none' (for backtracking)")
    print("                              or 'fc' for forward checking")


def main(argv):
    if argv.__len__() == 4:
        print ("Starting search...")
    else:
        printUsage()
        sys.exit(1)
    if argv[3].lower() == "none" or argv[3].lower() == "fc":
        constraintSolver = solver(argv[1], argv[2], argv[3].lower())
    else:
        print "Invalid value specified for consistency enforcing.  Legal values are 'none' or 'fc'"
        print "Assumming 'none'"
        constraintSolver = solver(argv[1], argv[2], "none")
    constraintSolver.solve()


if __name__ == "__main__":
    main(sys.argv)