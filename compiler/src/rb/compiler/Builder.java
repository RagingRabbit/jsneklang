package rb.compiler;

import static me.qmx.jitescript.util.CodegenUtils.*;
import static org.objectweb.asm.Opcodes.*;
import static rb.compiler.Parser.*;

import java.lang.reflect.Method;
import java.util.Map;

import me.qmx.jitescript.CodeBlock;
import me.qmx.jitescript.JiteClass;
import rb.compiler.Parser.Expression;
import rb.compiler.Parser.ExpressionStatement;
import rb.compiler.Parser.FuncCallExpression;
import rb.compiler.Parser.IdentifierExpression;
import rb.compiler.Parser.ImportStatement;
import rb.compiler.Parser.IntExpression;
import rb.compiler.Parser.Statement;
import rb.compiler.Parser.StringExpression;
import rb.compiler.Parser.Syntax;
import rb.compiler.Parser.VarDeclExpression;
import rb.compiler.Parser.Variable;

public class Builder {
	private static Syntax syntax;
	
	public static CompilationUnit run(Syntax syntax) {
		CompilationUnit unit = new CompilationUnit(syntax.name);
		Builder.syntax = syntax;
		
		unit.defineMethod("main", ACC_PUBLIC | ACC_STATIC, sig(void.class, String[].class), new CodeBlock() {
			{
				for (Statement statement : syntax.statements) {
					buildStatement(statement, syntax, this, unit);
				}
				voidreturn();
			}
		});
		
		Builder.syntax = null;
		
		return unit;
	}
	
	private static void buildStatement(Statement statement, Parser.CodeBlock parent, CodeBlock block, CompilationUnit unit) {
		switch (statement.getType()) {
		case STATEMENT_IMPORT: {
			ImportStatement s = (ImportStatement) statement;
			syntax.loadImport(s.name);
			break;
		}
		
		case STATEMENT_EXPR: {
			ExpressionStatement s = (ExpressionStatement) statement;
			buildExpression(s.expr, parent, block);
			block.pop(); // pop expression result
			break;
		}
		}
	}
	
	private static void buildExpression(Expression expr, Parser.CodeBlock parent, CodeBlock block) {
		switch (expr.getExprType()) {
		case EXPR_IDENTIFIER: {
			IdentifierExpression e = (IdentifierExpression) expr;
			loadExpression(e.name, block);
			break;
		}
		case EXPR_STRING: {
			StringExpression e = (StringExpression) expr;
			block.ldc(e.val);
			break;
		}
		case EXPR_INTEGER: {
			IntExpression e = (IntExpression) expr;
			block.pushInt(e.val);
			break;
		}
		case EXPR_VAR_DECL: {
			VarDeclExpression e = (VarDeclExpression) expr;
			buildExpression(e.val, parent, block);
			block.dup();
			storeExpression(e.val, e.name, parent, block);
			break;
		}
		case EXPR_FUNC_CALL: {
			FuncCallExpression e = (FuncCallExpression) expr;
			
			for (Expression arg : e.args) {
				buildExpression(arg, parent, block);
			}
			
			Class<?>[] argTypes = new Class<?>[e.args.size()];
			for (int i = 0; i < e.args.size(); i++) {
				argTypes[i] = getVarType(e.args.get(i));
			}
			Class<?> funcClass = findFunctionClass(e.name, argTypes);
			block.invokestatic(p(funcClass), e.name, sig(void.class, argTypes));
			block.ldc(0);
			break;
		}
		default:
			// TODO ERROR
			break;
		}
	}
	
	private static void storeExpression(Expression expr, String name, Parser.CodeBlock parent, CodeBlock block) {
		Variable var = new Variable();
		var.name = name;
		var.type = getVarType(expr);
		int slot = getVarSlot(var, syntax.vars);
		parent.locals.add(slot);
		
		if (var.type == int.class) {
			block.istore(slot);
		} else {
			block.astore(slot);
		}
	}
	
	private static void loadExpression(String name, CodeBlock block) {
		Variable var = getVariable(name);
		int slot = getVarSlot(var, syntax.vars);
		
		if (var.type == int.class) {
			block.iload(slot);
		} else {
			block.aload(slot);
		}
	}
	
	private static int getVarSlot(Variable var, Map<Integer, Variable> vars) {
		if (!vars.containsValue(var)) {
			for (int i = 0; i < vars.size() + 1; i++) {
				if (i >= vars.size() || !vars.containsKey(i)) {
					vars.put(i, var);
					return i;
				}
			}
		} else {
			for (int index : vars.keySet()) {
				if (vars.get(index) == var) {
					return index;
				}
			}
		}
		
		return -1; // unreachable code
	}
	
	private static Class<?> getVarType(Expression expr) {
		switch (expr.getExprType()) {
		case EXPR_IDENTIFIER: {
			IdentifierExpression e = (IdentifierExpression) expr;
			return getVariable(e.name).type;
		}
		
		case EXPR_STRING: {
			return String.class;
		}
		
		case EXPR_INTEGER: {
			return int.class;
		}
		
		case EXPR_VAR_DECL: {
			VarDeclExpression e = (VarDeclExpression) expr;
			return getVarType(e.val);
		}
		
		case EXPR_FUNC_CALL: {
			return null;// TODO
		}
		default:
			// TODO ERROR
			break;
		}
		return null;
	}
	
	private static Variable getVariable(String name) {
		Map<Integer, Variable> vars = syntax.vars;
		for (int index : vars.keySet()) {
			if (vars.get(index).name.equals(name)) {
				return vars.get(index);
			}
		}
		// TODO ERROR
		return null;
	}
	
	private static Class<?> findFunctionClass(String name, Class<?>... argTypes) {
		Class<?> c = null;
		for (Class<?> imp : syntax.imports) {
			try {
				if (imp.getMethod(name, argTypes) != null) {
					if (c == null) {
						c = imp;
					} else {
						// TODO ERROR function name is ambiguous
					}
				}
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
				// TODO ERROR
			} catch (SecurityException e) {
				e.printStackTrace();
				// TODO ERROR
			}
		}
		return c;
	}
	
	public static class CompilationUnit extends JiteClass {
		public CompilationUnit(String name) {
			super(name);
		}
	}
}
