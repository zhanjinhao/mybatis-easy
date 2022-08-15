package cn.addenda.me.idfilling.idgenerator;

import java.util.UUID;

/**
 * @Author ISJINHAO
 * @Date 2022/2/4 14:38
 */
public class UUIDIdGenerator implements IdGenerator {

    @Override
    public String nextId(String scopeName) {
        return scopeName + ":" + UUID.randomUUID();
    }

}
