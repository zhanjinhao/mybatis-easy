package cn.addenda.me.resulthandler;

/**
 * @Author ISJINHAO
 * @Date 2022/2/5 15:42
 */
public interface MapResultHandlerTestMapper {

    void testStringMapResultHandler(MapResultHandler<String> resultHelper);

    void testLongMapResultHandler(MapResultHandler<Long> resultHelper);

}
