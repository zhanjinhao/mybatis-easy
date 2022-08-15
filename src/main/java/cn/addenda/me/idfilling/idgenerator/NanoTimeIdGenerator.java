package cn.addenda.me.idfilling.idgenerator;

/**
 * @Author ISJINHAO
 * @Date 2022/2/4 14:38
 */
public class NanoTimeIdGenerator implements IdGenerator {

    @Override
    public String nextId(String scopeName) {
        return scopeName + ":" + System.nanoTime();
    }

}
