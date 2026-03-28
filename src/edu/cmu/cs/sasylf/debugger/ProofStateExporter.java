package edu.cmu.cs.sasylf.debugger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.cmu.cs.sasylf.ast.Clause;
import edu.cmu.cs.sasylf.ast.Fact;

/**
 * ProofStateExporter exports proof state to JSON format for visualization. The
 * JSON can be consumed by external tools to create interactive proof
 * visualizations.
 */
public class ProofStateExporter {

    private final ProofState proofState;

    public ProofStateExporter(ProofState state) {
        this.proofState = state;
    }

    /**
     * Export the complete proof state to JSON.
     * 
     * @return JSON string representation
     */
    public String exportToJSON() {
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);

        out.println("{");
        out.println("  \"theorem\": \""
                + escapeJSON(proofState.getTheorem().getName()) + "\",");
        out.println("  \"kind\": \""
                + escapeJSON(proofState.getTheorem().getKind()) + "\",");

        // Export foralls
        out.println("  \"foralls\": [");
        List<Fact> foralls = proofState.getTheorem().getForalls();
        for (int i = 0; i < foralls.size(); i++) {
            Fact f = foralls.get(i);
            out.print("    \""
                    + escapeJSON(f.getName() + ": " + f.getElement().toString())
                    + "\"");
            if (i < foralls.size() - 1)
                out.print(",");
            out.println();
        }
        out.println("  ],");

        // Export exists
        Clause exists = proofState.getTheorem().getExists();
        out.println("  \"exists\": \"" + escapeJSON(exists.toString()) + "\",");

        // Export proof tree
        out.println("  \"proofTree\": {");
        exportProofTree(out, proofState.getProofTree());
        out.println("  }");

        out.println("}");

        return sw.toString();
    }

    /**
     * Export the proof tree structure to JSON.
     */
    private void exportProofTree(PrintWriter out, ProofTree tree) {
        out.println("    \"theoremName\": \""
                + escapeJSON(tree.getTheoremName()) + "\",");
        out.println("    \"totalSteps\": " + tree.getTotalSteps() + ",");
        out.println("    \"maxDepth\": " + tree.getMaxDepth() + ",");
        out.println("    \"roots\": [");

        List<DerivationStep> roots = tree.getRoots();
        for (int i = 0; i < roots.size(); i++) {
            exportStep(out, roots.get(i), 6);
            if (i < roots.size() - 1)
                out.println(",");
        }

        out.println();
        out.println("    ]");
    }

    /**
     * Export a single derivation step to JSON.
     */
    private void exportStep(PrintWriter out, DerivationStep step, int indent) {
        String ind = getIndent(indent);

        out.println(ind + "{");
        out.println(
                ind + "  \"name\": \"" + escapeJSON(step.getName()) + "\",");
        out.println(ind + "  \"judgment\": \""
                + escapeJSON(step.getJudgmentString()) + "\",");
        out.println(ind + "  \"depth\": " + step.getDepth() + ",");
        out.println(ind + "  \"completed\": " + step.isCompleted() + ",");

        // Export additional info
        Map<String, String> info = step.getAllInfo();
        if (!info.isEmpty()) {
            out.println(ind + "  \"info\": {");
            List<String> keys = new ArrayList<>(info.keySet());
            for (int i = 0; i < keys.size(); i++) {
                String key = keys.get(i);
                String value = info.get(key);
                out.print(ind + "    \"" + escapeJSON(key) + "\": \""
                        + escapeJSON(value) + "\"");
                if (i < keys.size() - 1)
                    out.print(",");
                out.println();
            }
            out.println(ind + "  },");
        }

        // Export children
        List<DerivationStep> children = step.getChildren();
        out.println(ind + "  \"children\": [");
        for (int i = 0; i < children.size(); i++) {
            exportStep(out, children.get(i), indent + 4);
            if (i < children.size() - 1)
                out.println(",");
        }
        out.println();
        out.println(ind + "  ]");

        out.print(ind + "}");
    }

    /**
     * Get indentation string.
     */
    private String getIndent(int spaces) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < spaces; i++) {
            sb.append(" ");
        }
        return sb.toString();
    }

    /**
     * Escape string for JSON.
     */
    private String escapeJSON(String s) {
        if (s == null)
            return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
