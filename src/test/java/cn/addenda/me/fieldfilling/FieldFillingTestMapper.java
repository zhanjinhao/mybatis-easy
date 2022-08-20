package cn.addenda.me.fieldfilling;

import cn.addenda.me.idfilling.annotation.IdScopeController;
import cn.addenda.me.pojo.TCourse;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author addenda
 * @datetime 2022/8/20 10:18
 */
public interface FieldFillingTestMapper {

    @IdScopeController(mode = IdScopeController.FORCE_INJECT)
    void testInsert(@Param("tCourse") TCourse tCourse);

    void testInsertBatch(@Param("tCourses") List<TCourse> tCourseList);

    void testUpdate(String courseName);

    List<TCourse> testSelect(String courseName);

    void testDelete(String courseName);

}
