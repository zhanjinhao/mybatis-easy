package cn.addenda.me.fieldfilling.sql;

import cn.addenda.me.fieldfilling.FieldFillingException;
import cn.addenda.me.fieldfilling.entity.BaseEntity;
import cn.addenda.ro.grammar.ast.expression.*;
import cn.addenda.ro.grammar.ast.retrieve.*;
import cn.addenda.ro.grammar.ast.retrieve.visitor.SelectVisitor;
import cn.addenda.ro.grammar.lexical.token.Token;
import cn.addenda.ro.grammar.lexical.token.TokenType;

import java.lang.reflect.Field;
import java.util.*;

/**
 * @author addenda
 * @datetime 2022/8/18 18:28
 */
class SelectReturnBaseEntityColumnVisitor extends SelectVisitor<List<ColumnRep>> {

    private static final Set<String> COLUMN_NAME_SET = new HashSet<>();
    private static final List<ColumnRep> COLUMN_REP_LIST = new ArrayList<>();

    static {
        Class<BaseEntity> baseEntityClass = BaseEntity.class;
        Field[] declaredFields = baseEntityClass.getDeclaredFields();
        for (Field field : declaredFields) {
            String name = field.getName();
            String columnName = camelCaseToSnakeCase(name);
            COLUMN_NAME_SET.add(columnName);
            COLUMN_REP_LIST.add(new BaseEntityColumnRep(
                    new Identifier(new Token(TokenType.STRING, columnName)), new Token(TokenType.STRING, columnName)));
        }
    }

