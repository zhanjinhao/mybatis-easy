package cn.addenda.me.helper;

import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;

/**
 * @author addenda
 * @datetime 2022/10/9 16:57
 */
public class MybatisBatchOperationHelper {

    private static final Logger logger = LoggerFactory.getLogger(MybatisBatchOperationHelper.class);

    private static final int BATCH_SIZE = 100;

    private final SqlSessionFactory sqlSessionFactory;

    public MybatisBatchOperationHelper(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }

    public <T, U> void batch(Class<T> mapperClass, List<U> data, BiConsumer<T, U> consumer) {
        batch(mapperClass, data, toVoidBiFunction(consumer), getVoidMerger());
    }

    public <T, U> void batch(Class<T> mapperClass, List<U> data, BiConsumer<T, U> consumer, int batchSize) {
        batch(mapperClass, data, toVoidBiFunction(consumer), getVoidMerger(), batchSize);
    }

    public <T, U> void batch(Class<T> mapperClass, List<U> data, BiConsumer<T, U> consumer, String name) {
        batch(mapperClass, data, toVoidBiFunction(consumer), getVoidMerger(), name);
    }

    public <T, U> void batch(Class<T> mapperClass, List<U> data, BiConsumer<T, U> consumer, Integer batchSize, String name) {
        batch(mapperClass, data, toVoidBiFunction(consumer), getVoidMerger(), batchSize, name);
    }

    public <T, U, R> R batch(Class<T> mapperClass, List<U> data, BiFunction<T, U, R> function, BinaryOperator<R> merger) {
        return batch(mapperClass, data, function, merger, BATCH_SIZE, null);
    }

    public <T, U, R> R batch(Class<T> mapperClass, List<U> data, BiFunction<T, U, R> function, BinaryOperator<R> merger, String name) {
        return batch(mapperClass, data, function, merger, BATCH_SIZE, name);
    }

    public <T, U, R> R batch(Class<T> mapperClass, List<U> data, BiFunction<T, U, R> function, BinaryOperator<R> merger, Integer batchSize) {
        return batch(mapperClass, data, function, merger, batchSize, null);
    }

    /**
     * 批量处理 DML 语句
     *
     * @param data        需要被处理的数据
     * @param mapperClass Mybatis的Mapper类
     * @param function    自定义处理逻辑
     * @param merger      合并单次function的接口
     * @param batchSize   flush到db的最大值
     * @param name        给batch操作起个名，方便排查问题
     */
    public <T, U, R> R batch(Class<T> mapperClass, List<U> data, BiFunction<T, U, R> function, BinaryOperator<R> merger, Integer batchSize, String name) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        if (batchSize == null) {
            batchSize = BATCH_SIZE;
        }
        long start = System.currentTimeMillis();
        R pre = null;
        try (SqlSession batchSqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH)) {
            int i = 1;
            T mapper = batchSqlSession.getMapper(mapperClass);
            int size = data.size();
            for (U element : data) {
                R apply = function.apply(mapper, element);
                pre = merger.apply(pre, apply);
                if ((i % batchSize == 0) || i == size) {
                    batchSqlSession.flushStatements();
                }
                i++;
            }
            batchSqlSession.flushStatements();
        }
        if (name == null) {
            logger.info("batch operation execute [{}] ms. ", System.currentTimeMillis() - start);
        } else {
            logger.info("batch {} operation execute [{}] ms. ", name, System.currentTimeMillis() - start);
        }
        return pre;
    }

    private static <T, U> BiFunction<T, U, Void> toVoidBiFunction(BiConsumer<T, U> consumer) {
        return (t, u) -> {
            consumer.accept(t, u);
            return null;
        };
    }

    private static <R> BinaryOperator<R> getVoidMerger() {
        return (r, r2) -> null;
    }

}
