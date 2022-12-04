package cn.addenda.me.constraint;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author addenda
 * @datetime 2022/11/26 20:56
 */
public class ConstraintContext {

    private ConstraintContext() {
    }

    private static final ThreadLocal<Map<String, String>> TABLE_CONSTRAINT_THREAD_LOCAL = ThreadLocal.withInitial(() -> null);

    private static final ThreadLocal<Map<String, String>> VIEW_CONSTRAINT_THREAD_LOCAL = ThreadLocal.withInitial(() -> null);

    public static void addTableConstraint(String tableName, String condition) {
        Map<String, String> tableConstraintMap = TABLE_CONSTRAINT_THREAD_LOCAL.get();
        if (tableConstraintMap == null) {
            tableConstraintMap = new LinkedHashMap<>();
            TABLE_CONSTRAINT_THREAD_LOCAL.set(tableConstraintMap);
        }
        if (tableConstraintMap.containsKey(tableName)) {
            throw new ConstraintException("table already exists! ");
        }
        tableConstraintMap.put(tableName, condition);
    }

    public static void addViewConstraint(String tableName, String condition) {
        Map<String, String> viewConstraintMap = VIEW_CONSTRAINT_THREAD_LOCAL.get();
        if (viewConstraintMap == null) {
            viewConstraintMap = new LinkedHashMap<>();
            VIEW_CONSTRAINT_THREAD_LOCAL.set(viewConstraintMap);
        }
        if (viewConstraintMap.containsKey(tableName)) {
            throw new ConstraintException("view already exists! ");
        }
        viewConstraintMap.put(tableName, condition);
    }

    public static Map<String, String> getTableConstraints() {
        return TABLE_CONSTRAINT_THREAD_LOCAL.get();
    }

    public static Map<String, String> getViewConstraints() {
        return VIEW_CONSTRAINT_THREAD_LOCAL.get();
    }

    public static void clearTableConstraints() {
        TABLE_CONSTRAINT_THREAD_LOCAL.remove();
    }

    public static void clearViewConstraints() {
        VIEW_CONSTRAINT_THREAD_LOCAL.remove();
    }

    public static void clearConstraint() {
        clearTableConstraints();
        clearViewConstraints();
    }

}
