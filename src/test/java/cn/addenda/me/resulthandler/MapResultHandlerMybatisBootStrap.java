package cn.addenda.me.resulthandler;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.Reader;

/**
 * @Author ISJINHAO
 * @Date 2022/2/3 17:39
 */
public class MapResultHandlerMybatisBootStrap {

    public static void main(String[] args) {
//        testString();
        testLong();
    }

    private static void testString() {
        String resource = "cn/addenda/me/resulthandler/mybatis-config-mapResultHandler.xml";
        Reader reader;
        try {
            reader = Resources.getResourceAsReader(resource);
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
            SqlSession sqlSession = sqlSessionFactory.openSession();

            try {
                MapResultHandlerTestMapper courseMapper = sqlSession.getMapper(MapResultHandlerTestMapper.class);
                MapResultHandler<String> resultHelper = new MapResultHandler<>(true, true);
                courseMapper.testStringMapResultHandler(resultHelper);
                System.out.println(resultHelper.getResult());
            } finally {
                sqlSession.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void testLong() {
        String resource = "cn/addenda/me/resulthandler/mybatis-config-mapResultHandler.xml";
        Reader reader;
        try {
            reader = Resources.getResourceAsReader(resource);
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
            SqlSession sqlSession = sqlSessionFactory.openSession();

            try {
                MapResultHandlerTestMapper courseMapper = sqlSession.getMapper(MapResultHandlerTestMapper.class);
                // 允许重复，先进先出，过滤空
//                MapResultHandler<Long> resultHelper = new MapResultHandler<>(true, true);
                // 不允许重复，先进先出，过滤空
//                MapResultHandler<Long> resultHelper = new MapResultHandler<>(true);
                // 允许重复，先进先出，不过滤空
                MapResultHandler<Long> resultHelper = new MapResultHandler<>(true, false);
                courseMapper.testLongMapResultHandler(resultHelper);
                System.out.println(resultHelper.getResult());
            } finally {
                sqlSession.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
