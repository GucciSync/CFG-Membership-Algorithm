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

    public void replaceChar(int index, char c) {
        expression = expression.substring(0, index) + c + expression.substring(index + 1);
    }

    public void replaceFirstPairWithChar(String pair, char c) {
        expression = expression.replaceFirst(pair, Character.toString(c));
    }

    public boolean satisfiesCNF() {
        char c = expression.charAt(0);
        if(expression.length() == 1 && (Character.isDigit(c) || !Character.isUpperCase(c)))
            return true;
        else
            return expression.length() == 2 && Character.isUpperCase(c) && Character.isUpperCase(expression.charAt(1));
    }

}
