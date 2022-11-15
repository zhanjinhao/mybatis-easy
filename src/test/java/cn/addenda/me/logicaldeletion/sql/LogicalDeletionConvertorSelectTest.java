package cn.addenda.me.logicaldeletion.sql;

import cn.addenda.ro.grammar.function.evaluator.DefaultFunctionEvaluator;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author addenda
 * @datetime 2022/8/9 18:24
 */
public class LogicalDeletionConvertorSelectTest {

    static String[] sqls = new String[]{

            "select a, b"
                    + "  from tab2 t cross join tab3 left join tab3 on tab4.e = tab2.e, (select * from tab5) t5\n"
                    + " where t.m = ?\n"
                    + "   and exists (select 1\n"
                    + "                 from tab4 t4\n"
                    + "                where t1.n = t4.n)\n"
                    + "   and t.tm >= '2016-11-11'",

            "select count(*) as a from t_user where sqc = 1 limit 1 for update",

            "select * from t_a left join t_b on t_a.b_id = t_b.id " +
                    " left join t_c on t_b.c_id = t_c.id",

            "select a, b"
                    + "  from tab2 t left join tab3 on t.c = tab3.c and t.d = t.c, (select * from tab5) t5\n"
                    + " where t.m = ?\n"
                    + "   and exists (select 1\n"
                    + "                 from tab4 t4\n"
                    + "                where t1.n = t4.n)\n"
                    + "   and t.tm >= '2016-11-11'",

            "select T.FLIGHT_ID, ROUTE_TOWARDS\n"
                    + "from T_DISPATCH_FLIGHT_RELEASE RELEASE join\n"
                    + "     (select FLIGHT_ID, max(MODIFY_TM) as LATEAST_TIME\n"
                    + "      from T_DISPATCH_FLIGHT_RELEASE\n"
                    + "      where DELETE_FLAG = 'N'\n"
                    + "        and FLIGHT_ID in (100838874, 100813825)\n"
                    + "      group by FLIGHT_ID) T on RELEASE.ID = T.RID\n"
                    + "where RELEASE.FLIGHT_ID = T.FLIGHT_ID\n"
                    + " and RELEASE.MODIFY_TM = T.LATEAST_TIME order by RELEASE.CREATE_TIME",

            "select ts_user.*, ts_role.create_time, ts_role.create_user from ts_user join ts_role on ts_user.user_id = ts_role.create_user " +
                    "where (select c.a from (select 1 as a from dual) c) > ts_role.a and (select c.a from (select true as a from dual) c) ",

            "select distinct SNO\n" +
                    "from SCORE SCX\n" +
                    "where not exists\n" +
                    "          (\n" +
                    "              select *\n" +
                    "              from SCORE SCY\n" +
                    "              where SCY.SNO = '2002151122' and\n" +
                    "                        not exists\n" +
                    "                        (\n" +
                    "                              select *\n" +
                    "                              from SCORE SCZ\n" +
                    "                              where SCZ.SNO = SCX.Sno and\n" +
                    "                                        SCZ.CNO = SCY.CNO))",

            "select T.FLIGHT_ID, ROUTE_TOWARDS\n"
                    + "from T_DISPATCH_FLIGHT_RELEASE RELEASE,\n"
                    + "     (select FLIGHT_ID, max(MODIFY_TM) as LATEAST_TIME\n"
                    + "      from T_DISPATCH_FLIGHT_RELEASE\n"
                    + "      where DELETE_FLAG = 'N'\n"
                    + "        and FLIGHT_ID in (?, ?)\n"
                    + "      group by FLIGHT_ID) T\n"
                    + "where RELEASE.FLIGHT_ID = T.FLIGHT_ID\n"
                    + " and RELEASE.MODIFY_TM = T.LATEAST_TIME",

            "select T.FLIGHT_ID, ROUTE_TOWARDS\n"
                    + "from T_DISPATCH_FLIGHT_RELEASE RELEASE,\n"
                    + "     (select FLIGHT_ID, max(MODIFY_TM) as LATEAST_TIME\n"
                    + "      from T_DISPATCH_FLIGHT_RELEASE\n"
                    + "      where DELETE_FLAG = 'N'\n"
                    + "        and FLIGHT_ID in (#{flightId1}, #{flightId2}, #{flightId3})\n"
                    + "      group by FLIGHT_ID) T\n"
                    + "where RELEASE.FLIGHT_ID = T.FLIGHT_ID\n"
                    + " and RELEASE.MODIFY_TM = T.LATEAST_TIME",


            "select SNO, SNAME, SBIRTHDAY, date_format(now(), 'yyyy') as anow from STUDENT \n" +
                    "where date_format(SBIRTHDAY, 'yyyy') = (select date_format(SBIRTHDAY, 'yyyy') as t from STUDENT where SNO = '108')",

            "select SNO, CNO from SCORE where DEGREE not in (select max(DEGREE) as maxd from SCORE)",

            "select a.SNO, a.CNO from SCORE a, (select max(DEGREE) as MAX from SCORE) b where b.MAX = SCORE.DEGREE",

            "select 1 from (select a from dual d1 join dual d2 on d1.id = d2.outer_id) t1 where (select 2 from dual) > t1.a",

            "select 1 from dual where (select c as d from t1 where id = 5) > (select a from t2 where id = 6)",

            "select 1 from dual union select 2 from dual union select 3 from dual union select 4 from dual union select 5 from dual",

            "select 1 from dual minus select 2 from dual union select 3 from dual",

            "select * from ts_user " +
                    "where (select c.a from (select true as a from dual) c) and (select c.a from (select true as a from dual) c)",

            "select * from t1, t2 where t1.a = (select b from t2 where t1.c = t2.c)",

            "select * from ts_user where (select c.a from (select true as a from dual) c)",

            "select * from ts_user where (select 1 from dual) = 1",

            "select now() as a from dual",

            "select 1 from dual",

            "select -(1 * SCORE) as score from dual",

            "select * from SCORE where DEGREE > 60 and DEGREE < 80",

            "select * from STUDENT order by CLASS desc",

            "select * from SCORE order by CNO asc, DEGREE desc",

            "select avg(DEGREE) as avgd from SCORE where CNO = '3-105'",

            "select avg(DEGREE) as avgd from SCORE where CNO like '3%' group by CNO having count(CNO) > 5",

            "select SNO from SCORE group by SNO having min(DEGREE) > 70 and max(DEGREE) < 90",

            "select STUDENT.SNAME, SCORE.CNO, SCORE.DEGREE from STUDENT, SCORE where STUDENT.SNO = SCORE.SNO",

            "select STUDENT.SNAME, COURSE.CNAME, SCORE.DEGREE \n" +
                    "from SCORE, COURSE, STUDENT where STUDENT.SNO = SCORE.SNO and SCORE.CNO = COURSE.CNO",

            "select SCORE.SNO, SCORE.CNO, GRADE.RANK \n" +
                    "from SCORE, GRADE where SCORE.DEGREE >= GRADE.LOW and SCORE.DEGREE <= GRADE.UPP",

            "select * from SCORE \n" +
                    "where \n" +
                    "\tSCORE.CNO = '3-105' \n" +
                    "\tand \n" +
                    "\t(select DEGREE from SCORE where SNO = '109' and CNO = '3-105') < SCORE.DEGREE",

            "select * from SCORE \n" +
                    "where \n" +
                    "\tSCORE.CNO = ? \n" +
                    "\tand \n" +
                    "\t(select DEGREE from SCORE where SNO = '?' and CNO = '3-105') < SCORE.DEGREE",

            "select * \n" +
                    "from SCORE, (select SNO, max(DEGREE) as MAXDEGREE from SCORE group by SNO having count(*) > 1) b \n" +
                    "where b.SNO = a.SNO and b.MAXDEGREE > a.DEGREE",

            "select * from SCORE a \n" +
                    "where \n" +
                    "\t((select max(b.DEGREE) as maxd from SCORE b where a.SNO = b.SNO) > a.DEGREE) \n" +
                    "\tand \n" +
                    "\t(a.SNO in (select c.SNO from SCORE group by c.SNO having count(*) > 1))",

            "select * from SCORE where (select DEGREE from SCORE where SNO = '109' and CNO = '3-105') < DEGREE",

            "select * from SCORE \n" +
                    "where \n" +
                    "\tCNO in \n" +
                    "\t(select CNO from COURSE where (select TNO from TEACHER where TNAME = '张旭') = COURSE.TNO)",

            "select TNAME from TEACHER \n" +
                    "where \n" +
                    "\tTNO in \n" +
                    "\t(select TNO from COURSE where CNO in (select CNO from SCORE group by CNO having count(*) > 5))",

            "select * from SCORE a where a.CNO = '3-105' " +
                    "and a.DEGREE >= (select min(DEGREE) as c from SCORE b where b.CNO = '3-245') order by DEGREE desc",

            "select SNO, a.CNO, DEGREE from SCORE a, (select avg(DEGREE) as AVGG, CNO from SCORE group by CNO) b \n" +
                    "where a.CNO = b.CNO and a.DEGREE < b.AVGG",

            "select * from SCORE a where (select avg(b.DEGREE) as a from SCORE b where a.CNO = b.CNO) > a.DEGREE",

            "select TNAME, DEPART from TEACHER where TEACHER.TNO not in (select distinct TNO from COURSE)",

            "select * from STUDENT \n" +
                    "where \n" +
                    "\tSSEX = (select SSEX from STUDENT where SNAME = '李军') \n" +
                    "\tand \n" +
                    "\tCLASS = (select CLASS from STUDENT where SNAME = '李军') \n" +
                    "\tand \n" +
                    "\tSNAME != '李军'",

            "select * from SCORE a \n" +
                    "where (select count(*) as a from SCORE b where a.CNO = b.CNO and a.DEGREE < b.DEGREE) < 2",

            "select * from SCORE minus \n" +
                    "select * \n" +
                    "from SCORE a where (select count(*) as a from SCORE b where a.CNO = b.CNO and a.DEGREE < b.DEGREE) < 1",

            "select SNAME\n" +
                    "from  STUDENT\n" +
                    "where exists\n" +
                    "(\n" +
                    "      select *\n" +
                    "      from SCORE\n" +
                    "      where SNO = STUDENT.SNO and CNO='1'\n" +
                    ")",

            "select * from (select 'rupert', 'somewhere', '022' from dual) tmp " +
                    "where not exists ( " +
                    "    select name from table_listnames where name = 'rupert' " +
                    ") limit 1",

            "select * from (select 'rupert', 'somewhere', '022' from dual) tmp " +
                    "where not exists ( " +
                    "    select name from table_listnames where name = 'rupert' and 1 = 2 " +
                    ") limit 1",


            "select * from A where a is null",

            "select case a when b+1 then '1' when b+2 then '2' else '3' end as A from (select 2 as a, 1 as b from dual) A",

            "select t1.*, t2.* from t1, t2 where t1.id = t2.outerId"


    };

