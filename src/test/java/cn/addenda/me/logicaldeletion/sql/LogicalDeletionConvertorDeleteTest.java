package cn.addenda.me.logicaldeletion.sql;

import cn.addenda.ro.grammar.function.evaluator.DefaultFunctionEvaluator;

/**
 * @author addenda
 * @datetime 2022/8/9 18:24
 */
public class LogicalDeletionConvertorDeleteTest {

    static String[] sqls = new String[]{
            "delete from score where CREATE_TM < date_add(now(), interval 1 day) and DEGREE + 1 < 60 - 1",
            "delete from score where DEGREE < 50",
            "delete from score where CREATE_TM < now()",
            "delete from score where DEGREE + 1 < 60 - 1",
            "delete from score"
    };

    public static void main(String[] args) {
        test();
    }

    private static void test() {
        LogicalDeletionConvertor logicalDeletionConvertor = new LogicalDeletionConvertor(DefaultFunctionEvaluator.getInstance());
        for (int i = 0; i < sqls.length; i++) {
            System.out.println(logicalDeletionConvertor.deleteLogically(sqls[i]));
        }
    }

}
