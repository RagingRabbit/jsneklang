package rb.compiler;

import static rb.compiler.Lexer.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rb.compiler.Tokenizer.Token;
import rb.compiler.utils.IntPtr;

public class Parser {
	public static final int STATEMENT_UNDEFINED = 0,
			STATEMENT_NO_OP = 1,
			STATEMENT_COMPOUND = 2,
			STATEMENT_EXPR = 3,
			STATEMENT_IF = 4,
			STATEMENT_ELSE = 5,
			STATEMENT_IMPORT = 6;
	
	public static final int EXPR_UNDEFINED = 0,
			EXPR_NULL = 1,
			EXPR_COMPOUND = 2,
			EXPR_IDENTIFIER = 3,
			EXPR_STRING = 4,
			EXPR_INTEGER = 5,
			EXPR_BOOLEAN = 6,
			EXPR_VAR_DECL = 7,
			EXPR_FUNC_CALL = 8;
	
	public static void run(List<Token> tokens, Syntax syntax) {
		for (IntPtr i = new IntPtr(); i.val < tokens.size();) {
			Statement statement = null;
			if ((statement = parseStatement(tokens, i)) != null) {
				syntax.statements.add(statement);
			} else {
				// TODO ERROR or optimized out
			}
		}
	}
	
	private static Statement parseStatement(List<Token> tokens, IntPtr i) {
		// Import statement
		if (tokens.size() - i.val >= 2 && tokens.get(i.val).type == TOKEN_KWORD_IMPORT) {
			return parseImportStatement(tokens, i);
		}
		
		// No operation statement
		if (tokens.size() - i.val >= 1 && tokens.get(i.val).type == TOKEN_EOL) {
			i.val++;
			return null;
		}
		
		// Expression statement
		{
			return parseExpressionStatement(tokens, i);
		}
	}
	
	private static Statement parseImportStatement(List<Token> tokens, IntPtr i) {
		int eol = findToken(tokens, TOKEN_EOL, i.val + 2);
		if (eol == -1) {
			// TODO ERROR
		}
		String name = tokens.get(i.val + 1).val;
		i.val = eol + 1;
		
		ImportStatement result = new ImportStatement();
		result.name = name;
		return result;
	}
	
	private static Statement parseExpressionStatement(List<Token> tokens, IntPtr i) {
		int eol = findToken(tokens, TOKEN_EOL, i.val + 1);
		if (eol == -1) {
			// TODO ERROR
		}
		Expression expr = parseExpression(tokens, i);
		if (i.val != eol) {
			// TODO ERROR
		}
		i.val = eol + 1;
		
		ExpressionStatement result = new ExpressionStatement();
		result.expr = expr;
		return result;
	}
	
	private static Expression parseExpression(List<Token> tokens, IntPtr i) {
		// Compound expression
		if (tokens.size() - i.val >= 2 && tokens.get(i.val).type == TOKEN_PAREN_LEFT) {
			i.val++;
			Expression expr = parseExpression(tokens, i);
			if (tokens.get(i.val).type != TOKEN_PAREN_RIGHT) {
				// TODO ERROR
			}
			i.val++;
			
			CompoundExpression result = new CompoundExpression();
			result.expr = expr;
			return result;
		}
		
		// String literal expression
		if (tokens.size() - i.val >= 1 && tokens.get(i.val).type == TOKEN_STRING) {
			String val = tokens.get(i.val).val.substring(1, tokens.get(i.val).val.length() - 1);
			i.val++;
			
			StringExpression result = new StringExpression();
			result.val = val;
			return result;
		}
		
		// Integer literal expression
		if (tokens.size() - i.val >= 1 && tokens.get(i.val).type == TOKEN_INTEGER) {
			int val = Integer.parseInt(tokens.get(i.val).val);
			i.val++;
			
			IntExpression result = new IntExpression();
			result.val = val;
			return result;
		}
		
		// Boolean literal expression
		if (tokens.size() - i.val >= 1 && tokens.get(i.val).type == TOKEN_BOOLEAN) {
			boolean val = Boolean.parseBoolean(tokens.get(i.val).val);
			i.val++;
			
			BoolExpression result = new BoolExpression();
			result.val = val;
			return result;
		}
		
		// Function call expression
		if (tokens.size() - i.val >= 2 && tokens.get(i.val).type == TOKEN_IDENTIFIER && tokens.get(i.val + 1).type == TOKEN_PAREN_LEFT) {
			int leftParen = i.val + 1;
			int rightParen = findMatchingParen(tokens, i.val + 2);
			if (rightParen == -1) {
				// TODO ERROR
			}
			String funcName = tokens.get(i.val).val;
			
			i.val = leftParen + 1;
			List<Expression> args = parseFuncCallArgs(tokens, i);
			
			FuncCallExpression result = new FuncCallExpression();
			result.name = funcName;
			result.args = args;
			return result;
		}
		
		// Variable declaration expression
		if (tokens.size() - i.val >= 3 && tokens.get(i.val).type == TOKEN_IDENTIFIER && tokens.get(i.val + 1).type == TOKEN_ASSIGN) {
			int assign = findToken(tokens, TOKEN_ASSIGN, i.val + 1);
			if (assign != i.val + 1) {
				// TODO ERROR
			}
			String varName = tokens.get(i.val).val;
			i.val += 2;
			Expression expr = parseExpression(tokens, i);
			
			VarDeclExpression result = new VarDeclExpression();
			result.name = varName;
			result.val = expr;
			return result;
		}
		
		// Identifier expression
		if (tokens.size() - i.val >= 1 && tokens.get(i.val).type == TOKEN_IDENTIFIER) {
			String name = tokens.get(i.val).val;
			i.val++;
			
			IdentifierExpression result = new IdentifierExpression();
			result.name = name;
			return result;
		}
		
		return null;
	}
	
