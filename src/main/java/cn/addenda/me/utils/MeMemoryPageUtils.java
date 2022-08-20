package cn.addenda.me.utils;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/**
 * @author ISJINHAO
 * @date 2021/5/18
 */
public class MeMemoryPageUtils {

    private MeMemoryPageUtils() {
    }

    enum SqlOrder {
        ASC("ASC"), DESC("DESC");
        private String name;

        SqlOrder(String name) {
            this.name = name;
        }
    }

    /**
     * orderBy 的优先级是高于分页的。
     * <p>
     * fieldMapping 的目的是处理数据库字段到属性的映射的。什么意思呢？假如你有一个 orderBy ： AIRPORT_3CODE desc，
     * 这在数据库是可以运行的，但是Java的Comparator需要的是字段属性，所以需要将AIRPORT_3CODE转换为airport3Code。这种映射规则由fieldMapping定义。
     */
    public static <T> PageInfo<T> pageQuery(List<T> info, Integer pageNum, Integer pageSize, String orderBy, UnaryOperator<String> fieldMapping) {
        List<T> order = order(info, orderBy, fieldMapping);
        Page<T> page = page(order, pageNum, pageSize);
        if (page != null) {
            page.setOrderBy(orderBy);
        }
        return new PageInfo<>(page);
    }

    /**
     * 集合分页
     *
     * @param info     集合
     * @param pageNum  当前页
     * @param pageSize 每页数量
     * @param orderBy  排序
     */
    public static <T> PageInfo<T> pageQuery(List<T> info, Integer pageNum, Integer pageSize, String orderBy) {
        return pageQuery(info, pageNum, pageSize, orderBy, true);
    }


    public static <T> PageInfo<T> pageQuery(List<T> info, Integer pageNum, Integer pageSize, String orderBy, boolean isUnderline) {
        if (isUnderline) {
            return pageQuery(info, pageNum, pageSize, orderBy, MeMemoryPageUtils::underlineToCamel);
        }
        return pageQuery(info, pageNum, pageSize, orderBy, null);
    }

    /**
     * 集合分页
     *
     * @param infos    对象集合
     * @param pageNum  当前页
     * @param pageSize 每页数量
     */
    public static <T> Page<T> page(List<T> infos, Integer pageNum, Integer pageSize) {
        if (null == pageNum || pageNum <= 0) {
            pageNum = 1;
        }
        if (null == pageSize || pageSize <= 0) {
            pageSize = 10;
        }
        Page<T> page = new Page<>(pageNum, pageSize);
        page.setTotal(infos.size());
        int size = infos.size();
        int start = Math.min(pageSize * (pageNum - 1), size);
        int end = Math.min(pageNum * pageSize, size);
        infos = infos.subList(start, end);
        page.addAll(infos);
        return page;
    }

    private static final Map<String, Comparator<?>> comparatorMap = new ConcurrentHashMap<>();

    public static <T> List<T> order(List<T> infos, String orderBy, UnaryOperator<String> fieldMapping) {
        Map<String, SqlOrder> orderRulesMap = orderRulesMap(orderBy, fieldMapping);
        if (NO_ORDER == orderRulesMap || (infos == null || infos.isEmpty()) || infos.size() == 1) {
            return infos;
        }
        // 取出泛型的真实类型
        T t = infos.get(0);
        String key = comparatorKey(orderRulesMap, t);
        // 这里warning的问题是，我们存在map中的Comparator是通配符形式的Comparator。
        // 而对于当前代码来说，是一个确定形式的Comparator，所以编译器认为强转不安全。
        // 但是map的key存了泛型的类型信息，这里实际上是安全的，所以我们压制这个警告。
        @SuppressWarnings("unchecked")
        Comparator<T> comparator = (Comparator<T>) comparatorMap.computeIfAbsent(key, s -> createComparator(infos, orderRulesMap));
        infos.sort(comparator);
        if (infos instanceof Page<?>) {
            Page<?> page = (Page<?>) infos;
            page.setOrderBy(orderBy);
        }
        return infos;
    }

    public static <T> List<T> order(List<T> infos, String orderBy, boolean isUnderline) {
        if (isUnderline) {
            return order(infos, orderBy, MeMemoryPageUtils::underlineToCamel);
        }
        return order(infos, orderBy, null);
    }

    private static <T> String comparatorKey(Map<String, SqlOrder> orderRulesMap, T t) {
        if (t == null) {
            throw new MeUtilsException("please make sure the parameter 't' is not null");
        }
        Set<Entry<String, SqlOrder>> entries = orderRulesMap.entrySet();
        StringBuilder sb = new StringBuilder();
        sb.append(t.getClass());
        for (Entry<String, SqlOrder> entry : entries) {
            sb.append(",").append(entry.getKey()).append(" ").append(entry.getValue());
        }
        return sb.toString().toUpperCase();
    }

    private static <T> Comparator<T> createComparator(List<T> infos, Map<String, SqlOrder> orderRulesMap) {
        T t = infos.get(0);
        Class<?> aClass = t.getClass();
        assertOrderByField(aClass, orderRulesMap);

        // Comparator的内部实现采用了反射，所以性能比较弱，一种可能的解决方案是使用 MethodHandle？
        return (o1, o2) -> {
            if (o1 == null && o2 == null) {
                return 0;
            }
            if (o1 == null) {
                return -1;
            }
            if (o2 == null) {
                return 1;
            }
            try {
                Set<Entry<String, SqlOrder>> entries = orderRulesMap.entrySet();
                for (Entry<String, SqlOrder> entry : entries) {
                    Object fieldData1 = getFieldData(aClass, entry.getKey(), o1);
                    Object fieldData2 = getFieldData(aClass, entry.getKey(), o2);
                    Integer integer = compareToEqual(fieldData1, fieldData2);
                    if (integer == 0) {
                        continue;
                    }
                    if (SqlOrder.DESC.equals(entry.getValue())) {
                        return -integer;
                    } else if (SqlOrder.ASC.equals(entry.getValue())) {
                        return integer;
                    }
                }
            } catch (Exception e) {
                throw new MeUtilsException("error occurred on createComparator()", e);
            }
            return 0;
        };
    }

