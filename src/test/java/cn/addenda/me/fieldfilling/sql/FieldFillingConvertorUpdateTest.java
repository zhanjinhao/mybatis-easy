package cn.addenda.me.fieldfilling.sql;

import cn.addenda.me.fieldfilling.DefaultFieldFillingContext;

/**
 * @author addenda
 * @datetime 2022/8/9 18:24
 */
public class FieldFillingConvertorUpdateTest {

    static String[] sqls = new String[]{

            "update runoob_tbl set runoob_title = replace(runoob_title, 'c++', 'python') where runoob_id = 3",

            "update runoob_tbl set runoob_title = replace(runoob_title, 'c++', 'python')",

            "update runoob_tbl set runoob_title = replace(runoob_title, 'c++', 'python'), a = a+1, b = c",

    };

    public static void main(String[] args) {
        test();
    }

    private static void test() {
        for (String sql : sqls) {
            System.out.println(FieldFillingConvertor.updateFieldFilling(sql, DefaultFieldFillingContext.getInstance()));
        }
    }

}
