package cn.addenda.me.fieldfilling;

/**
 * 实例化FieldFillingContext时默认先找getInstance方法，如果找不到，找无参构造函数
 *
 * @author addenda
 * @datetime 2022/8/16 20:40
 */
public interface FieldFillingContext {

    String getCreator();

    String getCreatorName();

    Object getCreateTime();

    String getModifier();

    String getModifierName();

    Object getModifyTime();

    String getRemark();

}
