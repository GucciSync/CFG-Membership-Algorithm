package main;

public class Production {

    private NonTerminal nonTerminal;

    private String expression;

    public Production(NonTerminal nonTerminal, String expression) {
        this.nonTerminal = nonTerminal;
        this.expression = expression;
    }

    public NonTerminal getNonTerminal() {
        return nonTerminal;
    }

    public String getExpression() {
        return expression;
    }

}
