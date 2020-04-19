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

    public void addProduction(String expression) {
        for(Production existing : productionList)
            if(existing.getExpression().equals(expression))
                return;
        productionList.add(new Production(this, expression));
    }

    public void removeProduction(Production production) {
        productionList.remove(production);
    }

    public char getSymbol() {
        return symbol;
    }

    public List<Production> getProductionList() {
        return productionList;
    }

    public String productionsToString() {
        StringBuilder result = new StringBuilder();
        for(Production production : productionList)
            result.append(production.getExpression()).append("|");
        return result.substring(0, result.length() - 1);
    }

}
