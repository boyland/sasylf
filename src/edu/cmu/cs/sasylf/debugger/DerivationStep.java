package edu.cmu.cs.sasylf.debugger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cmu.cs.sasylf.ast.Clause;

/**
 * DerivationStep represents a single step in a proof derivation. Each step
 * corresponds to one derivation in a SASyLF proof, capturing the judgment being
 * proved, the justification, and the context.
 * 
 * Steps form a tree structure where children represent sub-derivations (e.g.,
 * cases in an induction proof, premises in a rule application).
 */
public class DerivationStep {

    private final String name;
    private final Clause judgment;
    private final ContextSnapshot context;
    private final List<DerivationStep> children;
    private final Map<String, String> additionalInfo;
    private DerivationStep parent;
    private boolean completed;
    private long timestamp;

    /**
     * Create a new derivation step.
     * 
     * @param name
     *                     the derivation name (e.g., "d1", "d_ih")
     * @param judgment
     *                     the judgment being derived
     * @param context
     *                     snapshot of the proof context
     */
    public DerivationStep(String name, Clause judgment,
            ContextSnapshot context) {
        this.name = name;
        this.judgment = judgment;
        this.context = context;
        this.children = new ArrayList<>();
        this.additionalInfo = new HashMap<>();
        this.parent = null;
        this.completed = false;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Get the name of this derivation.
     * 
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the judgment being proved in this step.
     * 
     * @return the judgment clause
     */
    public Clause getJudgment() {
        return judgment;
    }

    /**
     * Get the judgment as a string for display.
     * 
     * @return string representation of judgment
     */
    public String getJudgmentString() {
        if (judgment == null)
            return "proof";
        return judgment.toString();
    }

    /**
     * Get the context snapshot for this step.
     * 
     * @return the context snapshot
     */
    public ContextSnapshot getContextSnapshot() {
        return context;
    }

    /**
     * Add a child step (sub-derivation).
     * 
     * @param child
     *                  the child step to add
     */
    public void addChild(DerivationStep child) {
        children.add(child);
    }

    /**
     * Get all child steps.
     * 
     * @return list of child steps
     */
    public List<DerivationStep> getChildren() {
        return children;
    }

    /**
     * Set the parent step.
     * 
     * @param parent
     *                   the parent step
     */
    public void setParent(DerivationStep parent) {
        this.parent = parent;
    }

    /**
     * Get the parent step.
     * 
     * @return the parent, or null if this is a root step
     */
    public DerivationStep getParent() {
        return parent;
    }

    /**
     * Check if this step is a root (has no parent).
     * 
     * @return true if this is a root step
     */
    public boolean isRoot() {
        return parent == null;
    }

    /**
     * Check if this step is a leaf (has no children).
     * 
     * @return true if this is a leaf step
     */
    public boolean isLeaf() {
        return children.isEmpty();
    }

    /**
     * Mark this step as completed.
     */
    public void markCompleted() {
        this.completed = true;
    }

    /**
     * Check if this step has been completed.
     * 
     * @return true if completed
     */
    public boolean isCompleted() {
        return completed;
    }

    /**
     * Add additional information to this step. This can be used to record
     * justification details, error messages, etc.
     * 
     * @param key
     *                  the information key
     * @param value
     *                  the information value
     */
    public void addInfo(String key, String value) {
        additionalInfo.put(key, value);
    }

    /**
     * Get additional information by key.
     * 
     * @param key
     *                the information key
     * @return the value, or null if not found
     */
    public String getInfo(String key) {
        return additionalInfo.get(key);
    }

    /**
     * Get all additional information.
     * 
     * @return map of all additional information
     */
    public Map<String, String> getAllInfo() {
        return new HashMap<>(additionalInfo);
    }

    /**
     * Get the timestamp when this step was created.
     * 
     * @return timestamp in milliseconds
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Get the depth of this step in the tree. Root steps have depth 0.
     * 
     * @return the depth
     */
    public int getDepth() {
        int depth = 0;
        DerivationStep current = this.parent;
        while (current != null) {
            depth++;
            current = current.getParent();
        }
        return depth;
    }

    /**
     * Convert this step to a string for debugging.
     * 
     * @return string representation
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(": ");
        if (judgment != null) {
            sb.append(judgment.toString());
        } else {
            sb.append("proof");
        }
        if (!children.isEmpty()) {
            sb.append(" (").append(children.size()).append(" children)");
        }
        return sb.toString();
    }
}
