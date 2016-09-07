package com.timguan.common.cache.manager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.timguan.common.cache.common.CacheLoggerUtil;
import com.timguan.common.cache.common.Constants;
import org.slf4j.Logger;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPoolConfig;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * redis集群模式
 * Created by gkq on 16/4/6.
 */
public class RedisClusterCacheManager extends CacheManager {
    private static final Logger logger = CacheLoggerUtil.getLogger();
    private JedisCluster jedisCluster;

    public RedisClusterCacheManager(String namespace, String env) {
        super(namespace, env);
    }

    public RedisClusterCacheManager setJedisCluster(JedisCluster jedisCluster) {
        this.jedisCluster = jedisCluster;
        return this;
    }

    public static class Builder extends AbstractCacheManagerBuilder {
        /**
         * redisHosts
         */
        private String redisServerHosts;

        private Integer redisMaxTotal;

        private Integer redisMinIdle;

        private Integer redisMaxIdle;

        private Integer redisMaxWaitTime;

        private Integer redisTimeout;

        private Integer redisMaxRedirections;

        private Boolean redisTestOnBorrow;

        public Builder(String namespace, String env) {
            super(namespace, env);
        }

        public Builder setRedisServerHosts(String redisServerHosts) {
            this.redisServerHosts = redisServerHosts;
            logger.info("[RedisClusterCacheManager.init]" + Constants.CONFIG_KEY_REDISCLUSTER_HOSTS + "={}", redisServerHosts);
            return this;
        }

        public Builder setRedisMaxTotal(Integer redisMaxTotal) {
            this.redisMaxTotal = redisMaxTotal;
            logger.info("[RedisClusterCacheManager.init]" + Constants.CONFIG_KEY_REDISCLUSTER_MAXTOTAL + "={}", redisMaxTotal);
            return this;
        }

        public Builder setRedisMinIdle(Integer redisMinIdle) {
            this.redisMinIdle = redisMinIdle;
            logger.info("[RedisClusterCacheManager.init]" + Constants.CONFIG_KEY_REDISCLUSTER_MINIDLE + "={}", redisMinIdle);
            return this;
        }

        public Builder setRedisMaxIdle(Integer redisMaxIdle) {
            this.redisMaxIdle = redisMaxIdle;
            logger.info("[RedisClusterCacheManager.init]" + Constants.CONFIG_KEY_REDISCLUSTER_MAXIDLE + "={}", redisMaxIdle);
            return this;
        }

        public Builder setRedisMaxWaitTime(Integer redisMaxWaitTime) {
            this.redisMaxWaitTime = redisMaxWaitTime;
            logger.info("[RedisClusterCacheManager.init]" + Constants.CONFIG_KEY_REDISCLUSTER_MAXWAITTIME + "={}", redisMaxWaitTime);
            return this;
        }

        public Builder setRedisTimeout(Integer redisTimeout) {
            this.redisTimeout = redisTimeout;
            logger.info("[RedisClusterCacheManager.init]" + Constants.CONFIG_KEY_REDISCLUSTER_TIMEOUT + "={}", redisTimeout);
            return this;
        }

        public Builder setRedisMaxRedirections(Integer redisMaxRedirections) {
            this.redisMaxRedirections = redisMaxRedirections;
            logger.info("[RedisClusterCacheManager.init]" + Constants.CONFIG_KEY_REDISCLUSTER_MAXREDIRECTIONS + "={}", redisMaxRedirections);
            return this;
        }

        public Builder setRedisTestOnBorrow(Boolean redisTestOnBorrow) {
            this.redisTestOnBorrow = redisTestOnBorrow;
            logger.info("[RedisClusterCacheManager.init]" + Constants.CONFIG_KEY_REDISCLUSTER_TESTONBORROW + "={}", redisTestOnBorrow);
            return this;
        }

        /**
         * 获取jedisCluster节点信息
         *
         * @param redisServerHosts
         */
        private Set<HostAndPort> getJedisClusterNode(String redisServerHosts) {
            Set<HostAndPort> jedisClusterNode = new HashSet<HostAndPort>();
            String[] hostAndIps = redisServerHosts.split(",|;");
            for (String hostAndIp : hostAndIps) {
                String[] hostAndIpArray = hostAndIp.split(":");
                jedisClusterNode.add(new HostAndPort(hostAndIpArray[0], Integer.parseInt(hostAndIpArray[1])));
            }
            return jedisClusterNode;
        }

