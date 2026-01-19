package edu.cmu.cs.sasylf.debugger;

import java.util.HashMap;
import java.util.Map;

import edu.cmu.cs.sasylf.ast.Fact;
import edu.cmu.cs.sasylf.ast.NonTerminal;
import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;

/**
 * ContextSnapshot captures the state of the proof context at a particular point
 * in the derivation. It stores immutable snapshots of: - Current substitution -
 * Current goal - Assumed context (Gamma) - Available derivations
 * 
 * This class is immutable once created to ensure consistent snapshots.
 */
public class ContextSnapshot {

    private Substitution substitution;
    private Term goal;
    private NonTerminal assumedContext;
    private Map<String, Fact> availableDerivations;

    /**
     * Create an empty context snapshot. Use setter methods to populate it
     * during construction.
     */
    public ContextSnapshot() {
        this.substitution = null;
        this.goal = null;
        this.assumedContext = null;
        this.availableDerivations = new HashMap<>();
    }

    /**
     * Set the substitution for this snapshot. Should only be called during
     * construction.
     * 
     * @param sub
     *                the substitution
     */
    void setSubstitution(Substitution sub) {
        // Create a copy to ensure immutability
        this.substitution = new Substitution(sub);
    }

    /**
     * Set the current goal for this snapshot. Should only be called during
     * construction.
     * 
     * @param g
     *              the goal term
     */
    void setGoal(Term g) {
        this.goal = g;
    }

    /**
     * Set the assumed context for this snapshot. Should only be called during
     * construction.
     * 
     * @param ctx
     *                the assumed context
     */
    void setAssumedContext(NonTerminal ctx) {
        this.assumedContext = ctx;
    }

    /**
     * Set the available derivations for this snapshot. Should only be called
     * during construction.
     * 
     * @param derivs
     *                   map of available derivations
     */
    void setAvailableDerivations(Map<String, Fact> derivs) {
        this.availableDerivations = new HashMap<>(derivs);
    }

    /**
     * Get the substitution at this point in the proof.
     * 
     * @return the substitution, or null if not set
     */
    public Substitution getSubstitution() {
        return substitution;
    }

    /**
     * Get the current goal at this point in the proof.
     * 
     * @return the goal term, or null if not set
     */
    public Term getGoal() {
        return goal;
    }

    /**
     * Get the assumed context (Gamma) at this point.
     * 
     * @return the assumed context, or null if not set
     */
    public NonTerminal getAssumedContext() {
        return assumedContext;
    }

    /**
     * Get the derivations available at this point in the proof.
     * 
     * @return map of derivation names to their facts
     */
    public Map<String, Fact> getAvailableDerivations() {
        return new HashMap<>(availableDerivations);
    }

    /**
     * Check if a derivation with the given name is available.
     * 
     * @param name
     *                 the derivation name
     * @return true if available
     */
    public boolean hasDerivation(String name) {
        return availableDerivations.containsKey(name);
    }

    /**
     * Get a derivation by name.
     * 
     * @param name
     *                 the derivation name
     * @return the fact, or null if not found
     */
    public Fact getDerivation(String name) {
        return availableDerivations.get(name);
    }

    /**
     * Get a summary string of this context.
     * 
     * @return summary string
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Context(");
        if (assumedContext != null) {
            sb.append("Gamma=").append(assumedContext).append(", ");
        }
        if (goal != null) {
            sb.append("goal=").append(goal).append(", ");
        }
        sb.append("derivations=").append(availableDerivations.size());
        sb.append(")");
        return sb.toString();
    }
}
