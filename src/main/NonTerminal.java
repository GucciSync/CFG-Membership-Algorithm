package main;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class NonTerminal {

    private char symbol;

    private List<Production> productionList;

    public NonTerminal(char symbol) {
        this.symbol = symbol;
        productionList = new CopyOnWriteArrayList<>();
    }

    public boolean addProduction(String expression) {
        for(Production existing : productionList)
            if(existing.getExpression().equals(expression))
                return false;
        return productionList.add(new Production(this, expression));
    }

    public boolean removeProduction(Production production) {
        return productionList.remove(production);
    }

    public char getSymbol() {
        return symbol;
    }

    public List<Production> getProductionList() {
        return productionList;
    }

    public String productionsToString() {
        String result = "";
        for(Production production : productionList)
            result += production.getExpression() + "|";
        return result.substring(0, result.length() - 1);
    }

}
