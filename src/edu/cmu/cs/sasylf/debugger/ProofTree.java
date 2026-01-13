package edu.cmu.cs.sasylf.debugger;

import java.util.ArrayList;
import java.util.List;

/**
 * ProofTree represents the hierarchical structure of a proof. It maintains the
 * root derivation steps and provides methods for traversing and querying the
 * tree structure.
 * 
 * A proof may have multiple root steps (e.g., in a theorem with multiple forall
 * clauses that each contribute to the final result).
 */
public class ProofTree {

    private final String theoremName;
    private final List<DerivationStep> roots;

    /**
     * Create a new proof tree for the given theorem.
     * 
     * @param theoremName
     *                        the name of the theorem
     */
    public ProofTree(String theoremName) {
        this.theoremName = theoremName;
        this.roots = new ArrayList<>();
    }

    /**
     * Add a root-level derivation step.
     * 
     * @param step
     *                 the step to add as a root
     */
    public void addRootStep(DerivationStep step) {
        roots.add(step);
    }

    /**
     * Get all root steps in this proof tree.
     * 
     * @return list of root steps
     */
    public List<DerivationStep> getRoots() {
        return new ArrayList<>(roots);
    }

    /**
     * Get the theorem name.
     * 
     * @return the theorem name
     */
    public String getTheoremName() {
        return theoremName;
    }

    /**
     * Check if the tree is empty (has no roots).
     * 
     * @return true if empty
     */
    public boolean isEmpty() {
        return roots.isEmpty();
    }

    /**
     * Get total number of steps in the tree (roots + all descendants).
     * 
     * @return total step count
     */
    public int getTotalSteps() {
        int count = 0;
        for (DerivationStep root : roots) {
            count += countSteps(root);
        }
        return count;
    }

    /**
     * Count steps in a subtree rooted at the given step.
     * 
     * @param step
     *                 the root of the subtree
     * @return number of steps including root
     */
    private int countSteps(DerivationStep step) {
        int count = 1;
        for (DerivationStep child : step.getChildren()) {
            count += countSteps(child);
        }
        return count;
    }

    /**
     * Find a step by name, searching the entire tree.
     * 
     * @param name
     *                 the step name to find
     * @return the step, or null if not found
     */
    public DerivationStep findStep(String name) {
        for (DerivationStep root : roots) {
            DerivationStep found = findStepRecursive(root, name);
            if (found != null)
                return found;
        }
        return null;
    }

    /**
     * Recursively search for a step by name.
     * 
     * @param step
     *                 current step to search
     * @param name
     *                 the name to find
     * @return the step, or null if not found
     */
    private DerivationStep findStepRecursive(DerivationStep step, String name) {
        if (step.getName().equals(name)) {
            return step;
        }
        for (DerivationStep child : step.getChildren()) {
            DerivationStep found = findStepRecursive(child, name);
            if (found != null)
                return found;
        }
        return null;
    }

    /**
     * Clear all roots from this tree.
     */
    public void clear() {
        roots.clear();
    }

    /**
     * Get the maximum depth of the tree.
     * 
     * @return maximum depth
     */
    public int getMaxDepth() {
        int maxDepth = 0;
        for (DerivationStep root : roots) {
            maxDepth = Math.max(maxDepth, getSubtreeDepth(root));
        }
        return maxDepth;
    }

    /**
     * Get the depth of a subtree rooted at the given step.
     * 
     * @param step
     *                 the root of the subtree
     * @return depth of subtree
     */
    private int getSubtreeDepth(DerivationStep step) {
        if (step.isLeaf())
            return 1;
        int maxChildDepth = 0;
        for (DerivationStep child : step.getChildren()) {
            maxChildDepth = Math.max(maxChildDepth, getSubtreeDepth(child));
        }
        return 1 + maxChildDepth;
    }

    /**
     * Print a text representation of the tree for debugging.
     * 
     * @return string representation
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ProofTree[").append(theoremName).append("]\n");
        for (DerivationStep root : roots) {
            printSubtree(sb, root, 0);
        }
        return sb.toString();
    }

    /**
     * Print a subtree with indentation.
     * 
     * @param sb
     *                   string builder to append to
     * @param step
     *                   current step
     * @param indent
     *                   indentation level
     */
    private void printSubtree(StringBuilder sb, DerivationStep step,
            int indent) {
        // Add indentation
        for (int i = 0; i < indent; i++) {
            sb.append("  ");
        }
        // Add step info
        sb.append(step.toString()).append("\n");
        // Recursively print children
        for (DerivationStep child : step.getChildren()) {
            printSubtree(sb, child, indent + 1);
        }
    }
}
