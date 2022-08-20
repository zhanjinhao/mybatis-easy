package cn.addenda.me.fieldfilling.sql;

import cn.addenda.me.fieldfilling.FieldFillingContext;
import cn.addenda.me.fieldfilling.FieldFillingException;
import cn.addenda.ro.grammar.ast.CurdParser;
import cn.addenda.ro.grammar.ast.CurdParserFactory;
import cn.addenda.ro.grammar.ast.create.*;
import cn.addenda.ro.grammar.ast.create.visitor.InsertAstMetaDataDetector;
import cn.addenda.ro.grammar.ast.expression.*;
import cn.addenda.ro.grammar.ast.retrieve.Select;
import cn.addenda.ro.grammar.ast.retrieve.visitor.SelectAstMetaDataDetector;
import cn.addenda.ro.grammar.ast.update.Update;
import cn.addenda.ro.grammar.ast.update.visitor.UpdateAstMetaDataDetector;
import cn.addenda.ro.grammar.lexical.token.Token;
import cn.addenda.ro.grammar.lexical.token.TokenType;

import java.util.*;

/**
 * @author addenda
 * @datetime 2022/8/16 20:40
 */
public class FieldFillingConvertor {

    private FieldFillingConvertor() {
    }

    public static String selectFieldFilling(String sql) {
        CurdParser curdParser = CurdParserFactory.createCurdParser(sql);
        Curd parse = curdParser.parse();
        if (!(parse instanceof Select)) {
            return parse.toString();
        }

        return selectFieldFilling((Select) parse);
    }

    public static String selectFieldFilling(Select select) {
        select.accept(new SelectReturnBaseEntityColumnVisitor(null));
        select.setDetector(SelectAstMetaDataDetector.getInstance());
        select.reSetAstMetaData();
        return select.toString();
    }

    public static String selectFieldFilling(String sql, Set<String> tableNameSet) {
        CurdParser curdParser = CurdParserFactory.createCurdParser(sql);
        Curd parse = curdParser.parse();
        if (!(parse instanceof Select)) {
            return parse.toString();
        }

        return selectFieldFilling((Select) parse, tableNameSet);
    }

    public static String selectFieldFilling(Select select, Set<String> tableNameSet) {
        select.accept(new SelectReturnBaseEntityColumnVisitor(tableNameSet));
        select.setDetector(SelectAstMetaDataDetector.getInstance());
        select.reSetAstMetaData();
        return select.toString();
    }

    public static String insertFieldFilling(String sql, FieldFillingContext context) {
        CurdParser curdParser = CurdParserFactory.createCurdParser(sql);
        Curd parse = curdParser.parse();
        if (!(parse instanceof Insert)) {
            return parse.toString();
        }

        return insertFieldFilling((Insert) parse, context);
    }

    public static String insertFieldFilling(Insert insert, FieldFillingContext context) {
        Curd curd = insert.getInsertRep();

        if (curd instanceof InsertSelectRep) {
            throw new FieldFillingException("不能对 InsertSelectRep 进行字段填充");
        } else if (curd instanceof InsertSetRep) {
            InsertSetRep insertSetRep = (InsertSetRep) curd;
            AssignmentList assignmentList = (AssignmentList) insertSetRep.getAssignmentList();
            List<AssignmentList.Entry> entryList = assignmentList.getEntryList();
            entryList.add(new AssignmentList.Entry(FilledFillingConst.CREATOR_TOKEN, newLiteral(TokenType.STRING, context.getCreator())));
            entryList.add(new AssignmentList.Entry(FilledFillingConst.CREATOR_NAME_TOKEN, newLiteral(TokenType.STRING, context.getCreatorName())));
            entryList.add(new AssignmentList.Entry(FilledFillingConst.CREATE_TIME_TOKEN, newLiteral(TokenType.INTEGER, context.getCreateTime())));
            entryList.add(new AssignmentList.Entry(FilledFillingConst.MODIFIER_TOKEN, newLiteral(TokenType.STRING, context.getModifier())));
            entryList.add(new AssignmentList.Entry(FilledFillingConst.MODIFIER_NAME_TOKEN, newLiteral(TokenType.STRING, context.getModifierName())));
            entryList.add(new AssignmentList.Entry(FilledFillingConst.MODIFY_TIME_TOKEN, newLiteral(TokenType.INTEGER, context.getModifyTime())));
            entryList.add(new AssignmentList.Entry(FilledFillingConst.REMARK_TOKEN, newLiteral(TokenType.STRING, context.getRemark())));
        } else if (curd instanceof InsertValuesRep) {
            InsertValuesRep insertValuesRep = (InsertValuesRep) curd;
            List<List<Curd>> curdListList = insertValuesRep.getCurdListList();
            for (List<Curd> curdList : curdListList) {
                curdList.add(newLiteral(TokenType.STRING, context.getCreator()));
                curdList.add(newLiteral(TokenType.STRING, context.getCreatorName()));
                curdList.add(newLiteral(TokenType.INTEGER, context.getCreateTime()));
                curdList.add(newLiteral(TokenType.STRING, context.getModifier()));
                curdList.add(newLiteral(TokenType.STRING, context.getModifierName()));
                curdList.add(newLiteral(TokenType.INTEGER, context.getModifyTime()));
                curdList.add(newLiteral(TokenType.STRING, context.getRemark()));
            }
            List<Token> columnList = insertValuesRep.getColumnList();
            columnList.add(FilledFillingConst.CREATOR_TOKEN);
            columnList.add(FilledFillingConst.CREATOR_NAME_TOKEN);
            columnList.add(FilledFillingConst.CREATE_TIME_TOKEN);
            columnList.add(FilledFillingConst.MODIFIER_TOKEN);
            columnList.add(FilledFillingConst.MODIFIER_NAME_TOKEN);
            columnList.add(FilledFillingConst.MODIFY_TIME_TOKEN);
            columnList.add(FilledFillingConst.REMARK_TOKEN);
        }
        insert.setDetector(InsertAstMetaDataDetector.getInstance());
        insert.reSetAstMetaData();
        return insert.toString();
    }

    public static String updateFieldFilling(String sql, FieldFillingContext context) {
        CurdParser curdParser = CurdParserFactory.createCurdParser(sql);
        Curd parse = curdParser.parse();
        if (!(parse instanceof Update)) {
            return parse.toString();
        }

        return updateFieldFilling((Update) parse, context);
    }

    public static String updateFieldFilling(Update update, FieldFillingContext context) {
        AssignmentList assignmentList = (AssignmentList) update.getAssignmentList();
        List<AssignmentList.Entry> entryList = assignmentList.getEntryList();
        entryList.add(new AssignmentList.Entry(FilledFillingConst.MODIFIER_TOKEN, newLiteral(TokenType.STRING, context.getModifier())));
        entryList.add(new AssignmentList.Entry(FilledFillingConst.MODIFIER_NAME_TOKEN, newLiteral(TokenType.STRING, context.getModifierName())));
        entryList.add(new AssignmentList.Entry(FilledFillingConst.MODIFY_TIME_TOKEN, newLiteral(TokenType.INTEGER, context.getModifyTime())));
        String remark = context.getRemark();
        if (remark != null) {
            entryList.add(new AssignmentList.Entry(FilledFillingConst.REMARK_TOKEN, newLiteral(TokenType.STRING, remark)));
        }
        update.setDetector(UpdateAstMetaDataDetector.getInstance());
        update.reSetAstMetaData();
        return update.toString();
    }

    private static Literal newLiteral(TokenType tokenType, Object literal) {
        return new Literal(new Token(tokenType, literal));
    }

}