	private static List<Expression> parseFuncCallArgs(List<Token> tokens, IntPtr i) {
		List<Expression> args = new ArrayList<Expression>();
		int rightParen = findMatchingParen(tokens, i.val);
		if (rightParen == -1) {
			// TODO ERROR
		}
		for (int j = i.val; j < tokens.size(); j++) {
			int argEnd = j + 1 == rightParen ? rightParen : findToken(tokens, TOKEN_COMMA, j + 1);
			if (argEnd == -1) {
				// TODO ERROR
			}
			i.val = j;
			Expression expr = parseExpression(tokens, i);
			if (i.val != argEnd) {
				// TODO ERROR
			}
			i.val = argEnd + 1;
			args.add(expr);
			
			if (argEnd == rightParen) {
				break;
			}
		}
		return args;
	}
	
	private static int findMatchingParen(List<Token> tokens, int start) {
		int level = 1;
		for (int i = start; i < tokens.size(); i++) {
			if (tokens.get(i).type == TOKEN_PAREN_LEFT) {
				level++;
			} else if (tokens.get(i).type == TOKEN_PAREN_RIGHT) {
				level--;
				if (level == 0) {
					return i;
				}
			}
		}
		return -1;
	}
	
	private static int findToken(List<Token> tokens, int type, int start) {
		for (int i = start; i < tokens.size(); i++) {
			if (tokens.get(i).type == type) {
				return i;
			}
		}
		return -1;
	}
	
	static class Variable {
		String name;
		Class<?> type;
	}
	
	static class CodeBlock {
		List<Statement> statements = new ArrayList<Statement>();
		List<Integer> locals = new ArrayList<Integer>();
	}
	
	static class Syntax extends CodeBlock {
		String name;
		Map<Integer, Variable> vars = new HashMap<Integer, Variable>();
		List<Class<?>> imports = new ArrayList<Class<?>>();
		
		void loadImport(String imp) {
			try {
				Class<?> c = Class.forName(imp);
				imports.add(c);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
	
	static abstract class Statement {
		abstract int getType();
	}
	
	static class NoOpStatement extends Statement {
		int getType() {
			return STATEMENT_NO_OP;
		}
	}
	
	static class CompoundStatement extends Statement {
		int getType() {
			return STATEMENT_COMPOUND;
		}
	}
	
	static class ImportStatement extends Statement {
		String name;
		
		int getType() {
			return STATEMENT_IMPORT;
		}
	}
	
	static class ExpressionStatement extends Statement {
		Expression expr;
		
		int getType() {
			return STATEMENT_EXPR;
		}
	}
	
	static abstract class Expression {
		abstract int getExprType();
	}
	
	static class CompoundExpression extends Expression {
		Expression expr;
		
		int getExprType() {
			return EXPR_COMPOUND;
		}
	}
	
	static class IdentifierExpression extends Expression {
		String name;
		
		int getExprType() {
			return EXPR_IDENTIFIER;
		}
	}
	
	static class StringExpression extends Expression {
		String val;
		
		int getExprType() {
			return EXPR_STRING;
		}
	}
	
	static class IntExpression extends Expression {
		int val;
		
		int getExprType() {
			return EXPR_INTEGER;
		}
	}
	
	static class BoolExpression extends Expression {
		boolean val;
		
		int getExprType() {
			return EXPR_BOOLEAN;
		}
	}
	
	static class VarDeclExpression extends Expression {
		String name;
		Expression val;
		
		int getExprType() {
			return EXPR_VAR_DECL;
		}
	}
	
	static class FuncCallExpression extends Expression {
		String name;
		List<Expression> args = new ArrayList<Expression>();
		
		int getExprType() {
			return EXPR_FUNC_CALL;
		}
	}
}
