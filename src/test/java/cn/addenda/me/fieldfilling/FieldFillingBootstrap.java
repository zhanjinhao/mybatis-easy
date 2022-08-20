package cn.addenda.me.fieldfilling;

import cn.addenda.me.pojo.TCourse;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author addenda
 * @datetime 2022/8/17 22:59
 */
public class FieldFillingBootstrap {

    static SqlSessionFactory sqlSessionFactory;

    static {
        String resource = "cn/addenda/me/fieldfilling/mybatis-config-fieldfilling.xml";
        Reader reader = null;
        try {
            reader = Resources.getResourceAsReader(resource);
        } catch (IOException e) {
            e.printStackTrace();
        }
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
    }

    public static void main(String[] args) {
        testInsert();
        testInsert();


        testInsertBatch();
        testInsertBatch();
        testBatchInsert();
        testBatchInsert();

        testUpdate();
        testBatchUpdate();

        testDelete();
        testBatchDelete();

        testSelect();
    }

    private static void testInsert() {
        SqlSession sqlSession = sqlSessionFactory.openSession();
        try {
            FieldFillingTestMapper courseMapper = sqlSession.getMapper(FieldFillingTestMapper.class);
            courseMapper.testInsert(new TCourse("addenda1", "testInsert1"));
            courseMapper.testInsert(new TCourse(null, "testInsert2"));
            sqlSession.commit();
        } finally {
            sqlSession.close();
        }
    }

    private static void testInsertBatch() {
        SqlSession sqlSession = sqlSessionFactory.openSession();

        try {
            FieldFillingTestMapper courseMapper = sqlSession.getMapper(FieldFillingTestMapper.class);
            List<TCourse> list = new ArrayList<>();
            list.add(new TCourse(null, "testInsertBatch1"));
            list.add(new TCourse(null, "testInsertBatch2"));
            courseMapper.testInsertBatch(list);
            sqlSession.commit();
        } finally {
            sqlSession.close();
        }
    }

    private static void testBatchInsert() {
        try (SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH)) {
            FieldFillingTestMapper courseMapper = sqlSession.getMapper(FieldFillingTestMapper.class);

            courseMapper.testInsert(new TCourse(null, "testBatchInsert1"));
            sqlSession.flushStatements();

            courseMapper.testInsert(new TCourse(null, "testBatchInsert2"));
            sqlSession.commit();
        }
    }

    private static void testSelect() {
        try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
            FieldFillingTestMapper courseMapper = sqlSession.getMapper(FieldFillingTestMapper.class);
            final List<TCourse> list = courseMapper.testSelect("testInsert1");
            for (TCourse tCourse : list) {
                System.out.println(tCourse);
            }
        }
    }

    private static void testUpdate() {
        try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
            FieldFillingTestMapper courseMapper = sqlSession.getMapper(FieldFillingTestMapper.class);
            courseMapper.testUpdate("testInsert1");
            courseMapper.testUpdate("testInsert2");
            sqlSession.commit();
        }
    }

    private static void testBatchUpdate() {
        try (SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH)) {
            FieldFillingTestMapper courseMapper = sqlSession.getMapper(FieldFillingTestMapper.class);

            courseMapper.testUpdate("testBatchInsert1");
            sqlSession.flushStatements();

            courseMapper.testUpdate("testBatchInsert2");
            sqlSession.commit();
        }
    }

    private static void testDelete() {
        try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
            FieldFillingTestMapper courseMapper = sqlSession.getMapper(FieldFillingTestMapper.class);
            courseMapper.testDelete("testInsertBatch1");
            courseMapper.testDelete("testInsertBatch2");
            sqlSession.commit();
        }
    }

    private static void testBatchDelete() {
        try (SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH)) {
            FieldFillingTestMapper courseMapper = sqlSession.getMapper(FieldFillingTestMapper.class);

            courseMapper.testDelete("testBatchInsert11");
            sqlSession.flushStatements();

            courseMapper.testDelete("testBatchInsert21");
            sqlSession.commit();
        }
    }

}