    public static void main(String[] args) {
        test();

//        testSubTable_SubQuery_TwoTableLeftJoin_TwoLogicTable_AllTwoTable();
//        testTwoTableLeftJoin_TwoLogicTable_AllThreeTable();
    }

    private static void test() {
        LogicalDeletionConvertor logicalDeletionConvertor = new LogicalDeletionConvertor(DefaultFunctionEvaluator.getInstance());
        for (int i = 0; i < sqls.length; i++) {
            System.out.println(logicalDeletionConvertor.selectLogically(sqls[i]));
        }
    }


    private static void testSubTable_SubQuery_TwoTableLeftJoin_TwoLogicTable_AllTwoTable() {
        String sql =
                "select * " +
                        "from (select * from A) a left join B b on a.id = b.aid " +
                        "where a.id in (select aid from c)";
        LogicalDeletionConvertor logicalDeletionConvertor = new LogicalDeletionConvertor(DefaultFunctionEvaluator.getInstance());
        System.out.println(logicalDeletionConvertor.selectLogically(sql, asSet("b")));
    }


    public static void testTwoTableLeftJoin_TwoLogicTable_AllThreeTable() {

        String sql = "select\n"
                + "           ts_group.id as group_id,\n"
                + "           ts_group.create_time,\n"
                + "           ts_group.create_user,\n"
                + "           ts_group.description as group_desc,\n"
                + "           ts_group.name        as group_name\n"
                + "    from ts_group\n"
                + "           left join ts_group_role on ts_group.id = ts_group_role.group_id\n"
                + "           left join ts_role on ts_role.ROLE_ID = ts_group_role.role_id\n"
                + "    where ts_group.name like concat('%', #{groupName,jdbcType=VARCHAR}, '%')\n"
                + "      and (ts_role.ROLE_NAME is null or ts_role.ROLE_NAME like concat('%', #{roleName,jdbcType=VARCHAR}, '%'))\n"
                + "      group by ts_group.id";

        LogicalDeletionConvertor logicalDeletionConvertor = new LogicalDeletionConvertor(DefaultFunctionEvaluator.getInstance());
        System.out.println(logicalDeletionConvertor.selectLogically(sql, asSet("ts_group_role", "ts_group", "ts_role")));

        /**
         * correct result:
         *
         * select *
         * from ts_group
         *          left join ts_group_role on ts_group.id = ts_group_role.group_id and ts_group_role.del_fg = 0
         *          left join ts_role on ts_role.ROLE_ID = ts_group_role.role_id
         * where ts_group.del_fg = 0 and ts_group.name like concat('%', '', '%')
         *   and (ts_role.ROLE_NAME is null or ts_role.ROLE_NAME like concat('%', '', '%'))
         *
         *
         * select *
         * from ts_group
         *        left join ts_group_role on ts_group.id = ts_group_role.group_id
         *        left join ts_role on ts_role.ROLE_ID = ts_group_role.role_id
         * where ts_group.name like concat('%', '', '%')
         *   and (ts_role.ROLE_NAME is null or ts_role.ROLE_NAME like concat('%', '', '%'))
         *   and (ts_group_role.del_fg = 0 or ts_group_role.del_fg is null)
         *   and ts_group.del_fg = 0
         */
    }


    private static Set<String> asSet(String... items) {
        return new HashSet<>(Arrays.asList(items));
    }

}
