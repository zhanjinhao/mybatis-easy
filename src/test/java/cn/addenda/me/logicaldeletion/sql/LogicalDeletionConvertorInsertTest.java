package cn.addenda.me.logicaldeletion.sql;

/**
 * @author addenda
 * @datetime 2022/8/9 18:24
 */
public class LogicalDeletionConvertorInsertTest {

    static String[] sqls = new String[]{

            "insert into score(SNO, CNO, DEGREE) values (109, '3-105', 76)",

            "insert into score(SNO, CNO, DEGREE) values (109, '3-105', DEGREE + 76) on duplicate key update SNO = 131, CNO = '4-111', DEGREE = DEGREE_MAX+1",

            "insert into score(SNO, CNO, DEGREE) values (109, '3-105', 76), (109, '3-105', 76), (109, '3-105', 76)",

            "insert into score(SNO, CNO, DEGREE) values (?, '3-105', ?), (109, ?, 76), (?, '3-105', ?)",

            "insert into score set SNO = 109, CNO = '3-105', DEGREE = 76",

            "insert into score set SNO = 109, CNO = date_format(now(), 'yyyy-dd-mm'), DEGREE = DEGREE + 9 * 3",

            "insert ignore into score set SNO = 109, CNO = '3-105', DEGREE = 76",

            "insert ignore into score set SNO = ?, CNO = '3-105', DEGREE = ?",

            "insert ignore into score set SNO = '1387398', CNO = #{cno}, DEGREE = ?",

            "insert into table_listnames (name, address, tele) " +
                    "select * from (select 'rupert', 'somewhere', '022' from dual) tmp " +
                    "where not exists ( " +
                    "    select name from table_listnames where name = 'rupert' " +
                    ") limit 1"

    };

    public static void main(String[] args) {
        test();
    }

    private static void test() {
        for (int i = 0; i < sqls.length; i++) {
            System.out.println(LogicalDeletionConvertor.insertLogically(sqls[i]));
        }
    }

}
