package edu.cmu.cs.sasylf.util;

import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Marker;
import edu.cmu.cs.sasylf.util.VSDocument;
import edu.cmu.cs.sasylf.util.VSFindReplaceDocumentAdapter;
import edu.cmu.cs.sasylf.util.VSMarker;
import edu.cmu.cs.sasylf.util.VSRegion;
import java.util.HashMap;

/**
 * The makeQuickfix method implements the method for quickfixes used in
 * Eclipse, but it replaced the Eclipse classes with custom made classes that
 * mirror the Eclipse ones.
 */
public class Quickfix {
	private HashMap<String, String> getCategoryAndLexicalInfo(String ruleName) {
		HashMap<String, String> categoryAndLexicalInfo =
				new HashMap<String, String>();

		return categoryAndLexicalInfo;
	}

	public HashMap<String, Object> makeQuickfix(VSDocument doc, VSMarker marker) {
		String fixInfo;
		int line;
		VSRegion lineInfo = null;
		String lineText = "";
		Errors markerType = (Errors)marker.getAttribute(Marker.SASYLF_ERROR_TYPE);
		String nl = doc.getLineDelimiter();

		fixInfo = (String)marker.getAttribute(Marker.SASYLF_ERROR_INFO);
		line = (int)marker.getAttribute(Marker.LINE_NUMBER, 0);
		if (line > 0) {
			lineInfo = doc.getLineInformation(line - 1);
			lineText = doc.get(lineInfo.getOffset(), lineInfo.getLength());
		}

		if (markerType == null || fixInfo == null || line == 0) return null;

		String[] split = fixInfo.split("\r\n|\r|\n", -1);

		String lineIndent;
		{
			int i;
			for (i = 0; i < lineText.length(); ++i) {
				int ch = lineText.charAt(i);
				if (ch == ' ' || ch == '\t') continue;
				break;
			}
			lineIndent = lineText.substring(0, i);
		}

		int indentAmount =
				doc.getLines()[line - 1].length() -
				doc.getLines()[line - 1].replaceAll("^\\s+", "").length();
		String indent = "    ";

		if (indentAmount >= 0 && indentAmount <= 8) {
			indent = "        ".substring(0, indentAmount);
		}

		String extraIndent = "";
		int ind = doc.getBody().substring(lineInfo.getOffset()).indexOf(split[0]);
		VSRegion old;

		if (ind == -1) {
			old = null;
		} else {
			old = new VSRegion(ind + lineInfo.getOffset(), split[0].length());
		}
		// VSRegion old = new VSFindReplaceDocumentAdapter(doc).find(
		// lineInfo.getOffset(), split[0], true, true, false, false);

		if (old == null) {
			if (split[0].equals(lineText)) {
				old = new VSRegion(lineInfo.getOffset(), lineText.length());
			}
		}

		String newText;
		HashMap<String, Object> res;

		switch (markerType) {
		default:
			break;
		// case MISSING_CASE:
		//   int newCursor;
		//   StringBuilder sb = new StringBuilder();
		//   if (fixInfo.indexOf("\n\n") == -1) { // syntax case
		//     int n = split.length - 1;
		//     for (int i = 0; i < n; ++i) {
		//       sb.append(lineIndent);
		//       sb.append(indent);
		//       sb.append("case ");
		//       sb.append(split[i]);
		//       sb.append(" is");
		//       sb.append(nl);
		//       sb.append(lineIndent);
		//       sb.append(indent);
		//       sb.append(indent);
		//       sb.append("proof by unproved");
		//       sb.append(nl);
		//       sb.append(lineIndent);
		//       sb.append(indent);
		//       sb.append("end case");
		//       sb.append(nl);
		//       sb.append(nl);
		//     }
		//     newCursor = lineIndent.length() + indentAmount + 5;
		//   } else {
		//     newCursor = -1;
		//     boolean startCase = true;
		//     int n = split.length - 1; // extra line at end
		//     for (int i = 0; i < n; ++i) {
		//       if (startCase) {
		//         sb.append(lineIndent);
		//         sb.append(indent);
		//         sb.append("case rule");
		//         sb.append(nl);
		//         startCase = false;
		//       }
		//       if (split[i].length() == 0) {
		//         sb.append(lineIndent);
		//         sb.append(indent);
		//         sb.append("is");
		//         sb.append(nl);
		//         sb.append(lineIndent);
		//         sb.append(indent);
		//         sb.append(indent);
		//         sb.append("proof by unproved");
		//         sb.append(nl);
		//         sb.append(lineIndent);
		//         sb.append(indent);
		//         sb.append("end case");
		//         sb.append(nl);
		//         sb.append(nl);
		//         startCase = true;
		//         continue;
		//       }
		//       if (newCursor == -1)
		//         newCursor = sb.length();
		//       sb.append(lineIndent);
		//       sb.append(indent);
		//       sb.append(indent);
		//       if (split[i].startsWith("---")) {
		//         String ruleName = split[i].split(" ")[1];
		//         if (proofEditor != null) {
		//           ProofElement pe =
		//               proofEditor.getProofOutline().findProofElementByName(
		//                   ruleName);
		//           if (pe != null && pe.getCategory().equals("Rule")) {
		//             String bar = pe.getLexicalInfo();
		//             if (bar.length() >= 3) {
		//               String prefix = bar.substring(0, 3);
		//               split[i] = prefix + prefix + bar + " " + ruleName;
		//             }
		//           }
		//         }
		//       } else {
		//         sb.append("_: ");
		//       }
		//       sb.append(split[i]);
		//       sb.append(nl);
		//     }
		//   }
		//   if (lineText.contains("by contradiction on") &&
		//       !lineText.contains(
		//           "by case analysis on")) { // XXX: Could be confused by a
		//           comment
		//     IRegion reg = doc.getLineInformation(line - 1); // lines are
		//     zero-based int lo = lineText.indexOf("contradiction"); String[] parts
		//     = lineText.split("\\s+"); int l = parts.length;
		//     // try to avoid dangerous changes
		//     if (l > 3 && parts[l - 2].equals("on")) {
		//       String derivName = parts[l - 1];
		//       newText = "case analysis on " + derivName + ":" + nl + sb +
		//                 lineIndent + "end case analysis";
		//       int offset = reg.getOffset() + lo;
		//       // System.out.println("Converting starting at column " + lo + "
		//       using
		//       // '" + newText + "'");
		//       proposals.add(new MyCompletionProposal(
		//           res, newText, offset, reg.getLength() - lo, newCursor, null,
		//           "convert to case analysis with missing case(s)", null,
		//           fixInfo));
		//     }
		//   } else {
		//     newText = sb.toString();
		//     proposals.add(new MyCompletionProposal(
		//         res, newText, doc.getLineOffset(line), 0, newCursor, null,
		//         "insert missing case(s)", null, fixInfo));
		//   }
		//   break;

		// case ABSTRACT_NOT_PERMITTED_HERE:
		// case ILLEGAL_ASSUMES:
		// case EXTRANEOUS_ASSUMES:
		//   if (old != null) {
		//     proposals.add(new MyCompletionProposal(
		//         res, "", old.getOffset(), old.getLength(), 0, null,
		//         "remove '" + split[0] + "'", null, null));
		//   }
		//   // fall through -- don't know why
		case RULE_NOT_THEOREM:
		case THEOREM_NOT_RULE:
		case THEOREM_KIND_WRONG:
		case THEOREM_KIND_MISSING:
		case INDUCTION_REPEAT:
		case WRONG_END:
		case WRONG_MODULE_NAME:
		case PARTIAL_CASE_ANALYSIS:
			if (old != null) {
				res = new HashMap<>();
				if (split.length > 1 && split[1].length() > 0) {
					res.put("newText", split[1]);
					res.put("charStart", old.getOffset());
					res.put("charEnd", old.getOffset() + old.getLength());
					res.put("title",
									"replace '" + split[0] + "' with '" + split[1] + "'");
				}

				return res;
			}
			break;
		// case WRONG_PACKAGE:
		//   if (split[0].length() == 0) {
		//     newText = split[1];
		//     proposals.add(new MyCompletionProposal(
		//         res, newText + doc.getLineDelimiter(line),
		//         doc.getLineOffset(line), 0, newText.length(), null, "insert '" +
		//         newText + "'", null, null));
		//   }
		//   // System.out.println("fixInfo = " + fixInfo + ", old = " + old + ",
		//   res =
		//   // " + res + ", split = " + Arrays.toString(split));
		//   if (old != null && split.length > 1) {
		//     if (split[1].length() == 0) {
		//       proposals.add(new MyCompletionProposal(
		//           res, "", old.getOffset(), old.getLength(), 0, null,
		//           "remove '" + split[0] + "'", null, null));
		//     } else {
		//       proposals.add(new MyCompletionProposal(
		//           res, split[1], old.getOffset(), old.getLength(), 0, null,
		//           "replace '" + split[0] + "' with '" + split[1] + "'", null,
		//           null));
		//     }
		//   }
		//   break;
		case ASSUMED_ASSUMES:
			extraIndent = indent;
			// fall through
		case MISSING_ASSUMES:
			newText = lineIndent + extraIndent + fixInfo;

			res = new HashMap<>();
			res.put("newText", newText + doc.getLineDelimiter());
			res.put("charStart", doc.getLineOffset(line));
			res.put("charEnd", doc.getLineOffset(line) + newText.length());
			res.put("title", "insert '" + fixInfo + "'");

			return res;
		case OTHER_JUSTIFIED:
			if (lineInfo != null) {
				newText = " " + split[1];
				int holeStart = split[0].indexOf("...");
				String startPat = split[0].substring(0, holeStart);
				String endPat = split[0].substring(holeStart + 3);
				int findStart = lineText.indexOf(startPat);
				// System.out.println("indexOf(" + startPat + ") = " + findStart + " in
				// " + lineText);
				if (findStart < 0) break;
				int findEnd = lineText.indexOf(endPat, findStart);
				// System.out.println("indexOf(" + endPat + ") = " + findEnd + " in " +
				// lineText);
				if (findEnd < 0) break;
				int oldStart = findStart + startPat.length();
				String oldText = lineText.substring(oldStart, findEnd);

				res = new HashMap<>();
				res.put("newText", newText);
				res.put("charStart", lineInfo.getOffset() + oldStart);
				res.put("charEnd", lineInfo.getOffset() + oldStart + oldText.length());
				res.put("title", "replace '" + oldText + "' with '" + newText + "'");

				return res;
			}
			break;
		case RULE_CONCLUSION_CONTRADICTION:
			if (lineInfo != null) {
				int findBy = lineText.indexOf(" by ");
				if (findBy >= lineIndent.length()) {
					String oldText = lineText.substring(lineIndent.length(), findBy);

					res = new HashMap<>();
					res.put("newText", "_: contradiction");
					res.put("charStart", lineInfo.getOffset() + lineIndent.length());
					res.put("charEnd", lineInfo.getOffset() + lineIndent.length() +
																 oldText.length());
					res.put("title", "replace '" + oldText + "' with '_: contradiction'");

					return res;
				}
			}
			break;
		case DERIVATION_NOT_FOUND:
			int colon = split[0].indexOf(':');
			int useStart = (int)marker.getAttribute(Marker.CHAR_START, -1);
			int useEnd = (int)marker.getAttribute(Marker.CHAR_END, -1);
			String useName = doc.get(useStart, useEnd - useStart);
			String defName;

			if (colon >= 0) {
				defName = split[0].substring(0, colon);
			} else {
				defName = split[0];

				res = new HashMap<>();
				res.put("newText", defName);
				res.put("charStart", useStart);
				res.put("charEnd", useStart + useName.length());
				res.put("title", "replace '" + useName + " with '" + defName + "'");

				return res;
			}

			VSRegion prevLineInfo = lineInfo;
			// int diff = 0;
			// try {
			//   if (split.length > 1)
			//     diff = Integer.parseInt(split[1]);
			// } catch (RuntimeException ex) {
			//   // muffle array or number format
			// }
			// if (diff > 0)
			//   prevLineInfo = doc.getLineInformation(line - 1 - diff);
			// String prevLine =
			//     doc.get(prevLineInfo.getOffset(), prevLineInfo.getLength());
			// System.out.println(prevLine);
			// int prevStart;
			// for (prevStart = 0; prevStart < prevLine.length(); ++prevStart) {
			//   int ch = lineText.charAt(prevStart);
			//   if (ch == ' ' || ch == '\t')
			//     continue;
			//   break;
			// }
			// String prevIndent = lineText.substring(0, prevStart);
			newText = nl + indent + split[0] + " by unproved";

			String extra = "";

			if (!defName.equals(useName)) {
				if (useName.equals("_")) {
					extra = ", and replace '_' with '" + defName + "'";
				}
			}
			res = new HashMap<>();
			res.put("newText", newText);
			res.put("charStart", prevLineInfo.getOffset());
			res.put("charEnd", prevLineInfo.getOffset());
			res.put("title",
							"insert '" + split[0] + " by unproved' before this line" + extra);

			return res;
		}

		return null;
	}
}
