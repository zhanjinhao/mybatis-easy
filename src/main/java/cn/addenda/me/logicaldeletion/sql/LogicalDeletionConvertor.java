package cn.addenda.me.logicaldeletion.sql;

import cn.addenda.me.fieldfilling.FiledFillingException;
import cn.addenda.ro.grammar.ast.CurdParser;
import cn.addenda.ro.grammar.ast.CurdParserFactory;
import cn.addenda.ro.grammar.ast.create.Insert;
import cn.addenda.ro.grammar.ast.create.InsertSelectRep;
import cn.addenda.ro.grammar.ast.create.InsertSetRep;
import cn.addenda.ro.grammar.ast.create.InsertValuesRep;
import cn.addenda.ro.grammar.ast.delete.Delete;
import cn.addenda.ro.grammar.ast.expression.AssignmentList;
import cn.addenda.ro.grammar.ast.expression.Curd;
import cn.addenda.ro.grammar.ast.expression.Logic;
import cn.addenda.ro.grammar.ast.expression.WhereSeg;
import cn.addenda.ro.grammar.ast.retrieve.Select;
import cn.addenda.ro.grammar.ast.update.Update;
import cn.addenda.ro.grammar.ast.update.visitor.UpdateAstMetaDataDetector;
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

    private LogicalDeletionConvertor() {

    }

    public static String insertLogically(String sql) {
        CurdParser curdParser = CurdParserFactory.createCurdParser(sql);
        Curd parse = curdParser.parse();
        if (!(parse instanceof Insert)) {
            return parse.toString();
        }

        return insertLogically((Insert) parse);
    }

    public static String insertLogically(Insert insert) {
        Curd curd = insert.getInsertRep();

        if (curd instanceof InsertSelectRep) {
            throw new FiledFillingException("不能对 InsertSelectRep 进行字段填充");
        } else if (curd instanceof InsertSetRep) {
            InsertSetRep insertSetRep = (InsertSetRep) curd;
            AssignmentList assignmentList = (AssignmentList) insertSetRep.getAssignmentList();
            List<AssignmentList.Entry> entryList = assignmentList.getEntryList();
            entryList.add(new AssignmentList.Entry(LogicalDeletionConst.DELETE_TOKEN, LogicalDeletionConst.ONE));
        } else if (curd instanceof InsertValuesRep) {
            InsertValuesRep insertValuesRep = (InsertValuesRep) curd;
            List<List<Curd>> curdListList = insertValuesRep.getCurdListList();
            for (List<Curd> curdList : curdListList) {
                curdList.add(LogicalDeletionConst.ONE);
            }
            List<Token> columnList = insertValuesRep.getColumnList();
            columnList.add(LogicalDeletionConst.DELETE_TOKEN);
        }

        insert.reSetAstMetaData();
        return insert.toString();
    }

    public static String deleteLogically(String sql) {
        CurdParser curdParser = CurdParserFactory.createCurdParser(sql);
        Curd parse = curdParser.parse();
        if (!(parse instanceof Delete)) {
            return parse.toString();
        }

        return deleteLogically((Delete) parse);
    }

    public static String deleteLogically(Delete delete) {
        Token tableName = delete.getTableName();
        Curd whereSeg = delete.getWhereSeg();
        List<AssignmentList.Entry> entryList = new ArrayList<>();
        entryList.add(new AssignmentList.Entry(LogicalDeletionConst.DELETE_COLUMN.getName(), LogicalDeletionConst.ONE));
        Update update = new Update(tableName, new AssignmentList(entryList), whereSeg);
        update.setDetector(UpdateAstMetaDataDetector.getInstance());
        update.reSetAstMetaData();
        return update.toString();
    }

    public static String selectLogically(String sql) {
        CurdParser curdParser = CurdParserFactory.createCurdParser(sql);
        Curd select = curdParser.parse();
        if (!(select instanceof Select)) {
            return select.toString();
        }
        return selectLogically((Select) select);
    }

    public static String selectLogically(Select select) {
        Curd accept = select.accept(new SelectAddDeleteConditionVisitor());
        accept.reSetAstMetaData();
        return accept.toString();
    }

    public static String selectLogically(String sql, Set<String> tableNameSet) {
        CurdParser curdParser = CurdParserFactory.createCurdParser(sql);
        Curd select = curdParser.parse();
        if (!(select instanceof Select)) {
            return select.toString();
        }

        return selectLogically((Select) select, tableNameSet);
    }

    public static String selectLogically(Select select, Set<String> tableNameSet) {
        Curd accept = select.accept(new SelectAddDeleteConditionVisitor(tableNameSet));
        accept.reSetAstMetaData();
        return accept.toString();
    }

    public static String updateLogically(String sql) {
        CurdParser curdParser = CurdParserFactory.createCurdParser(sql);
        Curd parse = curdParser.parse();
        if (!(parse instanceof Update)) {
            return parse.toString();
        }

        return updateLogically((Update) parse);
    }

    public static String updateLogically(Update update) {
        WhereSeg whereSeg = (WhereSeg) update.getWhereSeg();
        if (whereSeg == null) {
            ReflectUtils.setFieldValue(update, "whereSeg", new WhereSeg(LogicalDeletionConst.EQUAL_ONE.deepClone()));
        } else {
            Curd logic = whereSeg.getLogic();
            logic = new Logic(logic, new Token(TokenType.AND, "and"), LogicalDeletionConst.EQUAL_ONE);
            ReflectUtils.setFieldValue(whereSeg, "logic", logic);
        }
        update.setDetector(UpdateAstMetaDataDetector.getInstance());
        update.reSetAstMetaData();
        return update.toString();
    }

}
