package main;

import java.util.*;
import java.util.function.Predicate;

public class CFG {

    private char startSymbol;

    private char lastSymbol;

    private List<Character> terminals;

    private LinkedHashMap<Character, NonTerminal> nonTerminals;

    public CFG() {
        startSymbol = 'S';
        lastSymbol = 'A';
        terminals = new ArrayList<>();
        nonTerminals = new LinkedHashMap<>();
    }

    public void addTerminal(char symbol) {
        if(!terminals.contains(symbol))
            terminals.add(symbol);
    }

    public NonTerminal addNonTerminal(char symbol) {
        if(nonTerminals.containsKey(symbol))
            return nonTerminals.get(symbol);
        if(symbol != startSymbol && symbol > lastSymbol)
            lastSymbol = symbol;
        NonTerminal nonTerminal = new NonTerminal(symbol);
        nonTerminals.put(symbol, nonTerminal);
        return nonTerminal;
    }

    public void convertToCNF(char newStartSymbol) {
        boolean newStart = false;
        for(NonTerminal nonterminal : nonTerminals.values())
            for(Production production : nonterminal.getProductionList())
                if(production.getExpression().contains(Character.toString(startSymbol)))
                    newStart = true;
        if(newStart) {
            NonTerminal nonTerminal = addNonTerminal(newStartSymbol);
            nonTerminal.addProduction(Character.toString(startSymbol));
            startSymbol = newStartSymbol;
        }
        simplify();
    }

    public void simplify() {
        /* Lambda removal */
        List<NonTerminal> nonTerminalsWithLambda = getNonTerminalsContaining(Main.LAMBDA);
        for(NonTerminal n : nonTerminalsWithLambda)
            for(Production production : n.getProductionList())
                if(production.getExpression().contains(Character.toString(Main.LAMBDA)))
                    n.removeProduction(production);
        while(true) { // find all nonterminal "paths" to lambda productions
            List<NonTerminal> newNullables = new ArrayList<>();
            for(NonTerminal nonTerminal : nonTerminalsWithLambda) {
                List<NonTerminal> oneLevelUp = getNonTerminalsContaining(nonTerminal.getSymbol());
                for(NonTerminal n : oneLevelUp)
                    if(!newNullables.contains(n) && !nonTerminalsWithLambda.contains(n) && n.getSymbol() != startSymbol) // TODO: add lambda to S where appropriate
                        newNullables.add(n);
            }
            if(newNullables.size() == 0)
                break;
            nonTerminalsWithLambda.addAll(newNullables);
        }
        for(NonTerminal n : nonTerminalsWithLambda) { // do for all nonTerminals with lambda
            List<Production> relevantProductions = getProductionsContaining(n.getSymbol());
            for(Production production : relevantProductions) { // do for each production containing the nonterminal n
                if(production.getExpression().length() == 1) // ignore unit productions
                    continue;
                for(int i = 0; i < production.getExpression().length(); i++)
                    if(production.getExpression().charAt(i) == n.getSymbol())
                        production.getNonTerminal().addProduction(production.getExpression().substring(0, i) + production.getExpression().substring(i + 1));
            }
        }

        /* Unit production removal */
        List<Production> unitProductions = getUnitProductions();
        while(unitProductions.size() > 0) {
            for(Production unitProduction : unitProductions) {
                NonTerminal unitProductionSymbol = nonTerminals.get(unitProduction.getExpression().charAt(0));
                for(Production production : unitProductionSymbol.getProductionList()) // add all productions from the unit production's non-terminal
                   unitProduction.getNonTerminal().addProduction(production.getExpression()); // addProduction method takes care of any duplicates
                unitProduction.getNonTerminal().removeProduction(unitProduction);
            }
            unitProductions = getUnitProductions(); // update list each time because unit productions might be moved around a few times before they're completely gone
        }

        /* Useless production removal */
        Set<Character> containsTerminal = getTerminatingNonTerminals();
        Iterator<Map.Entry<Character, NonTerminal>> iter = nonTerminals.entrySet().iterator();
        while(iter.hasNext()) {
            Map.Entry<Character, NonTerminal> entry = iter.next();
            if (containsTerminal.contains(entry.getValue().getSymbol()))
                continue;
            List<Production> toBeRemoved = getProductionsContaining(entry.getValue().getSymbol());
            toBeRemoved.forEach(p -> p.getNonTerminal().removeProduction(p)); // remove all occurrences of productions that contain this non-terminal's symbol
            iter.remove();
        }
        Queue<Character> next = new LinkedList<>();
        Set<Character> visited = new HashSet<>();
        next.add(startSymbol);
        while(next.size() > 0) { // determine which non-terminals can be reached from the start symbol
            NonTerminal n = nonTerminals.get(next.poll());
            for(Production p : n.getProductionList())
                for(char c : p.getExpression().toCharArray())
                    if(nonTerminals.containsKey(c) && !visited.contains(c) && !next.contains(c))
                        next.add(c);
            visited.add(n.getSymbol());
        }
        nonTerminals.keySet().removeIf(character -> !visited.contains(character)); // remove all non-terminals that couldn't be reached from the start symbol
    }

