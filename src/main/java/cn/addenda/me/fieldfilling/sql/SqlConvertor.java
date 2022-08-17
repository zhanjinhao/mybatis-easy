package cn.addenda.me.fieldfilling.sql;

import cn.addenda.me.fieldfilling.FiledFillingException;
import cn.addenda.ro.grammar.ast.CurdParser;
import cn.addenda.ro.grammar.ast.CurdParserFactory;
import cn.addenda.ro.grammar.ast.create.*;
import cn.addenda.ro.grammar.ast.expression.*;
import cn.addenda.ro.grammar.ast.update.Update;
import cn.addenda.ro.grammar.lexical.token.Token;
import cn.addenda.ro.grammar.lexical.token.TokenType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author ISJINHAO
 * @Date 2022/1/26 18:01
 */
public class SqlConvertor {

    private SqlConvertor() {
    }

    public static String insertAddEntry(String sql, Map<String, Curd> entryMap) {
        return insertAddEntry(sql, entryMap == null ? new HashMap<>() : entryMap, entryMap == null ? new HashSet<>() : entryMap.keySet());
    }

    public static String insertAddEntry(String sql, Map<String, Curd> entryMap, Set<String> duplicateCheck) {
        CurdParser curdParser = CurdParserFactory.createCurdParser(sql);
        Curd parse = curdParser.parse();
        if (!(parse instanceof Insert)) {
            return parse.toString();
        }

        Insert insert = (Insert) parse;
        Curd curd = insert.getInsertRep();

        if (curd instanceof InsertSelectRep) {
            throw new FiledFillingException("不能对 InsertSelectRep 进行字段填充");
        } else if (curd instanceof InsertSetRep) {
            InsertSetRep insertSetRep = (InsertSetRep) curd;
            AssignmentList assignmentList = (AssignmentList) insertSetRep.getAssignmentList();
            List<AssignmentList.Entry> entryList = assignmentList.getEntryList();
            entryList.forEach(item -> {
                String column = item.getColumn().getLiteral().toString();
                if (duplicateCheck.contains(column)) {
                    throw new FiledFillingException("sql本身字段已存在" + column + "!");
                }
            });
            entryList.addAll(toEntry(entryMap));
        } else if (curd instanceof InsertValuesRep) {
            InsertValuesRep insertValuesRep = (InsertValuesRep) curd;
            Set<String> strings = entryMap.keySet();
            List<List<Curd>> curdListList = insertValuesRep.getCurdListList();
            for (List<Curd> curdList : curdListList) {
                for (String key : strings) {
                    Curd literal = entryMap.get(key);
                    curdList.add(literal);
                }
            }
            List<Token> columnList = insertValuesRep.getColumnList();
            List<Token> collect = strings.stream().map(item -> new Token(TokenType.IDENTIFIER, item)).collect(Collectors.toList());
            columnList.addAll(collect);
        }

        return insert.toString();
    }


    public static String updateAddEntry(String sql, Map<String, Curd> entryMap) {
        return updateAddEntry(sql, entryMap == null ? new HashMap<>() : entryMap, entryMap == null ? new HashSet<>() : entryMap.keySet());
    }

    public static String updateAddEntry(String sql, Map<String, Curd> entryMap, Set<String> duplicateCheck) {
        CurdParser curdParser = CurdParserFactory.createCurdParser(sql);
        Curd parse = curdParser.parse();
        if (!(parse instanceof Update)) {
            return parse.toString();
        }

        Update update = (Update) parse;
        AssignmentList assignmentList = (AssignmentList) update.getAssignmentList();
        List<AssignmentList.Entry> entryList = assignmentList.getEntryList();

        entryList.forEach(item -> {
            String column = item.getColumn().getLiteral().toString();
            if (duplicateCheck.contains(column)) {
                throw new FiledFillingException("sql本身字段已存在" + column + "!");
            }
        });

        entryList.addAll(toEntry(entryMap));
        return update.toString();
    }

    private static Collection<? extends AssignmentList.Entry> toEntry(Map<String, Curd> entryMap) {
        List<AssignmentList.Entry> entryList = new ArrayList<>();
        Set<Map.Entry<String, Curd>> entries = entryMap.entrySet();
        for (Map.Entry<String, Curd> entry : entries) {
            String key = entry.getKey();
            Curd value = entry.getValue();
            entryList.add(new AssignmentList.Entry(new Token(TokenType.IDENTIFIER, key), value));
        }
        return entryList;
    }

}
