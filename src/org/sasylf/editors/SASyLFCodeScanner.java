package org.sasylf.editors;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.IWhitespaceDetector;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WhitespaceRule;
import org.eclipse.jface.text.rules.WordRule;
import org.eclipse.swt.SWT;
import org.sasylf.editors.SASyLFColorProvider.TokenColorClass;

import edu.cmu.cs.sasylf.parser.DSLToolkitParser;

public class SASyLFCodeScanner extends RuleBasedScanner{

	public SASyLFCodeScanner(SASyLFColorProvider provider)	{
		IToken keyword = provider.createToken(new TextAttribute(null,null,SWT.BOLD), TokenColorClass.Keyword); // new Token (new TextAttribute (provider.getColor(SASyLFColorProvider.Fragments.Keyword), null,SWT.BOLD));
		IToken comment = provider.createToken(null, TokenColorClass.SingleLineComment); //new Token (new TextAttribute (provider.getColor(SASyLFColorProvider.Fragments.SingleLineComment)));
		IToken other = provider.createToken(null, TokenColorClass.Default); // new Token (new TextAttribute (provider.getColor(SASyLFColorProvider.Fragments.Default)));
		IToken multiLineComment = provider.createToken(null,  TokenColorClass.MultiLineComment); // new Token (new TextAttribute (provider.getColor(SASyLFColorProvider.Fragments.MultiLineComment)));
		IToken rule = provider.createToken(null, TokenColorClass.Rule); // new Token (new TextAttribute (provider.getColor(SASyLFColorProvider.Fragments.Rule)));

		List<IRule> rules = new ArrayList<IRule> ();
		rules.add (new EndOfLineRule ("//", comment));
		rules.add (new MultiLineRule ("/*", "*/", multiLineComment));
		rules.add (new LineRule ('-', rule));
		rules.add (new LineRule ('\u2014', rule));
		rules.add (new LineRule ('\u2015', rule));
		rules.add (new LineRule ('\u2500', rule));

		rules.add (new WhitespaceRule (new IWhitespaceDetector() {
			@Override
			public boolean isWhitespace(char ch){
				return Character.isWhitespace(ch);
			}
		}));

		WordRule wordRule = new WordRule (new SASyLFWordDetector(), other);

		for (String key : DSLToolkitParser.allKeywords()) {
			wordRule.addWord(key, keyword);
		}

		rules.add(wordRule);
		IRule[] result = new IRule[rules.size()];
		rules.toArray(result);
		setRules(result);

	}

	public static class SASyLFWordDetector implements IWordDetector {
		@Override
		public boolean isWordPart (char ch) {
			return Character.isLetter(ch) ||
					ch == '_' || ch == '-' ||
					Character.isDigit(ch);
		}

		@Override
		public boolean isWordStart (char ch) {
			return Character.isLetter(ch) ||
					ch == '_';
		}
	}
	
	public static class LineRule implements IPredicateRule {
		protected IToken token;
		private char comp;
		
		public LineRule(char barChar, IToken token) {
			comp = barChar;
			this.token = token;
		}
		
		@Override
		public IToken evaluate(ICharacterScanner scanner) {
			int c = scanner.read();
			if (c != comp) {
				scanner.unread();
				return Token.UNDEFINED;
			}
			c = scanner.read();
			if (c != comp) {
				scanner.unread();
				scanner.unread();
				return Token.UNDEFINED;
			}
			c = scanner.read();
			if (c != comp) {
				scanner.unread();
				scanner.unread();
				scanner.unread();
				return Token.UNDEFINED;
			}
		
			while (c == comp)
				c = scanner.read();
			
			while (Character.isWhitespace(c))
				c = scanner.read();
			
			while (Character.isLetterOrDigit(c) || c == '-')
				c = scanner.read();

			scanner.unread();
			return token;
		}

		@Override
		public IToken getSuccessToken() {
			return token;
		}

		@Override
		public IToken evaluate(ICharacterScanner scanner, boolean resume) {
			return evaluate(scanner);
		}
	}

}