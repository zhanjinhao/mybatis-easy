package cn.addenda.me.pojo;

import cn.addenda.me.idfilling.annotation.IdScope;

/**
 * @Author ISJINHAO
 * @Date 2022/2/5 15:28
 */
@IdScope(scopeName = "TCourse", idFieldName = "courseId")
public class TCourse {

    private String courseId;

    private String courseName;

    public TCourse() {
    }

    public TCourse(String courseId, String courseName) {
        this.courseId = courseId;
        this.courseName = courseName;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

}
