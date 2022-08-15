package cn.addenda.me.fieldfilling.sql;

import cn.addenda.me.fieldfilling.FiledFillingException;
import cn.addenda.ro.grammar.ast.AstMetaData;
import cn.addenda.ro.grammar.ast.CurdParser;
import cn.addenda.ro.grammar.ast.CurdParserFactory;
import cn.addenda.ro.grammar.ast.CurdVisitor;
import cn.addenda.ro.grammar.ast.create.*;
import cn.addenda.ro.grammar.ast.delete.Delete;
import cn.addenda.ro.grammar.ast.expression.*;
import cn.addenda.ro.grammar.ast.retrieve.*;
import cn.addenda.ro.grammar.ast.update.Update;
import cn.addenda.ro.grammar.lexical.token.Token;
import cn.addenda.ro.grammar.lexical.token.TokenType;
import cn.addenda.ro.grammar.util.ReflectUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author ISJINHAO
 * @Date 2022/1/26 18:01
 */
public class SqlConvertor {

    private static final Literal ONE = new Literal(new Token(TokenType.INTEGER, 1));
    private static final Literal ZERO = new Literal(new Token(TokenType.INTEGER, 0));
    private static final Literal NULL = new Literal(new Token(TokenType.NULL, "null"));

    private static final Identifier EQUAL = new Identifier(new Token(TokenType.EQUAL, "="));
    private static final IsNot IS = new IsNot(new Token(TokenType.IS, "is"), null);
    private static final Token OR = new Token(TokenType.OR, "or");

    private static final Identifier DELETE_COLUMN = new Identifier(new Token(TokenType.IDENTIFIER, "del_fg"));
    private static final Comparison EQUAL_ZERO = new Comparison(DELETE_COLUMN, EQUAL, ZERO);
    private static final Comparison IS_NULL = new Comparison(DELETE_COLUMN, IS, NULL);
    private static final Grouping EQUAL_ZERO_OR_IS_NULL = new Grouping(new Logic(EQUAL_ZERO, OR, IS_NULL));

    private SqlConvertor() {
    }

    public static String selectAddComparison(String sql) {
        CurdParser curdParser = CurdParserFactory.createCurdParser(sql);
        Curd parse = curdParser.parse();
        if (!(parse instanceof Select)) {
            return parse.toString();
        }
        return parse.accept(new SelectAddDeleteConditionVisitor()).toString();
    }

