package cn.addenda.me.logicaldeletion.sql;

import cn.addenda.ro.grammar.ast.AstMetaData;
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

class SelectAddDeleteConditionVisitor implements CurdVisitor<Curd> {

    /**
     * 允许被添加 comparison 字段的表名。
     * 为null时表示所有的表名都添加 comparison
     */
    private final Set<String> availableTableNameSet;

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
            deleteCondition = LogicalDeletionConst.EQUAL_ZERO_OR_IS_NULL;
        } else {
            deleteCondition = LogicalDeletionConst.EQUAL_ZERO;
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
                Curd deleteLogic = createLogic(physicalViewNameSet, userDefinedViewNameSet, LogicalDeletionConst.EQUAL_ZERO);
                if (deleteLogic != null) {
                    ReflectUtils.setFieldValue(tableSeg, "condition", deleteLogic);
                }
            } else {
                AstMetaData conditionAstMetaData = condition.getAstMetaData();
                physicalViewNameSet.addAll(getConditionViewNameSet(conditionAstMetaData));
                Curd deleteLogic = createLogic(physicalViewNameSet, userDefinedViewNameSet, LogicalDeletionConst.EQUAL_ZERO);
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
