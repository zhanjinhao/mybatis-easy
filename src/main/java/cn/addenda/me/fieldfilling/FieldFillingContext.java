package cn.addenda.me.fieldfilling;

/**
 * 实例化FieldFillingContext时默认先找getInstance方法，如果找不到，找无参构造函数
 *
 * @Author ISJINHAO
 * @Date 2022/2/1 16:43
 */
public interface FieldFillingContext {

    String getCreateUser();

    long getCreateTime();

    String getModifyUser();

    long getModifyTime();

    String getRemark();

    void removeCache();

}
