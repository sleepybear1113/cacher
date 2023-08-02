package cn.sleepybear.cacher.loader;

/**
 * 需要自行实现，当缓存过期的时候调用
 * @author sleepybear
 * @date 2023/08/02 22:48
 */
@FunctionalInterface
public interface ExpireAction<K> {
    /**
     * 缓存过期时的操作，可以是删除缓存，也可以是重新加载缓存，也可以是其他操作，比如删除对应的本地文件
     * @param key key
     */
    void expireAction(K key);
}
