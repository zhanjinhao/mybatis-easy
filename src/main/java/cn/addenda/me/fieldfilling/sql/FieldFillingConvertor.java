package cn.addenda.me.fieldfilling.sql;

import cn.addenda.me.fieldfilling.FieldFillingContext;
import cn.addenda.me.fieldfilling.FieldFillingException;
import cn.addenda.ro.grammar.ast.CurdUtils;
import cn.addenda.ro.grammar.ast.create.*;
import cn.addenda.ro.grammar.ast.expression.*;
import cn.addenda.ro.grammar.ast.retrieve.Select;
import cn.addenda.ro.grammar.ast.update.Update;
import cn.addenda.ro.grammar.function.evaluator.FunctionEvaluator;
import cn.addenda.ro.grammar.lexical.token.Token;
import cn.addenda.ro.grammar.lexical.token.TokenType;

import java.util.*;

/**
 * @author addenda
 * @datetime 2022/8/16 20:40
 */
public class FieldFillingConvertor {

    private final FunctionEvaluator<?> functionEvaluator;

    public FieldFillingConvertor(FunctionEvaluator<?> functionEvaluator) {
        this.functionEvaluator = functionEvaluator;
    }

    public String selectFieldFilling(String sql) {
        Curd parse = CurdUtils.parse(sql, functionEvaluator, true);
        if (!(parse instanceof Select)) {
            return parse.toString();
        }

        return selectFieldFilling((Select) parse);
    }

    public String selectFieldFilling(Select select) {
        select.accept(new SelectReturnBaseEntityColumnVisitor(null));
        select.reSetAstMetaData();
        return select.toString();
    }

    public String selectFieldFilling(String sql, Set<String> tableNameSet) {
        Curd parse = CurdUtils.parse(sql, functionEvaluator, true);
        if (!(parse instanceof Select)) {
            return parse.toString();
        }

        return selectFieldFilling((Select) parse, tableNameSet);
    }

    public String selectFieldFilling(Select select, Set<String> tableNameSet) {
        select.accept(new SelectReturnBaseEntityColumnVisitor(tableNameSet));
        select.reSetAstMetaData();
        return select.toString();
    }

    public String insertFieldFilling(String sql, FieldFillingContext context) {
        Curd parse = CurdUtils.parse(sql, functionEvaluator, true);
        if (!(parse instanceof Insert)) {
            return parse.toString();
        }

        return insertFieldFilling((Insert) parse, context);
    }

    public String insertFieldFilling(Insert insert, FieldFillingContext context) {
        Curd curd = insert.getInsertRep();

        if (curd instanceof InsertSelectRep) {
            throw new FieldFillingException("不能对 InsertSelectRep 进行字段填充");
        } else if (curd instanceof InsertSetRep) {
            InsertSetRep insertSetRep = (InsertSetRep) curd;
            AssignmentList assignmentList = (AssignmentList) insertSetRep.getAssignmentList();
            List<AssignmentList.Entry> entryList = assignmentList.getEntryList();
            entryList.add(new AssignmentList.Entry(FilledFillingConst.CREATOR_TOKEN, newLiteral(TokenType.STRING, context.getCreator())));
            entryList.add(new AssignmentList.Entry(FilledFillingConst.CREATOR_NAME_TOKEN, newLiteral(TokenType.STRING, context.getCreatorName())));
            // createTime类型是String，但是TokenType不能是STRING，否则会被''包裹。
            entryList.add(new AssignmentList.Entry(FilledFillingConst.CREATE_TIME_TOKEN, newLiteral(TokenType.INTEGER, context.getCreateTime())));
            entryList.add(new AssignmentList.Entry(FilledFillingConst.MODIFIER_TOKEN, newLiteral(TokenType.STRING, context.getModifier())));
            entryList.add(new AssignmentList.Entry(FilledFillingConst.MODIFIER_NAME_TOKEN, newLiteral(TokenType.STRING, context.getModifierName())));
            // createTime类型是String，但是TokenType不能是STRING，否则会被''包裹。
            entryList.add(new AssignmentList.Entry(FilledFillingConst.MODIFY_TIME_TOKEN, newLiteral(TokenType.INTEGER, context.getModifyTime())));
            String remark = context.getRemark();
            if (remark != null) {
                entryList.add(new AssignmentList.Entry(FilledFillingConst.REMARK_TOKEN, newLiteral(TokenType.STRING, remark)));
            }
        } else if (curd instanceof InsertValuesRep) {
            InsertValuesRep insertValuesRep = (InsertValuesRep) curd;
            List<List<Curd>> curdListList = insertValuesRep.getCurdListList();
            String remark = context.getRemark();
            for (List<Curd> curdList : curdListList) {
                curdList.add(newLiteral(TokenType.STRING, context.getCreator()));
                curdList.add(newLiteral(TokenType.STRING, context.getCreatorName()));
                // createTime类型是String，但是TokenType不能是STRING，否则会被''包裹。
                curdList.add(newLiteral(TokenType.INTEGER, context.getCreateTime()));
                curdList.add(newLiteral(TokenType.STRING, context.getModifier()));
                curdList.add(newLiteral(TokenType.STRING, context.getModifierName()));
                // createTime类型是String，但是TokenType不能是STRING，否则会被''包裹。
                curdList.add(newLiteral(TokenType.INTEGER, context.getModifyTime()));
                if (remark != null) {
                    curdList.add(newLiteral(TokenType.STRING, remark));
                }
            }
            List<Token> columnList = insertValuesRep.getColumnList();
            columnList.add(FilledFillingConst.CREATOR_TOKEN);
            columnList.add(FilledFillingConst.CREATOR_NAME_TOKEN);
            columnList.add(FilledFillingConst.CREATE_TIME_TOKEN);
            columnList.add(FilledFillingConst.MODIFIER_TOKEN);
            columnList.add(FilledFillingConst.MODIFIER_NAME_TOKEN);
            columnList.add(FilledFillingConst.MODIFY_TIME_TOKEN);
            if (remark != null) {
                columnList.add(FilledFillingConst.REMARK_TOKEN);
            }
        }
        insert.reSetAstMetaData();
        return insert.toString();
    }

    public String updateFieldFilling(String sql, FieldFillingContext context) {
        Curd parse = CurdUtils.parse(sql, functionEvaluator, true);
        if (!(parse instanceof Update)) {
            return parse.toString();
        }

        return updateFieldFilling((Update) parse, context);
    }

    public String updateFieldFilling(Update update, FieldFillingContext context) {
        AssignmentList assignmentList = (AssignmentList) update.getAssignmentList();
        List<AssignmentList.Entry> entryList = assignmentList.getEntryList();
        entryList.add(new AssignmentList.Entry(FilledFillingConst.MODIFIER_TOKEN, newLiteral(TokenType.STRING, context.getModifier())));
        entryList.add(new AssignmentList.Entry(FilledFillingConst.MODIFIER_NAME_TOKEN, newLiteral(TokenType.STRING, context.getModifierName())));
        // createTime类型是String，但是TokenType不能是STRING，否则会被''包裹。
        entryList.add(new AssignmentList.Entry(FilledFillingConst.MODIFY_TIME_TOKEN, newLiteral(TokenType.INTEGER, context.getModifyTime())));
        String remark = context.getRemark();
        if (remark != null) {
            entryList.add(new AssignmentList.Entry(FilledFillingConst.REMARK_TOKEN, newLiteral(TokenType.STRING, remark)));
        }
        update.reSetAstMetaData();
        return update.toString();
    }

    private static Literal newLiteral(TokenType tokenType, Object literal) {
        return new Literal(new Token(tokenType, literal));
    }

}
