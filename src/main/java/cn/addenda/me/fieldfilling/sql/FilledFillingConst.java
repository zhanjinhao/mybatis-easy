package cn.addenda.me.fieldfilling.sql;

import cn.addenda.ro.grammar.lexical.token.Token;
import cn.addenda.ro.grammar.lexical.token.TokenType;

/**
 * @author addenda
 * @datetime 2022/8/17 21:33
 */
public class FilledFillingConst {

    private FilledFillingConst() {

    }

    public static final Token CREATOR_TOKEN = new Token(TokenType.IDENTIFIER, "creator");
    public static final Token CREATOR_NAME_TOKEN = new Token(TokenType.IDENTIFIER, "creator_name");
    public static final Token CREATE_TIME_TOKEN = new Token(TokenType.IDENTIFIER, "create_time");
    public static final Token MODIFIER_TOKEN = new Token(TokenType.IDENTIFIER, "modifier");
    public static final Token MODIFIER_NAME_TOKEN = new Token(TokenType.IDENTIFIER, "modifier_name");
    public static final Token MODIFY_TIME_TOKEN = new Token(TokenType.IDENTIFIER, "modify_time");
    public static final Token REMARK_TOKEN = new Token(TokenType.IDENTIFIER, "remark");

}
