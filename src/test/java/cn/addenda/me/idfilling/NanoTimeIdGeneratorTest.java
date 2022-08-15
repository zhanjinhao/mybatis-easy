package cn.addenda.me.idfilling;

import cn.addenda.me.idfilling.idgenerator.IdGenerator;
import cn.addenda.me.idfilling.idgenerator.NanoTimeIdGenerator;

/**
 * @Author ISJINHAO
 * @Date 2022/2/5 15:34
 */
public class NanoTimeIdGeneratorTest {

    public static void main(String[] args) {
        IdGenerator idGenerator = new NanoTimeIdGenerator();
        System.out.println(idGenerator.nextId("tCourse"));
    }

}
