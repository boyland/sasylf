package edu.cmu.cs.sasylf.debugger;

import java.util.Stack;
import java.util.HashMap;
import java.util.Map;

import edu.cmu.cs.sasylf.ast.Clause;
import edu.cmu.cs.sasylf.ast.Context;
import edu.cmu.cs.sasylf.ast.Derivation;
import edu.cmu.cs.sasylf.ast.NonTerminal;
import edu.cmu.cs.sasylf.ast.Theorem;
import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;

/**
 * ProofState captures the complete state of a proof during type checking. It
 * maintains a stack of active derivations, records the context at each step,
 * and builds a hierarchical proof tree structure.
 * 
 * This class is designed to be non-intrusive - it can be enabled/disabled
 * without affecting normal SASyLF operation.
 */
public class ProofState {

    private final Theorem theorem;
    private final ProofTree tree;
    private final Stack<DerivationStep> executionStack;
    private final Map<String, DerivationStep> completedSteps;
    private boolean enabled;

    /**
     * Create a new proof state for the given theorem.
     * 
     * @param thm
     *                the theorem being proved
     */
    public ProofState(Theorem thm) {
        this.theorem = thm;
        this.tree = new ProofTree(thm.getName());
        this.executionStack = new Stack<>();
        this.completedSteps = new HashMap<>();
        this.enabled = false;
    }

    /**
     * Enable or disable proof state tracking. When disabled, all recording
     * operations become no-ops.
     * 
     * @param enable
     *                   true to enable tracking
     */
    public void setEnabled(boolean enable) {
        this.enabled = enable;
    }

    /**
     * Check if proof state tracking is currently enabled.
     * 
     * @return true if tracking is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Record the start of a derivation step. This should be called when
     * beginning to type check a derivation.
     * 
     * @param deriv
     *                  the derivation being checked
     * @param ctx
     *                  the current proof context
     */
    public void pushDerivation(Derivation deriv, Context ctx) {
        if (!enabled)
            return;

        String name = deriv.getName();
        Clause clause = deriv.getClause();
        DerivationStep step = new DerivationStep(name, clause,
                captureContext(ctx));

        // If there's a parent on the stack, add this as a child
        if (!executionStack.isEmpty()) {
            DerivationStep parent = executionStack.peek();
            parent.addChild(step);
            step.setParent(parent);
        } else {
            // This is a top-level derivation
            tree.addRootStep(step);
        }

        executionStack.push(step);
    }

    /**
     * Record the completion of a derivation step. This should be called after a
     * derivation has been successfully type checked.
     * 
     * @param deriv
     *                  the derivation that was completed
     */
    public void popDerivation(Derivation deriv) {
        if (!enabled)
            return;

        if (!executionStack.isEmpty()) {
            DerivationStep completed = executionStack.pop();
            completed.markCompleted();
            completedSteps.put(deriv.getName(), completed);
        }
    }

    /**
     * Record additional information about the current derivation. This can be
     * used to annotate steps with justification details.
     * 
     * @param key
     *                  the information key
     * @param value
     *                  the information value
     */
    public void recordInfo(String key, String value) {
        if (!enabled || executionStack.isEmpty())
            return;

        DerivationStep current = executionStack.peek();
        current.addInfo(key, value);
    }

    /**
     * Get the current execution context. Returns null if proof tracking is
     * disabled or no derivation is active.
     * 
     * @return the current context snapshot, or null
     */
    public ContextSnapshot getCurrentContext() {
        if (!enabled || executionStack.isEmpty())
            return null;
        return executionStack.peek().getContextSnapshot();
    }

    /**
     * Get the proof tree built during execution.
     * 
     * @return the proof tree
     */
    public ProofTree getProofTree() {
        return tree;
    }

    /**
     * Get the theorem being proved.
     * 
     * @return the theorem
     */
    public Theorem getTheorem() {
        return theorem;
    }

    /**
     * Get a completed derivation step by name.
     * 
     * @param name
     *                 the derivation name
     * @return the step, or null if not found
     */
    public DerivationStep getCompletedStep(String name) {
        return completedSteps.get(name);
    }

    /**
     * Capture a snapshot of the current context. This extracts relevant
     * information from the Context object without storing a reference to it.
     * 
     * @param ctx
     *                the context to snapshot
     * @return a context snapshot
     */
    private ContextSnapshot captureContext(Context ctx) {
        ContextSnapshot snapshot = new ContextSnapshot();

        // Capture current substitution
        if (ctx.currentSub != null) {
            snapshot.setSubstitution(ctx.currentSub);
        }

        // Capture current goal
        if (ctx.currentGoal != null) {
            snapshot.setGoal(ctx.currentGoal);
        }

        // Capture assumed context (Gamma)
        if (ctx.assumedContext != null) {
            snapshot.setAssumedContext(ctx.assumedContext);
        }

        // Capture available derivations
        if (ctx.derivationMap != null) {
            snapshot.setAvailableDerivations(new HashMap<>(ctx.derivationMap));
        }

        return snapshot;
    }

    /**
     * Reset the proof state, clearing all recorded information. This is useful
     * for processing multiple theorems.
     */
    public void reset() {
        executionStack.clear();
        completedSteps.clear();
        tree.clear();
    }
}
