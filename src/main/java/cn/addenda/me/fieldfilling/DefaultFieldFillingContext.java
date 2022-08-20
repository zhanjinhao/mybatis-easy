package cn.addenda.me.fieldfilling;

/**
 * @author addenda
 * @datetime 2022/8/16 20:40
 */
public class DefaultFieldFillingContext implements FieldFillingContext {

    private static final FieldFillingContext instance = new DefaultFieldFillingContext();

    private DefaultFieldFillingContext() {
    }

    public static FieldFillingContext getInstance() {
        return instance;
    }

    @Override
    public String getCreator() {
        return "addenda";
    }

    @Override
    public String getCreatorName() {
        return "ADDENDA";
    }

    @Override
    public long getCreateTime() {
        return System.currentTimeMillis();
    }

    @Override
    public String getModifier() {
        return "addenda";
    }

    @Override
    public String getModifierName() {
        return "ADDENDA";
    }

    @Override
    public long getModifyTime() {
        return System.currentTimeMillis();
    }

    @Override
    public String getRemark() {
        return null;
    }

}
