package cn.addenda.me.logicaldeletion.sql;

import cn.addenda.me.logicaldeletion.LogicalDeletionException;
import cn.addenda.ro.grammar.ast.CurdUtils;
import cn.addenda.ro.grammar.ast.create.Insert;
import cn.addenda.ro.grammar.ast.create.InsertSelectRep;
import cn.addenda.ro.grammar.ast.create.InsertSetRep;
import cn.addenda.ro.grammar.ast.create.InsertValuesRep;
import cn.addenda.ro.grammar.ast.delete.Delete;
import cn.addenda.ro.grammar.ast.expression.*;
import cn.addenda.ro.grammar.ast.retrieve.Select;
import cn.addenda.ro.grammar.ast.update.Update;
import cn.addenda.ro.grammar.function.evaluator.FunctionEvaluator;
import cn.addenda.ro.grammar.lexical.token.Token;
import cn.addenda.ro.grammar.lexical.token.TokenType;
import cn.addenda.ro.grammar.util.ReflectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author addenda
 * @datetime 2022/8/16 20:36
 */
public class LogicalDeletionConvertor {

    private final FunctionEvaluator<?> functionEvaluator;

    public LogicalDeletionConvertor(FunctionEvaluator<?> functionEvaluator) {
        this.functionEvaluator = functionEvaluator;
    }

    public String insertLogically(String sql) {
        Curd parse = CurdUtils.parse(sql, functionEvaluator, true);
        if (!(parse instanceof Insert)) {
            return parse.toString();
        }

        return insertLogically((Insert) parse);
    }

    public String insertLogically(Insert insert) {
        Curd curd = insert.getInsertRep();

        if (curd instanceof InsertSelectRep) {
            throw new LogicalDeletionException("不能对 InsertSelectRep 进行字段填充");
        } else if (curd instanceof InsertSetRep) {
            InsertSetRep insertSetRep = (InsertSetRep) curd;
            AssignmentList assignmentList = (AssignmentList) insertSetRep.getAssignmentList();
            List<AssignmentList.Entry> entryList = assignmentList.getEntryList();
            entryList.add(new AssignmentList.Entry(LogicalDeletionConst.DELETE_TOKEN, LogicalDeletionConst.ZERO.deepClone()));
        } else if (curd instanceof InsertValuesRep) {
            InsertValuesRep insertValuesRep = (InsertValuesRep) curd;
            List<List<Curd>> curdListList = insertValuesRep.getCurdListList();
            for (List<Curd> curdList : curdListList) {
                curdList.add(LogicalDeletionConst.ZERO.deepClone());
            }
            List<Token> columnList = insertValuesRep.getColumnList();
            columnList.add(LogicalDeletionConst.DELETE_TOKEN);
        }
        insert.reDetectAstMetaData();
        return insert.toString();
    }

    public String deleteLogically(String sql) {
        Curd parse = CurdUtils.parse(sql, functionEvaluator, true);
        if (!(parse instanceof Delete)) {
            return parse.toString();
        }

        return deleteLogically((Delete) parse);
    }

    public String deleteLogically(Delete delete) {
        Token tableName = delete.getTableName();
        List<AssignmentList.Entry> entryList = new ArrayList<>();
        entryList.add(new AssignmentList.Entry(
                LogicalDeletionConst.DELETE_COLUMN.getName().deepClone(), LogicalDeletionConst.ONE.deepClone()));
        WhereSeg whereSeg = (WhereSeg) delete.getWhereSeg();
        if (whereSeg == null) {
            whereSeg = new WhereSeg(LogicalDeletionConst.EQUAL_ZERO.deepClone());
        } else {
            whereSeg = new WhereSeg(new Logic(
                    new Grouping(whereSeg.getLogic().deepClone()),
                    new Token(TokenType.AND, "and"), LogicalDeletionConst.EQUAL_ZERO.deepClone()));
        }
        Update update = new Update(tableName, new AssignmentList(entryList), whereSeg);
        update.reDetectAstMetaData();
        return update.toString();
    }

    public String selectLogically(String sql) {
        Curd select = CurdUtils.parse(sql, functionEvaluator, true);
        if (!(select instanceof Select)) {
            return select.toString();
        }
        return selectLogically((Select) select);
    }

    public String selectLogically(Select select) {
        Curd accept = select.accept(new SelectAddDeleteConditionVisitor());
        accept.reDetectAstMetaData();
        return accept.toString();
    }

    public String selectLogically(String sql, Set<String> tableNameSet) {
        Curd select = CurdUtils.parse(sql, functionEvaluator, true);
        if (!(select instanceof Select)) {
            return select.toString();
        }

        return selectLogically((Select) select, tableNameSet);
    }

    public String selectLogically(Select select, Set<String> tableNameSet) {
        Curd accept = select.accept(new SelectAddDeleteConditionVisitor(tableNameSet));
        accept.reDetectAstMetaData();
        return accept.toString();
    }

    public String updateLogically(String sql) {
        Curd parse = CurdUtils.parse(sql, functionEvaluator, true);
        if (!(parse instanceof Update)) {
            return parse.toString();
        }

        return updateLogically((Update) parse);
    }

    public String updateLogically(Update update) {
        WhereSeg whereSeg = (WhereSeg) update.getWhereSeg();
        if (whereSeg == null) {
            ReflectUtils.setFieldValue(update, "whereSeg", new WhereSeg(LogicalDeletionConst.EQUAL_ZERO.deepClone()));
        } else {
            Curd logic = whereSeg.getLogic();
            logic = new Logic(logic, new Token(TokenType.AND, "and"), LogicalDeletionConst.EQUAL_ZERO.deepClone());
            ReflectUtils.setFieldValue(whereSeg, "logic", logic);
        }
        update.reDetectAstMetaData();
        return update.toString();
    }

}