    public static String selectAddComparison(String sql, Set<String> tableNameSet) {
        CurdParser curdParser = CurdParserFactory.createCurdParser(sql);
        Curd parse = curdParser.parse();
        if (!(parse instanceof Select)) {
            return parse.toString();
        }
        return parse.accept(new SelectAddDeleteConditionVisitor(tableNameSet)).toString();
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

    public static String deleteLogically(String sql) {
        CurdParser curdParser = CurdParserFactory.createCurdParser(sql);
        Curd parse = curdParser.parse();
        if (!(parse instanceof Delete)) {
            return parse.toString();
        }

        Delete delete = (Delete) parse;
        Token tableName = delete.getTableName();
        Curd whereSeg = delete.getWhereSeg();
        List<AssignmentList.Entry> entryList = new ArrayList<>();
        entryList.add(new AssignmentList.Entry(DELETE_COLUMN.getName(), ONE));
        return new Update(tableName, new AssignmentList(entryList), whereSeg).toString();
    }


    public static class SelectAddDeleteConditionVisitor implements CurdVisitor<Curd> {

        // 允许被添加 comparison 字段的表名。
        // 为null时表示所有的表名都添加 comparison
        private Set<String> availableTableNameSet;

        public SelectAddDeleteConditionVisitor(Set<String> availableTableNameSet) {
            this.availableTableNameSet = availableTableNameSet;
        }

        public SelectAddDeleteConditionVisitor() {
            this.availableTableNameSet = null;
        }

        @Override
        public Curd visitSelect(Select select) {
            select.getLeftCurd().accept(this);

            Curd rightCurd = select.getRightCurd();
            if (rightCurd != null) {
                rightCurd.accept(this);
            }
            return select;
        }

        @Override
        public Curd visitSingleSelect(SingleSelect singleSelect) {

            TableSeg tableSeg = (TableSeg) singleSelect.getTableSeg();
            tableSeg.accept(this);
            SingleSelectAstMetaData astMetaData = (SingleSelectAstMetaData) tableSeg.getAstMetaData();
            Set<String> physicalViewNameSet = getPhysicalViewNameSet(astMetaData);
            Set<String> userDefinedViewNameSet = getAvailableViewName(astMetaData);

            Curd deleteCondition;
            if (checkIsOuterJoinQuery(singleSelect)) {
                joinConditionAddDeleteCondition(tableSeg);
                deleteCondition = EQUAL_ZERO_OR_IS_NULL;
            } else {
                deleteCondition = EQUAL_ZERO;
            }

            WhereSeg whereSeg = (WhereSeg) singleSelect.getWhereSeg();
            // 对于不存在where条件的语法，修改singleSelect的whereSeg属性的值
            if (whereSeg == null) {
                Curd deleteLogic = createLogic(physicalViewNameSet, userDefinedViewNameSet, deleteCondition);
                if (deleteLogic != null) {
                    whereSeg = new WhereSeg(deleteLogic);
                    // TODO 触发计算AstMetaData
                    ReflectUtils.setFieldValue(singleSelect, "whereSeg", whereSeg);
                }
            }
            // 对于存在where条件的语法，修改whereSeg的logic属性的值
            else {
                Curd logic = whereSeg.getLogic();
                whereSeg.accept(this);
                Curd deleteLogic = createLogic(physicalViewNameSet, userDefinedViewNameSet, deleteCondition);
                if (deleteLogic != null) {
                    deleteLogic = new Logic(logic, new Token(TokenType.AND, "and"), deleteLogic);
                    ReflectUtils.setFieldValue(whereSeg, "logic", deleteLogic);
                }
            }

            return singleSelect;
        }

        private void joinConditionAddDeleteCondition(TableSeg tableSeg) {
            Curd leftCurd = tableSeg.getLeftCurd();
            TableRep rightCurd = (TableRep) tableSeg.getRightCurd();
            if (rightCurd != null) {
                SingleSelectAstMetaData astMetaData = (SingleSelectAstMetaData) tableSeg.getAstMetaData();
                Set<String> userDefinedViewNameSet = getAvailableViewName(astMetaData);

                Set<String> physicalViewNameSet = new HashSet<>();
                // 右孩子不为空，非叶子节点
                Curd condition = tableSeg.getCondition();
                if (leftCurd instanceof TableRep) {
                    String leftView = extractTableName((TableRep) leftCurd);
                    if (leftView != null) {
                        physicalViewNameSet.add(leftView);
                    }
                }
                String rightView = extractTableName(rightCurd);
                if (rightView != null) {
                    physicalViewNameSet.add(rightView);
                }

                if (condition == null) {
                    Curd deleteLogic = createLogic(physicalViewNameSet, userDefinedViewNameSet, EQUAL_ZERO);
                    if (deleteLogic != null) {
                        ReflectUtils.setFieldValue(tableSeg, "condition", deleteLogic);
                    }
                } else {
                    AstMetaData conditionAstMetaData = condition.getAstMetaData();
                    physicalViewNameSet.addAll(getConditionViewNameSet(conditionAstMetaData));
                    Curd deleteLogic = createLogic(physicalViewNameSet, userDefinedViewNameSet, EQUAL_ZERO);
                    if (deleteLogic != null) {
                        deleteLogic = new Logic(condition, new Token(TokenType.AND, "and"), deleteLogic);
                        ReflectUtils.setFieldValue(tableSeg, "condition", deleteLogic);
                    }
                }

            } else {
                // 如果右孩子为空，则表示是叶子节点。
                // 叶子节点不需要处理
            }
        }


        /**
         * 传入的 availableTableNameSet 是表名，但是在SQL中会使用别名，所以这一步是将表名转为别名
         */
        private Set<String> getAvailableViewName(SingleSelectAstMetaData astMetaData) {
            return availableTableNameSet == null ? null :
                    availableTableNameSet.stream()
                            .map(
                                    item -> {
                                        String viewAliasName =
                                                getViewAliasName(astMetaData, new Identifier(new Token(TokenType.IDENTIFIER, item)));
                                        if (viewAliasName == null) {
                                            return item;
                                        }
                                        return viewAliasName;
                                    }
                            ).collect(Collectors.toSet());
        }

        /**
         * @param physicalViewNameSet    SQL里面解析出来的物理表对应的view。（可以计算Logic的集合。）
         * @param userDefinedViewNameSet 用户指定的需要计算Logic的表对应的view。（需要计算Logic的集合）
         */
        private Curd createLogic(Set<String> physicalViewNameSet, Set<String> userDefinedViewNameSet, Curd deleteCondition) {
            Curd result = null;
            for (String tableName : physicalViewNameSet) {
                if (userDefinedViewNameSet == null || userDefinedViewNameSet.contains(tableName)) {
                    Curd deepClone = deleteCondition.deepClone();
                    deepClone.fillTableName(tableName);
                    if (result == null) {
                        result = deepClone;
                    } else {
                        result = new Logic(result, new Token(TokenType.AND, "and"), deepClone);
                    }
                }
            }
            return result;
        }

        @Override
        public Curd visitColumnSeg(ColumnSeg columnSeg) {
            return columnSeg;
        }

        @Override
        public Curd visitColumnRep(ColumnRep columnRep) {
            return columnRep;
        }

        @Override
        public Curd visitTableSeg(TableSeg tableSeg) {
            Curd leftCurd = tableSeg.getLeftCurd();
            leftCurd.accept(this);
            TableRep rightCurd = (TableRep) tableSeg.getRightCurd();
            if (rightCurd != null) {
                rightCurd.accept(this);
            }
            return tableSeg;
        }

        private Set<String> getConditionViewNameSet(AstMetaData astMetaData) {
            // conditionColumnReference 的 Key 是 view，不是table
            Map<String, Set<String>> conditionColumnReference = astMetaData.getConditionColumnReference();
            Set<String> viewNameSet = new HashSet<>(conditionColumnReference.keySet());
            viewNameSet.remove(AstMetaData.UNDETERMINED_TABLE);
            return viewNameSet;
        }

        private String extractTableName(TableRep tableRep) {
            Token alias = tableRep.getAlias();

            // curd 是 identifier 或者 select
            Curd curd = tableRep.getCurd();

            // 当 curd 是 Select 时，这个表不计算
            if (curd instanceof Select) {
                return null;
            }

            if (alias != null) {
                return String.valueOf(alias.getLiteral());
            }
            Identifier table = (Identifier) tableRep.getCurd();
            return String.valueOf(table.getName().getLiteral());
        }

        @Override
        public Curd visitTableRep(TableRep tableRep) {
            Curd curd = tableRep.getCurd();
            curd.accept(this);
            return tableRep;
        }

        @Override
        public Curd visitInCondition(InCondition inCondition) {
            Curd curd = inCondition.getSelect();
            if (curd != null) {
                curd.accept(this);
            }
            return inCondition;
        }

        @Override
        public Curd visitExistsCondition(ExistsCondition existsCondition) {
            Curd curd = existsCondition.getCurd();
            curd.accept(this);
            return existsCondition;
        }

        @Override
        public Curd visitGroupBySeg(GroupBySeg groupBySeg) {
            return groupBySeg;
        }

        @Override
        public Curd visitOrderBySeg(OrderBySeg orderBySeg) {
            return orderBySeg;
        }

        @Override
        public Curd visitLimitSeg(LimitSeg limitSeg) {
            return limitSeg;
        }

        @Override
        public Curd visitGroupFunction(GroupFunction groupFunction) {
            return groupFunction;
        }

        @Override
        public Curd visitCaseWhen(CaseWhen caseWhen) {
            return caseWhen;
        }

        @Override
        public Curd visitInsert(Insert insert) {
            insert.getInsertRep().accept(this);
            return null;
        }

        @Override
        public Curd visitInsertValuesRep(InsertValuesRep insertValuesRep) {
            return null;
        }

        @Override
        public Curd visitInsertSetRep(InsertSetRep insertSetRep) {
            return null;
        }

        @Override
        public Curd visitOnDuplicateKey(OnDuplicateKey onDuplicateKey) {
            return null;
        }

        @Override
        public Curd visitInsertSelectRep(InsertSelectRep insertSelectRep) {
            Curd select = insertSelectRep.getSelect();
            select.accept(this);
            return null;
        }

        @Override
        public Curd visitUpdate(Update update) {
            Curd whereSeg = update.getWhereSeg();
            if (whereSeg != null) {
                whereSeg.accept(this);
            }
            return null;
        }

        @Override
        public Curd visitDelete(Delete delete) {
            Curd whereSeg = delete.getWhereSeg();
            if (whereSeg != null) {
                whereSeg.accept(this);
            }
            return null;
        }

        @Override
        public Curd visitWhereSeg(WhereSeg whereSeg) {
            whereSeg.getLogic().accept(this);
            return whereSeg;
        }

        @Override
        public Curd visitLogic(Logic logic) {
            logic.getLeftCurd().accept(this);

            Curd rightCurd = logic.getRightCurd();
            if (rightCurd != null) {
                rightCurd.accept(this);
            }
            return logic;
        }

        @Override
        public Curd visitComparison(Comparison comparison) {
            comparison.getLeftCurd().accept(this);

            Curd rightCurd = comparison.getRightCurd();
            if (rightCurd != null) {
                rightCurd.accept(this);
            }
            return comparison;
        }

        @Override
        public Curd visitBinaryArithmetic(BinaryArithmetic binaryArithmetic) {
            binaryArithmetic.getLeftCurd().accept(this);
            Curd rightCurd = binaryArithmetic.getRightCurd();
            if (rightCurd != null) {
                rightCurd.accept(this);
            }
            return binaryArithmetic;
        }

        @Override
        public Curd visitUnaryArithmetic(UnaryArithmetic unaryArithmetic) {
            unaryArithmetic.getCurd().accept(this);
            return unaryArithmetic;
        }

        @Override
        public Curd visitLiteral(Literal literal) {
            return literal;
        }

        @Override
        public Curd visitGrouping(Grouping grouping) {
            grouping.getCurd().accept(this);
            return grouping;
        }

        @Override
        public Curd visitIdentifier(Identifier identifier) {
            return identifier;
        }

        @Override
        public Curd visitFunction(Function function) {
            return function;
        }

        @Override
        public Curd visitAssignmentList(AssignmentList assignmentList) {
            return assignmentList;
        }

        @Override
        public Curd visitTimeInterval(TimeInterval timeInterval) {
            return timeInterval;
        }

        @Override
        public Curd visitTimeUnit(TimeUnit timeUnit) {
            return timeUnit;
        }

        @Override
        public Curd visitIsNot(IsNot isNot) {
            return isNot;
        }

        private String getViewAliasName(SingleSelectAstMetaData astMetaData, Curd curd) {
            Map<String, Curd> aliasTableMap = astMetaData.getAliasTableMap();
            Set<Map.Entry<String, Curd>> entries = aliasTableMap.entrySet();
            for (Map.Entry<String, Curd> entry : entries) {
                if (entry.getValue().equals(curd)) {
                    return entry.getKey();
                }
            }
            return null;
        }

        /**
         * 1. from A  ->  A <br/>
         * 2. from A a  ->  a <br/>
         * 3. from (select a from A) B  ->  null <br/>
         * 获取 astMetaData 里物理表对应的view集合，临时表（子查询）会被过滤。
         */
        private Set<String> getPhysicalViewNameSet(SingleSelectAstMetaData astMetaData) {
            // conditionColumnReference 的 Key 是 view，不是table
            Map<String, Set<String>> conditionColumnReference = astMetaData.getConditionColumnReference();
            Set<String> viewNameSet = new HashSet<>(conditionColumnReference.keySet());
            viewNameSet.remove(AstMetaData.UNDETERMINED_TABLE);

            // 查出来所有的不需要执行的
            // key 是 view，value 是表
            Map<String, Curd> aliasTableMap = astMetaData.getAliasTableMap();
            Set<Map.Entry<String, Curd>> entries = aliasTableMap.entrySet();
            for (Map.Entry<String, Curd> next : entries) {
                // 如果 value 是子表，其对应的view（别名）是不需要计算条件的
                if (!next.getValue().getClass().equals(Identifier.class)) {
                    viewNameSet.remove(next.getKey());
                }
            }
            return viewNameSet;
        }

        private boolean checkIsOuterJoinQuery(SingleSelect singleSelect) {
            Queue<Curd> queue = new LinkedList<>();
            queue.offer(singleSelect.getTableSeg());
            while (!queue.isEmpty()) {
                TableSeg tableSeg = (TableSeg) queue.poll();
                Token qualifier = tableSeg.getQualifier();
                if (qualifier != null) {
                    return true;
                }
                Curd leftCurd = tableSeg.getLeftCurd();
                if (leftCurd instanceof TableSeg) {
                    queue.offer(leftCurd);
                }
                Curd rightCurd = tableSeg.getRightCurd();
                if (rightCurd instanceof TableSeg) {
                    queue.offer(leftCurd);
                }
            }
            return false;
        }
    }

}
