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

    /* Replaces the expression with a string with the character at the given index being replaced with another given character */
    public void replaceChar(int index, char c) {
        expression = expression.substring(0, index) + c + expression.substring(index + 1);
    }

    /* Replaces the expression with a string with the first occurrence of the given pair being replaced with a given character */
    public void replaceFirstPairWithChar(String pair, char c) {
        expression = expression.replaceFirst(pair, Character.toString(c));
    }

    /* Check if this production's expression is in CNF form */
    public boolean satisfiesCNF() {
        char c = expression.charAt(0);
        if(expression.length() == 1 && (Character.isDigit(c) || !Character.isUpperCase(c))) // expression with a single terminal character
            return true;
        else
            return expression.length() == 2 && Character.isUpperCase(c) && Character.isUpperCase(expression.charAt(1)); // expression with 2 non-terminal characters
    }

}