        /**
         * jedis连接池配置
         *
         * @param redisMaxTotal     最大连接数
         * @param redisMaxIdle      最大空闲连接数
         * @param redisMinIdle      最小空闲连接数
         * @param redisMaxWaitTime  获取连接时的最大等待毫秒数
         * @param redisTestOnBorrow 在空闲时检查有效性
         */
        private JedisPoolConfig getJedisPoolConfig(int redisMaxTotal,
                int redisMaxIdle,
                int redisMinIdle,
                int redisMaxWaitTime,
                boolean redisTestOnBorrow) {
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            //最大连接数, 默认20个
            poolConfig.setMaxTotal(redisMaxTotal);
            //最大空闲连接数, 默认20个
            poolConfig.setMaxIdle(redisMaxIdle);
            //最小空闲连接数, 默认0
            poolConfig.setMinIdle(redisMinIdle);
            //获取连接时的最大等待毫秒数(如果设置为阻塞时BlockWhenExhausted), 如果超时就抛异常, 小于零:阻塞不确定的时间, 默认 - 1
            poolConfig.setMaxWaitMillis(redisMaxWaitTime);
            //在获取连接的时候检查有效性, 默认false
            poolConfig.setTestOnBorrow(redisTestOnBorrow);
            poolConfig.setTestOnReturn(Boolean.FALSE);
            //逐出连接的最小空闲时间 默认1800000毫秒(30分钟)
            poolConfig.setMinEvictableIdleTimeMillis(1800000);
            //每次逐出检查时 逐出的最大数目 如果为负数就是:1 / abs(n), 默认3
            poolConfig.setNumTestsPerEvictionRun(3);
            //对象空闲多久后逐出, 当空闲时间 > 该值 且 空闲连接>最大空闲数 时直接逐出, 不再根据MinEvictableIdleTimeMillis判断 (默认逐出策略)
            poolConfig.setSoftMinEvictableIdleTimeMillis(1800000);
            //在空闲时检查有效性, 默认true
            poolConfig.setTestWhileIdle(Boolean.TRUE);
            //“空闲链接”检测线程，检测的周期，毫秒数。如果为负值，表示不运行“检测线程”。默认为-1.
            poolConfig.setTimeBetweenEvictionRunsMillis(60000);
            return poolConfig;
        }

        public RedisClusterCacheManager build() {
            JedisCluster jedisCluster = null;
            try {
                Set<HostAndPort> jedisClusterNode = getJedisClusterNode(redisServerHosts);
                JedisPoolConfig poolConfig = getJedisPoolConfig(
                        null != redisMaxTotal ? redisMaxTotal : 1000,
                        null != redisMaxIdle ? redisMaxIdle : 500,
                        null != redisMinIdle ? redisMinIdle : 400,
                        null != redisMaxWaitTime ? redisMaxWaitTime : 500,
                        null != redisTestOnBorrow ? redisTestOnBorrow : false);
                jedisCluster = new JedisCluster(
                        jedisClusterNode,
                        null != redisTimeout ? redisTimeout : 500,
                        null != redisMaxRedirections ? redisMaxRedirections : 2,
                        poolConfig);//超时时间500ms,最多允许2次重定向
            } catch (Exception e) {
                throw new RuntimeException("[RedisClusterCacheManager.init]can't initialize rediscluster client!", e);
            }
            return new RedisClusterCacheManager(getNamespace(), getEnv()).setJedisCluster(jedisCluster);
        }

    }

    @Override
    <T> void innerSet(String key, int exp, T o) {
        jedisCluster.setex(key.getBytes(Constants.UTF8), exp, JSON.toJSONBytes(o, SerializerFeature.WriteClassName));
    }

    @Override
    <T> T innerGet(String key, Class<T> tClass) {
        T result = null;
        String value = jedisCluster.get(key);
        if (value != null) {
            result = JSON.parseObject(value, tClass);
        }
        return result;
    }

    @Override
    void innerDelete(String key) {
        jedisCluster.del(key);
    }

    @Override
    long innerGetCas(String key) {
        return 0;
    }

    @Override
    boolean innerCas(String key, long casValue, int exp, Object value) {
        innerSet(key, exp, value);
        return true;
    }

    @Override
    public void innerClose() {
        if (jedisCluster != null) {
            try {
                jedisCluster.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 锁在给定的等待时间内空闲，则获取锁成功 返回true， 否则返回false，作为阻塞式锁使用
     *
     * @param key     锁键
     * @param timeout 尝试获取锁时长，建议传递500,结合实践单位，则可表示500毫秒
     * @param expire  锁被动失效时间
     *
     * @return 获取锁成功 返回true， 否则返回false
     */
    public boolean innerTryLock(String key, int timeout, int expire) {
        boolean isSuccess = false;
        try {
            //纳秒
            long begin = System.nanoTime();
            do {
                //EX seconds -- Set the specified expire time, in seconds.
                //PX milliseconds -- Set the specified expire time, in milliseconds.
                //NX -- Only set the key if it does not already exist.
                //XX -- Only set the key if it already exist.
                String result = jedisCluster.set(key, key, "NX", "EX", expire);
                if ("OK".equalsIgnoreCase(result)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("[Cache]get {} lock success,set expire {}", key,
                                expire);
                    }
                    isSuccess = true;
                    break;
                }
                if (timeout == 0) {
                    break;
                }
                Thread.sleep(100);
            } while ((System.nanoTime() - begin) < TimeUnit.MILLISECONDS.toNanos(timeout));
            //因超时没有获得锁
        } catch (Throwable e) {
            logger.error("[Cache]try lock failed!", e);
            //当Redis异常时,不阻塞业务流程
            isSuccess = true;
        }
        return isSuccess;
    }

    @Override
    void innerReleaseLock(String key) {
        innerDelete(key);
    }
}