    public List<NonTerminal> getNonTerminalsContaining(char symbol) {
        List<NonTerminal> nTs = new ArrayList<>();
        for(NonTerminal nonTerminal : nonTerminals.values())
            for(Production production : nonTerminal.getProductionList())
                if(production.getExpression().contains(Character.toString(symbol)))
                    nTs.add(nonTerminal);
        return nTs;
    }

    public List<Production> getProductionsContaining(char symbol) {
        List<Production> productions = new ArrayList<>();
        for(NonTerminal nonTerminal : nonTerminals.values())
            for(Production production : nonTerminal.getProductionList())
                if(production.getExpression().contains(Character.toString(symbol)))
                    productions.add(production);
        return productions;
    }

    public List<Production> getUnitProductions() {
        List<Production> unitProductions = new ArrayList<>();
        for(NonTerminal n : nonTerminals.values())
            for(Production p : n.getProductionList())
                if(p.getExpression().length() == 1 && !terminals.contains(p.getExpression().charAt(0)))
                    unitProductions.add(p);
        return unitProductions;
    }

    public Set<Character> getTerminatingNonTerminals() {
        Set<Character> terminatingNonTerminals = new HashSet<>();
        while(true) { // find all non-terminals that terminate directly and use those that have been found so far to find those that indirectly terminate until there are no new results
            int before = terminatingNonTerminals.size();
            for (NonTerminal n : nonTerminals.values())
                for (Production p : n.getProductionList())
                    if (p.getExpression().contains(Character.toString(Main.LAMBDA)) || p.getExpression().matches("[a-z]*")) // non-terminals that directly terminate
                        terminatingNonTerminals.add(n.getSymbol());
                    else {
                        boolean terminates = true;
                        for(char c : p.getExpression().toCharArray())
                            if(nonTerminals.keySet().contains(c) && !terminatingNonTerminals.contains(c)) // at this point the non-terminal must contain at least one production is an expression that fully terminates
                                terminates = false;
                        if(terminates)
                            terminatingNonTerminals.add(n.getSymbol());
                    }
            if(before == terminatingNonTerminals.size())
                break;
        }
        return terminatingNonTerminals;
    }

    public char[] getSymbols() {
        char[] symbols = new char[terminals.size() + nonTerminals.size()];
        int count = 0;
        for(char c : terminals)
            symbols[count++] = c;
        for(char c : nonTerminals.keySet())
            symbols[count++] = c;
        return symbols;
    }

    @Override
    public String toString() {
        String result = "";
        for(NonTerminal nonTerminal : nonTerminals.values())
            result += nonTerminal.getSymbol() + " " + Main.ARROW + " " + nonTerminal.productionsToString() + "\n";
        return result;
    }

}
