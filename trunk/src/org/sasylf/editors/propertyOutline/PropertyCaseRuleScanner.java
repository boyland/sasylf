package org.sasylf.editors.propertyOutline;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;

public class PropertyCaseRuleScanner {
	private IDocument document;
	private List<PropertyElement> pList = new ArrayList<PropertyElement>();
	private Stack<PropertyElement> caseStack = new Stack<PropertyElement>();
	
	private static Pattern packPattern = Pattern.compile("package\\s+[\\w.]+;");
	private static Pattern termPattern = Pattern.compile("terminals");
	private static Pattern theoPattern = Pattern.compile("theorem\\s+[\\w-]+");
	private static Pattern endtPattern = Pattern.compile("end theorem\\s*$");
	private static Pattern syntPattern = Pattern.compile("syntax");
	private static Pattern judgPattern = Pattern.compile("judgment\\s+[\\w-]+");
	private static Pattern casePattern = Pattern.compile("case rule");
	private static Pattern endcPattern = Pattern.compile("end case\\s*$");
	private static Pattern cnamPattern = Pattern.compile("-{2,}+\\s*[\\w-]+");
	private static Pattern dslcPattern = Pattern.compile("//");
	private static Pattern hslcPattern = Pattern.compile("/\\*");
	private static Pattern eslcPattern = Pattern.compile("\\*/");
	
	private int comHead = 0;
	private int comEnd = 0;
	private int keyHead = 0;
	private int keyEnd = 0;
	
	
	public PropertyCaseRuleScanner(IDocument document) {
		this.document = document;
	}
	
	private boolean inComment(Matcher pMatcher) {
		keyEnd = pMatcher.end();
		keyHead = pMatcher.start();
		return comHead < keyHead && keyEnd < comEnd; 
	}
	
