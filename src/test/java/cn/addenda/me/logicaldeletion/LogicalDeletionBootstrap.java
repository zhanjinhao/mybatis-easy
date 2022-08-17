package cn.addenda.me.logicaldeletion;

import cn.addenda.me.idfilling.IdFillingTestMapper;
import cn.addenda.me.pojo.TCourse;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author addenda
 * @datetime 2022/8/16 21:29
 */
public class LogicalDeletionBootstrap {

    static SqlSessionFactory sqlSessionFactory;

    static {
        String resource = "cn/addenda/me/logicaldeletion/mybatis-config-logicaldeletion.xml";
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
        testInsertBatch();
    }

    private static void testInsert() {
        SqlSession sqlSession = sqlSessionFactory.openSession();
        try {
            LogicalDeletionMapper courseMapper = sqlSession.getMapper(LogicalDeletionMapper.class);
            courseMapper.testInsert(new TCourse("addenda1", "zhanjinhao"));
            courseMapper.testInsert(new TCourse(null, "zhanjinhao"));
            sqlSession.commit();
        } finally {
            sqlSession.close();
        }
    }

    private static void testInsertBatch() {
        SqlSession sqlSession = sqlSessionFactory.openSession();

        try {
            IdFillingTestMapper courseMapper = sqlSession.getMapper(IdFillingTestMapper.class);
            List<TCourse> list = new ArrayList<>();
            list.add(new TCourse(null, "zhanjinhao"));
            list.add(new TCourse(null, "zhanjinhao"));
            courseMapper.testInsertBatch(list);
            sqlSession.commit();
        } finally {
            sqlSession.close();
        }
    }

}
