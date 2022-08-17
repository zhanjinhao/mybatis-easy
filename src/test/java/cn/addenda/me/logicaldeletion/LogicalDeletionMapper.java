package cn.addenda.me.logicaldeletion;

import cn.addenda.me.idfilling.annotation.IdScopeController;
import cn.addenda.me.pojo.TCourse;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Author ISJINHAO
 * @Date 2022/2/5 15:42
 */
public interface LogicalDeletionMapper {

    @IdScopeController(mode = IdScopeController.FORCE_INJECT)
    void testInsert(@Param("tCourse") TCourse tCourse);

    void testInsertBatch(@Param("tCourses") List<TCourse> tCourseList);

}