	public List<PropertyElement> parse3() {
		int lineNumber = document.getNumberOfLines();
		String lineStr = "";
		int lineOffset = 0;
		int lineLength = 0;
		String category = null;
		String content = null;
		boolean isCommented = false;
		
		for(int i = 0; i < lineNumber; i++) {
			comHead = Integer.MAX_VALUE;
			comEnd = Integer.MAX_VALUE;
			keyHead = 0;
			keyEnd = 0;
			
			try {
				lineOffset = document.getLineOffset(i);
				lineLength = document.getLineLength(i);
				lineStr = document.get(lineOffset, lineLength);
			} catch (BadLocationException e) {
				e.printStackTrace();
			}
			
			if(lineStr.indexOf("/*") != -1) {
				Matcher pMatcher = hslcPattern.matcher(lineStr);
				if(pMatcher.find()) {
					comHead = pMatcher.start();
					isCommented = true;
				}
			}
			if(lineStr.indexOf("//") != -1) {
				Matcher pMatcher = dslcPattern.matcher(lineStr);
				if(pMatcher.find()) {
					int tmphead = pMatcher.start();
					comHead = tmphead < comHead ? tmphead : comHead;
				}
			}
			if(lineStr.lastIndexOf("*/") != -1) {
				Matcher pMatcher = eslcPattern.matcher(lineStr);
				if(pMatcher.find()) {
					comEnd = pMatcher.end();
					isCommented = false;
				}
			}
			if(Pattern.matches("\\s+", lineStr)) {
				continue;
			}
			if(comHead == 0 || isCommented) {
				continue;
			}
			
			if(lineStr.indexOf("package") != -1) {
				Matcher pMatcher = packPattern.matcher(lineStr);
				if(pMatcher.find()) {
					if(!inComment(pMatcher)) {
						category = "package";
						content = pMatcher.group().replaceAll("(package\\s+)|;", "");
						Position position = new Position(lineOffset + keyHead, keyEnd - keyHead);
						try {
							document.addPosition(position);
						} catch (BadLocationException e) {
							e.printStackTrace();
						}
						PropertyElement element = new PropertyElement(category, content);
						element.setPosition(position);
						pList.add(element);
					}
				}
			} else if(lineStr.indexOf("terminals") != -1) {
				Matcher pMatcher = termPattern.matcher(lineStr);
				if(pMatcher.find()) {
					if(!inComment(pMatcher)) {
						category = "terminals";
						content = "";
						Position position = new Position(lineOffset + keyHead, keyEnd - keyHead);
						try {
							document.addPosition(position);
						} catch (BadLocationException e) {
							e.printStackTrace();
						}						
						PropertyElement element = new PropertyElement(category, content);
						element.setPosition(position);
						pList.add(element);
					}
				}
			} else if(lineStr.indexOf("end theorem") != -1) {
				Matcher pMatcher = endtPattern.matcher(lineStr);
				if(pMatcher.find()) {
					if(!inComment(pMatcher)) {
						if(!caseStack.empty() && caseStack.peek().getCategory().equals("theorem")) {
							caseStack.pop();
						}
					}
				}
			} else if(lineStr.indexOf("theorem") != -1) {
				Matcher pMatcher = theoPattern.matcher(lineStr);
				if(pMatcher.find()) {
					if(!inComment(pMatcher)) {
						category = "theorem";
						content = pMatcher.group().replaceAll("theorem\\s+", "");
						Position position = new Position(lineOffset + keyHead, keyEnd - keyHead);
						try {
							document.addPosition(position);
						} catch (BadLocationException e) {
							e.printStackTrace();
						}
						PropertyElement element = new PropertyElement(category, content);
						element.setPosition(position);
						pList.add(element);
						caseStack.push(element);
					}
				}
			} else if(lineStr.indexOf("syntax") != -1) {
				Matcher pMatcher = syntPattern.matcher(lineStr);
				if(pMatcher.find()) {
					if(!inComment(pMatcher)) {
						category = "syntax";
						content = "";
						Position position = new Position(lineOffset + keyHead, keyEnd - keyHead);
						try {
							document.addPosition(position);
						} catch (BadLocationException e) {
							e.printStackTrace();
						}
						PropertyElement element = new PropertyElement(category, content);
						element.setPosition(position);
						pList.add(element);
					}
				}
			} else if(lineStr.indexOf("judgment") != -1) {
				Matcher pMatcher = judgPattern.matcher(lineStr);
				if(pMatcher.find()) {
					if(!inComment(pMatcher)) {
						category = "judgment";
						content = pMatcher.group().replaceAll("judgment\\s+", "");
						Position position = new Position(lineOffset + keyHead, keyEnd - keyHead);
						try {
							document.addPosition(position);
						} catch (BadLocationException e) {
							e.printStackTrace();
						}
						PropertyElement element = new PropertyElement(category, content);
						element.setPosition(position);
						pList.add(element);
					}
				}
			} else if(lineStr.indexOf("case rule") != -1) {
				Matcher pMatcher = casePattern.matcher(lineStr);
				if(pMatcher.find()) {
					if(!inComment(pMatcher)) {
						category = "case rule";
						content = "";
						Position position = new Position(lineOffset + keyHead, keyEnd - keyHead);
						try {
							document.addPosition(position);
						} catch (BadLocationException e) {
							e.printStackTrace();
						}
						PropertyElement element = new PropertyElement(category, content);
						element.setPosition(position);
						pList.add(element);
						if(!caseStack.empty()) {
							caseStack.peek().addChild(element);
						}
						caseStack.push(element);
					}
				}
			} else if(lineStr.indexOf("end case") != -1) {
				Matcher pMatcher = endcPattern.matcher(lineStr);
				if(pMatcher.find()) {
					if(!inComment(pMatcher)) {
						if(!caseStack.empty() && caseStack.peek().getCategory().equals("case rule")) {
							caseStack.pop();
						}
					}
				}
			} else if(lineStr.indexOf("-") != -1) {
				Matcher pMatcher = cnamPattern.matcher(lineStr);
				if(pMatcher.find()) {
					if(!inComment(pMatcher)) {
						if(!caseStack.empty() && caseStack.peek().getCategory().equals("case rule")) {
							PropertyElement pe = caseStack.peek();
							if(pe.getContent().isEmpty()) {
								pe.setContent(pMatcher.group().replaceFirst("^-+\\s*", ""));
							}
						}
					}
				}
			}
		}
		
		return this.pList;
	}
}
