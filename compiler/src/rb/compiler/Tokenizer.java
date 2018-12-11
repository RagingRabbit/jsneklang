package rb.compiler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Tokenizer {
	static Vector<TokenDef> definitions;

	static {
		definitions = new Vector<TokenDef>();
	}

	public static void run(String src, List<Token> tokens) {
		List<TokenMatch> matches = new ArrayList<TokenMatch>();
		for (TokenDef def : definitions) {
			findMatches(src, def, matches);
		}

		Map<Integer, List<TokenMatch>> groupedMatches = new TreeMap<Integer, List<TokenMatch>>();
		for (TokenMatch match : matches) {
			if (groupedMatches.get(match.start) == null) {
				groupedMatches.put(match.start, new ArrayList<TokenMatch>());
			}
			groupedMatches.get(match.start).add(match);
		}

		TokenMatch lastMatch = null;
		for (int i : groupedMatches.keySet()) {
			Collections.sort(groupedMatches.get(i), new Comparator<TokenMatch>() {
				@Override
				public int compare(TokenMatch o1, TokenMatch o2) {
					return o1.precedence > o2.precedence ? 1 : -1;
				}
			});
			TokenMatch bestMatch = groupedMatches.get(i).get(0);
			if (lastMatch != null && bestMatch.start < lastMatch.end) {
				continue;
			}

			Token token = new Token();
			token.type = bestMatch.type;
			token.val = bestMatch.val;
			tokens.add(token);

			lastMatch = bestMatch;
		}
	}

	private static void findMatches(String input, TokenDef def, List<TokenMatch> matches) {
		Pattern pattern = Pattern.compile(def.regex);
		Matcher matcher = pattern.matcher(input);
		while (matcher.find()) {
			TokenMatch match = new TokenMatch();
			match.start = matcher.start();
			match.end = matcher.end();
			match.val = input.substring(match.start, match.end);
			match.type = def.type;
			match.precedence = def.precedence;
			matches.add(match);
		}
	}

	public static void addDefinition(int type, String regex, int precedence) {
		TokenDef def = new TokenDef();
		def.type = type;
		def.regex = regex;
		def.precedence = precedence;
		definitions.add(def);
	}

	public static void removeDefinitions() {
		definitions.clear();
	}

	public static class Token {
		public int type;
		public String val;
	}

	public static class TokenDef {
		public String regex;
		public int type;
		public int precedence;
	}

	public static class TokenMatch {
		public int type;
		public String val;
		public int start, end;
		public int precedence;
	}
}