    private static void assertOrderByField(Class<?> aClass, Map<String, SqlOrder> orderRulesMap) {
        Field[] declaredFields = aClass.getDeclaredFields();
        List<String> declaredFieldNames = Arrays.stream(declaredFields).map(Field::getName).collect(Collectors.toList());
        Set<Entry<String, SqlOrder>> entries = orderRulesMap.entrySet();
        for (Entry<String, SqlOrder> entry : entries) {
            if (!declaredFieldNames.contains(entry.getKey())) {
                throw new MeUtilsException("排序字段不是集合属性。集合属性：" + Arrays.deepToString(declaredFields) + ", 当前字段：" + entry.getKey());
            }
        }
    }

    private static final Map<String, SqlOrder> NO_ORDER = new HashMap<>(0);

    private static Map<String, SqlOrder> orderRulesMap(String orderBy, UnaryOperator<String> fieldMapping) {
        String[] orderRules = orderBy.split(",");
        if ((orderBy == null || orderBy.isEmpty()) || orderRules.length == 0) {
            return NO_ORDER;
        }
        Map<String, SqlOrder> orderRulesMap = new LinkedHashMap<>();
        for (String orderRule : orderRules) {
            orderRule = orderRule.trim();
            String[] split = orderRule.split("\\s+");
            if (split.length == 1) {
                if (fieldMapping != null) {
                    orderRulesMap.put(fieldMapping.apply(split[0].trim()), SqlOrder.DESC);
                } else {
                    orderRulesMap.put(split[0].trim(), SqlOrder.DESC);
                }
            } else if (split.length == 2) {
                if (fieldMapping != null) {
                    orderRulesMap.put(fieldMapping.apply(split[0].trim()), SqlOrder.valueOf(split[1].trim().toUpperCase()));
                } else {
                    orderRulesMap.put(split[0].trim(), SqlOrder.valueOf(split[1].trim().toUpperCase()));
                }
            }
        }
        return orderRulesMap;
    }

    private static final Map<String, Field> fieldMap = new HashMap<>();

    private static String fieldKey(Class<?> clazz, String property) {
        return clazz.getName() + ":" + property;
    }

    private static Object getFieldData(Class<?> clazz, String property, Object o) throws Exception {
        Field field = fieldMap.computeIfAbsent(fieldKey(clazz, property), s -> {
            // if the method assertOrderByField is invoked before this method, this method will not run the catch block
            try {
                return clazz.getDeclaredField(property);
            } catch (NoSuchFieldException e) {
                throw new MeUtilsException("找不到对应的字段，字段名：" + property, e);
            }
        });
        if (field == null) {
            return null;
        }
        field.setAccessible(true);
        return field.get(o);
    }

    private static final Method compareTo;

    static {
        try {
            compareTo = Comparable.class.getMethod("compareTo", Object.class);
        } catch (NoSuchMethodException e) {
            throw new MeUtilsException("当前类加载器无法找到java.lang.Comparable#compareTo", e);
        }
    }

    private static Integer compareToEqual(Object o1, Object o2) throws Exception {
        if (o1 == null && o2 == null) {
            return 0;
        }
        if (o1 == null) {
            return -1;
        }
        if (o2 == null) {
            return 1;
        }
        Class<?> leftClass = o1.getClass();
        Class<?> rightClass = o2.getClass();
        if (!leftClass.equals(rightClass)) {
            throw new MeUtilsException("the classes of two object need equal! but now the left is : " + leftClass + ", the right is : " + rightClass);
        }
        if (!Comparable.class.isAssignableFrom(leftClass)) {
            throw new MeUtilsException("please make sure the field to invoke compareTo method is a subtype of Comparator");
        }
        return (Integer) compareTo.invoke(o1, o2);
    }

    public static final char UNDERLINE = '_';

    /**
     * 下划线形式的字段转为驼峰形式
     * AIRPORT_CODE    ->  airportCode
     * AIRPORT__CODE   ->  airportCode
     * AIRPORT_4CODE   ->  airport4Code
     * AIRPORT__4CODE  ->  airport4Code
     */
    public static String underlineToCamel(String param) {
        if ((param == null || param.isEmpty())) {
            return "";
        }
        int len = param.length();
        StringBuilder sb = new StringBuilder(len);
        boolean flag = false;
        for (int i = 0; i < len; i++) {
            char c = param.charAt(i);
            // 如果是两个__连在一起，会认为是一个_
            if (c == UNDERLINE) {
                flag = true;
            } else {
                // 如果_后面跟的是数字，则延迟到下一个字符再消除flag
                if (isDigit(c)) {
                    sb.append(c);
                } else {
                    if (flag) {
                        sb.append(Character.toUpperCase(c));
                        flag = false;
                    } else {
                        sb.append(Character.toLowerCase(c));
                    }
                }
            }
        }
        return sb.toString();
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }


}
