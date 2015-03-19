__author__ = 'Andrew Paettie'


class Variable(object):
    def __init__(self, name, domain):
        self.name = name
        self.domain = domain
        self.assignment = None


class Constraint(object):
    def __init__(self, lhs, rhs, op):
        self.rhs = rhs
        self.lhs = lhs
        self.op = op