package cn.addenda.me.logicaldeletion.sql;

import cn.addenda.ro.grammar.function.evaluator.DefaultFunctionEvaluator;

/**
 * @author addenda
 * @datetime 2022/8/9 18:24
 */
public class LogicalDeletionConvertorUpdateTest {

    static String[] sqls = new String[]{

            "update runoob_tbl set runoob_title = replace(runoob_title, 'c++', 'python') where runoob_id = 3",

            "update runoob_tbl set runoob_title = replace(runoob_title, 'c++', 'python')",

            "update runoob_tbl set runoob_title = replace(runoob_title, 'c++', 'python'), a = a+1, b = c",

    };

    public static void main(String[] args) {
        test();
    }

    private static void test() {
        LogicalDeletionConvertor logicalDeletionConvertor = new LogicalDeletionConvertor(DefaultFunctionEvaluator.getInstance());
        for (int i = 0; i < sqls.length; i++) {
            System.out.println(logicalDeletionConvertor.updateLogically(sqls[i]));
        }
    }

}
