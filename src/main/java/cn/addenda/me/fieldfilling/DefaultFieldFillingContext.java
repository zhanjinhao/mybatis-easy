package cn.addenda.me.fieldfilling;

/**
 * @Author ISJINHAO
 * @Date 2022/2/1 16:46
 */
public class DefaultFieldFillingContext implements FieldFillingContext {

    private static final ThreadLocal<String> createUserMap = new ThreadLocal<>();
    private static final ThreadLocal<String> modifyUserMap = new ThreadLocal<>();
    private static final ThreadLocal<String> remarkMap = new ThreadLocal<>();

    private static final FieldFillingContext instance = new DefaultFieldFillingContext();

    private DefaultFieldFillingContext() {
    }

    public static FieldFillingContext getInstance() {
        return instance;
    }

    @Override
    public String getCreateUser() {
        return createUserMap.get();
    }

    @Override
    public long getCreateTime() {
        return System.currentTimeMillis();
    }

    @Override
    public String getModifyUser() {
        return modifyUserMap.get();
    }

    @Override
    public long getModifyTime() {
        return System.currentTimeMillis();
    }

    @Override
    public String getRemark() {
        return remarkMap.get();
    }

    @Override
    public void removeCache() {
        createUserMap.remove();
        modifyUserMap.remove();
        remarkMap.remove();
    }

    public static void setCreateUser(String createUser) {
        createUserMap.set(createUser);
    }

    public static void setModifyUser(String modifyUser) {
        modifyUserMap.set(modifyUser);
    }

    public static void setRemark(String remark) {
        remarkMap.set(remark);
    }

}