    private static String camelCaseToSnakeCase(String camelCase) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < camelCase.length(); i++) {
            char ch = camelCase.charAt(i);
            if (Character.isUpperCase(ch)) {
                builder.append("_");
            }
            builder.append(Character.toLowerCase(ch));
        }
        return builder.toString();
    }

    private List<ColumnRep> getColumnRepList(String prefix) {
        return getColumnRepList(COLUMN_REP_LIST, prefix);
    }

    private List<ColumnRep> getColumnRepList(List<ColumnRep> columnRepList, String prefix) {
        List<ColumnRep> columnRepListWithPrefix = new ArrayList<>();
        for (ColumnRep columnRep : columnRepList) {
            columnRepListWithPrefix.add(new BaseEntityColumnRep((Identifier) columnRep.getCurd(),
                    new Token(TokenType.STRING, columnRep.getOperator().getLiteral()), prefix));
        }
        return columnRepListWithPrefix;
    }

    /**
     * 允许被添加 comparison 字段的表名。
     * 为null时表示所有的表名都添加 comparison
     */
    private final Set<String> availableTableNameSet;

    public SelectReturnBaseEntityColumnVisitor(Set<String> availableTableNameSet) {
        this.availableTableNameSet = availableTableNameSet;
        if (this.availableTableNameSet != null) {
            this.availableTableNameSet.remove("dual");
        }
    }

    @Override
    public List<ColumnRep> visitSelect(Select select) {
        List<ColumnRep> tokenList = new ArrayList<>(nullAccept(select.getLeftCurd()));
        List<ColumnRep> rightTokenList = nullAccept(select.getRightCurd());
        if (rightTokenList == null) {
            return tokenList;
        }
        if (rightTokenList.size() != tokenList.size()) {
            clearColumnRep((SingleSelect) select.getLeftCurd());
            clearColumnRep((SingleSelect) select.getRightCurd());
            return new ArrayList<>();
        }

        int size = rightTokenList.size();
        for (int i = 0; i < size; i++) {
            ColumnRep leftColumnRep = tokenList.get(i);
            ColumnRep rightColumnRep = rightTokenList.get(i);
            if (!String.valueOf(leftColumnRep.getOperator().getLiteral()).equals(String.valueOf(rightColumnRep.getOperator().getLiteral()))) {
                clearColumnRep((SingleSelect) select.getLeftCurd());
                clearColumnRep((SingleSelect) select.getRightCurd());
                return new ArrayList<>();
            }
        }
        select.reSetAstMetaData();
        return tokenList;
    }

    private void clearColumnRep(SingleSelect singleSelect) {
        ColumnSeg columnSeg = (ColumnSeg) singleSelect.getColumnSeg();
        List<Curd> columnRepList = columnSeg.getColumnRepList();
        columnRepList.removeIf(BaseEntityColumnRep.class::isInstance);
    }

    @Override
    public List<ColumnRep> visitSingleSelect(SingleSelect singleSelect) {
        // 当 columnRepList 只有一个 * 的时候，不进行sql重写。
        ColumnSeg columnSeg = (ColumnSeg) singleSelect.getColumnSeg();
        List<Curd> columnRepList = columnSeg.getColumnRepList();
        for (Curd columnRepCurd : columnRepList) {
            ColumnRep columnRep = (ColumnRep) columnRepCurd;
            Curd curd = columnRep.getCurd();
            if (curd instanceof Identifier) {
                // * 不能存在别名
                Token token = ((Identifier) curd).getName();
                if (TokenType.STAR.equals(token.getType())
                        || "*".equals(extractColumnName(String.valueOf(token.getLiteral())))) {
                    throw new FieldFillingException("cannot support 'select *' or 'select tableName.*' grammar");
                }
            }
        }

        List<ColumnRep> injectedColumnList;

        // 当存在group by时：
        //  需要注入的列从 groupBySeg 里面取
        GroupBySeg groupBySeg = (GroupBySeg) singleSelect.getGroupBySeg();
        if (groupBySeg != null) {
            injectedColumnList = groupBySeg.accept(this);
        }
        // 不存在group by时：
        // 需要注入的列从 tableSeg 里面取
        else {
            injectedColumnList = singleSelect.getTableSeg().accept(this);
        }

        if (!injectedColumnList.isEmpty()) {
            columnRepList.addAll(injectedColumnList);
        }

        return injectedColumnList;
    }

    @Override
    public List<ColumnRep> visitColumnSeg(ColumnSeg columnSeg) {
        return null;
    }

    @Override
    public List<ColumnRep> visitColumnRep(ColumnRep columnRep) {
        return null;
    }

    @Override
    public List<ColumnRep> visitTableSeg(TableSeg tableSeg) {
        List<ColumnRep> tokenList = new ArrayList<>(nullAccept(tableSeg.getLeftCurd()));
        List<ColumnRep> rightTokenList = nullAccept(tableSeg.getRightCurd());
        if (rightTokenList != null) {
            tokenList.addAll(rightTokenList);
        }
        return tokenList;
    }

    @Override
    public List<ColumnRep> visitTableRep(TableRep tableRep) {
        Curd curd = tableRep.getCurd();
        Token alias = tableRep.getAlias();
        String prefix;
        if (alias != null) {
            prefix = String.valueOf(alias.getLiteral());
            // 对应 Identifier alias 场景
            // 如果 Identifier 对应的表是BaseEntity，以alias作为prefix生成注入列
            if (curd instanceof Identifier) {
                Token name = ((Identifier) curd).getName();
                String tableName = String.valueOf(name.getLiteral());
                if (contains(tableName)) {
                    return getColumnRepList(prefix);
                } else {
                    return new ArrayList<>();
                }
            }
            // 对应的 Select alias 场景
            // 以 Select 返回的结果 + alias作为prefix生成注入列
            else {
                List<ColumnRep> accept = curd.accept(this);
                if (accept.isEmpty()) {
                    return new ArrayList<>();
                }
                return getColumnRepList(accept, prefix);
            }
        }
        // 对于 Identifier 场景
        // 如果 Identifier 对应的表是BaseEntity，以 Identifier 作为 prefix 生成注入列
        else {
            Token name = ((Identifier) curd).getName();
            String tableName = String.valueOf(name.getLiteral());
            if (contains(tableName)) {
                return getColumnRepList(tableName);
            } else {
                return new ArrayList<>();
            }
        }
    }

    private boolean contains(String tableName) {
        if ("dual".equals(tableName.toLowerCase(Locale.ROOT))) {
            return false;
        }
        return availableTableNameSet == null || availableTableNameSet.contains(tableName);
    }

    @Override
    public List<ColumnRep> visitInCondition(InCondition inCondition) {
        return null;
    }

    @Override
    public List<ColumnRep> visitExistsCondition(ExistsCondition existsCondition) {
        return null;
    }

    /**
     * 如果 group by 的字段全部都是 baseEntity 的字段，进行sql重写。
     * 否则 不进行sql重写。
     *
     * @param groupBySeg
     * @return
     */
    @Override
    public List<ColumnRep> visitGroupBySeg(GroupBySeg groupBySeg) {
        List<Token> columnList = groupBySeg.getColumnList();
        for (Token column : columnList) {
            String literal = String.valueOf(column.getLiteral());
            String columnName = extractColumnName(literal);
            if (!COLUMN_NAME_SET.contains(columnName)) {
                return new ArrayList<>();
            }
        }
        List<ColumnRep> columnRepList = new ArrayList<>();
        for (Token token : columnList) {
            columnRepList.add(
                    new BaseEntityColumnRep(new Identifier(token.deepClone()), new Token(TokenType.STRING, String.valueOf(token.getLiteral()))));
        }
        return columnRepList;
    }

    private String extractColumnName(String value) {
        int i = value.indexOf(".");
        if (i == -1) {
            return value;
        }
        return value.substring(i + 1);
    }

    @Override
    public List<ColumnRep> visitOrderBySeg(OrderBySeg orderBySeg) {
        return null;
    }

    @Override
    public List<ColumnRep> visitOrderItem(OrderItem orderItem) {
        return null;
    }

    @Override
    public List<ColumnRep> visitLimitSeg(LimitSeg limitSeg) {
        return null;
    }

    @Override
    public List<ColumnRep> visitGroupFunction(GroupFunction groupFunction) {
        return null;
    }

    @Override
    public List<ColumnRep> visitGroupConcat(GroupConcat groupConcat) {
        return null;
    }

    @Override
    public List<ColumnRep> visitCaseWhen(CaseWhen caseWhen) {
        return null;
    }

    @Override
    public List<ColumnRep> visitSLock(SLock sLock) {
        return null;
    }

    @Override
    public List<ColumnRep> visitXLock(XLock xLock) {
        return null;
    }

    @Override
    public List<ColumnRep> visitFrameEdge(FrameEdge frameEdge) {
        return null;
    }

    @Override
    public List<ColumnRep> visitFrameBetween(FrameBetween frameBetween) {
        return null;
    }

    @Override
    public List<ColumnRep> visitDynamicFrame(DynamicFrame dynamicFrame) {
        return null;
    }

    @Override
    public List<ColumnRep> visitWindow(Window window) {
        return null;
    }

    @Override
    public List<ColumnRep> visitWindowFunction(WindowFunction windowFunction) {
        return null;
    }

    @Override
    public List<ColumnRep> visitWhereSeg(WhereSeg whereSeg) {
        return null;
    }

    @Override
    public List<ColumnRep> visitLogic(Logic logic) {
        return null;
    }

    @Override
    public List<ColumnRep> visitComparison(Comparison comparison) {
        return null;
    }

    @Override
    public List<ColumnRep> visitBinaryArithmetic(BinaryArithmetic binaryArithmetic) {
        return null;
    }

    @Override
    public List<ColumnRep> visitUnaryArithmetic(UnaryArithmetic unaryArithmetic) {
        return null;
    }

    @Override
    public List<ColumnRep> visitLiteral(Literal literal) {
        return null;
    }

    @Override
    public List<ColumnRep> visitGrouping(Grouping grouping) {
        return null;
    }

    @Override
    public List<ColumnRep> visitIdentifier(Identifier identifier) {
        return null;
    }

    @Override
    public List<ColumnRep> visitFunction(Function function) {
        return null;
    }

    @Override
    public List<ColumnRep> visitTimeInterval(TimeInterval timeInterval) {
        return null;
    }

    @Override
    public List<ColumnRep> visitTimeUnit(TimeUnit timeUnit) {
        return null;
    }

    @Override
    public List<ColumnRep> visitIsNot(IsNot isNot) {
        return null;
    }

    private static class BaseEntityColumnRep extends ColumnRep {

        public BaseEntityColumnRep(Identifier identifier, Token operator) {
            super(identifier, new Token(TokenType.STRING, String.valueOf(operator.getLiteral()).replace(".", "_")));
        }

        public BaseEntityColumnRep(Identifier identifier, Token operator, String prefix) {
            super(new Identifier(new Token(identifier.getName().getType(), prefix + "." + String.valueOf(identifier.getName().getLiteral()).replace(".", "_"))),
                    new Token(TokenType.STRING, prefix + "_" + operator.getLiteral()));
        }

    }

}
