package cn.addenda.me.logicaldeletion.sql;

import cn.addenda.ro.grammar.ast.expression.*;
import cn.addenda.ro.grammar.lexical.token.Token;
import cn.addenda.ro.grammar.lexical.token.TokenType;

/**
 * @author addenda
 * @datetime 2022/8/16 20:40
 */
public class LogicalDeletionConst {

    private LogicalDeletionConst() {

    }

    public static final Literal ONE = new Literal(new Token(TokenType.INTEGER, 1));
    public static final Literal ZERO = new Literal(new Token(TokenType.INTEGER, 0));
    public static final Literal NULL = new Literal(new Token(TokenType.NULL, "null"));

    public static final Identifier EQUAL = new Identifier(new Token(TokenType.EQUAL, "="));
    public static final IsNot IS = new IsNot(new Token(TokenType.IS, "is"), null);
    public static final Token OR = new Token(TokenType.OR, "or");

    public static final Token DELETE_TOKEN = new Token(TokenType.IDENTIFIER, "del_fg");
    public static final Identifier DELETE_COLUMN = new Identifier(DELETE_TOKEN.deepClone());
    public static final Comparison EQUAL_ZERO = new Comparison(DELETE_COLUMN, EQUAL.deepClone(), ZERO.deepClone());
    public static final Comparison EQUAL_ONE = new Comparison(DELETE_COLUMN, EQUAL.deepClone(), ONE.deepClone());
    public static final Comparison IS_NULL = new Comparison(DELETE_COLUMN, IS.deepClone(), NULL.deepClone());
    public static final Grouping EQUAL_ZERO_OR_IS_NULL = new Grouping(new Logic(EQUAL_ZERO.deepClone(), OR.deepClone(), IS_NULL.deepClone()));

}
