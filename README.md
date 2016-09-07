#缓存工具
##功能:缓存使用封装,针对页面级缓存以及数据级缓存进行了不同封装
##使用
###1.页面缓存,使用了二级缓存方式(本地缓存,分布式缓存),通过Filter方式配置
####1.1 本地缓存配置
**本地缓存Filter配置demo**

```
	<filter>
	    <filter-name>localHtmlCacheConfig</filter-name>
	    <filter-class>LocalHtmlCacheFilter
	    </filter-class>
	    <!--指定cache配置-->
	    <init-param>
	        <param-name>cacheName</param-name>
	        <param-value>LOCALCACHE</param-value>
	    </init-param>
	    <!--指定ehcache配置文件路径-->
	    <init-param>
	        <param-name>ehcacheConfigLocation</param-name>
	        <param-value>/web-ehcacheConfig.xml</param-value>
	    </init-param>
	</filter>
```
**ehcacheConfig.xml配置demo**

```
	<ehcache xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:noNamespaceSchemaLocation="ehcache.xsd">
    	<cache name="LOCALCACHE"
           maxEntriesLocalHeap="2048"
           eternal="false"
           timeToIdleSeconds="10"
           timeToLiveSeconds="10"
           transactionalMode="off">
        	<persistence strategy="localTempSwap"/>
    	</cache>
	</ehcache>
```

####1.2 分布式缓存配置
**filter配置示例**

```
<filter>
    <filter-name>remoteHtmlCache</filter-name>
    <filter-class>RemoteHtmlCacheFilter</filter-class>
    <!--指定缓存配置文件-->
    <init-param>
        <param-name>cache.configFileLocation</param-name>
        <param-value>/service-config.properties</param-value>
    </init-param>
</filter>
```
分布式缓存目前提供了memcache/rediscluster作为缓存实现,通过逻辑失效和物理失效+重入锁机制避免雪崩效应和数据更新问题

**service-config.properties demo**

```
##数据缓存使用,制定数据缓存静态配置文件
com.timguan.commoncache.cache.service.staticconfigfile=/service-cacheConfig.xml
##数据缓存使用,制定数据缓存动态配置GROUPID
com.timguan.commoncache.cache.service.dynamicconfiggroupId=CMS
##数据缓存使用,制定数据缓存动态配置DATAID
com.timguan.commoncache.cache.service.dynamicconfigdataid=com.timguan.b2cmall.service.cache.config
##页面缓存使用,制定数据缓存静态配置文件
com.timguan.commoncache.cache.web.staticconfigfile=/web-cacheConfig.xml
##页面缓存使用,制定数据缓存动态配置DATAID
com.timguan.commoncache.cache.web.dynamicconfiggroupId=CMS
##页面缓存使用,制定数据缓存动态配置GROUPID
com.timguan.commoncache.cache.web.dynamicconfigdataid=com.timguan.b2cmall.web.cache.config
##缓存命名空间,各个系统区分
com.timguan.commoncache.cache.namespace=cms
##环境信息,用于区分环境
dubbo.reference.version=DEV3
##缓存实现机制 memcache redis(读写分离目前性能存在问题不推荐使用) rediscluster
com.timguan.commoncache.cache.cachemanager=rediscluster
com.timguan.commoncache.cache.memcache.host=10.32.156.151
com.timguan.commoncache.cache.memcache.port=11211
##redis sentinel 配置,格式a.b.c.d:p,a2.b2.c2.d2:p2
com.timguan.commoncache.cache.redis.sentienls=10.32.156.154:26379
##目前采用SLB做读操作的负载均衡，此处指向的其实是SLB而非真实的redis节点
com.timguan.commoncache.cache.redis.readonly=10.32.156.154:6379
com.timguan.commoncache.cache.redis.mastername=mymaster
##redis cluster nodes 配置,格式a.b.c.d:p,a2.b2.c2.d2:p2
com.timguan.commoncache.cache.rediscluster.hosts=10.32.156.150:6379,10.32.156.151:6379,10.32.156.152:6379
```
**页面静态配置demo**

```
	<!--读取cache配置文件，搭配动态cache配置（动态调整cache的时间 lockSize等,必要场景才会使用到）
新增加一项cache需要进行一项静态配置包含cacheKey expire version，每次修改缓存数据的数据结构都需要对版本号进行修改。
对某项缓存有变更的需求，才需要在diamond上进行配置（注意版本号是不允许动态配置去覆盖静态配置的）。页面级cache如果动态的增加了
一项静态文件中不具备的cache配置，务必再静态文件中补上-->
<cacheConfig>
    <htmlCacheConfig>
        <urlPattern>/index.html</urlPattern>
        <expire>30</expire>
        <maxExpire>3600</maxExpire>
        <cacheKey>INDEX</cacheKey>
        <version>2</version>
        <lockSize>1</lockSize>
    </htmlCacheConfig>
</cacheConfig>
```
**动态配置demo**

```
<cacheConfig>
    <htmlCacheConfig>
        <urlPattern>/index.html</urlPattern>
        <expire>300</expire>
        <maxExpire>1800</maxExpire>
       	<cacheKey>INDEX</cacheKey>
       	<lockSize>1</lockSize>
    </htmlCacheConfig>
<cacheConfig>
```
静态配置主要管理版本号,动态配置用来动态改变逻辑失效时间 物理失效时间 重入锁数量,动静态配置文件合并后交由系统使用

####1.3 二级cache搭配使用
localcache主要解决分布式缓存访问的网络开销以及对象创建释放产生的损耗,所以在两级cache,配置filter时RemoteHtmlCacheFilter优先级要低于LocalHtmlCacheFilter

##2.数据级缓存
###实现策略与1.2类似,系统初始化会默认读取service-config.properties文件中相应的配置进行初始化.如果需要从其他配置文件装载,需要在缓存模块初始化前通过设置系统参数(System.properties) cache.configFileLocation重新制定配置文件路径.
**静态缓存配置 demo**

```
	<!--静态业务cache配置，读取cache配置文件，搭配动态cache配置（动态调整cache的时间,必要场景才会使用到）；
新增加一项cache需要进行一项静态配置包含cacheKey expire version，每次修改缓存数据的数据结构都需要对版本号进行修改。
对某项缓存有变更的需求，才需要在diamond上进行配置（注意版本号是不允许动态配置去覆盖静态配置的）-->
<cacheConfig>
    <serviceCacheConfig>
        <cacheKey>getProductHotDataList</cacheKey>
        <expire>30</expire>
        <maxExpire>3600</maxExpire>
        <lockSize>256</lockSize>
        <version>3</version>
    </serviceCacheConfig>
</cacheConfig>
```
**动态配置 demo**

```
<cacheConfig>
    <serviceCacheConfig>
        <cacheKey>getProductHotDataList</cacheKey>
        <expire>30</expire>
        <maxExpire>3600</maxExpire>
        <lockSize>256</lockSize>
        <version>3</version>
    </serviceCacheConfig>
</cacheConfig>
```

**代码示例**

```
private List<ProductHotData> getProductHotDatas(final int appid, long[] itemIds) {
    Arrays.sort(itemIds);//顺序不影响缓存命中
    return ConcurrentCacheHelper.getInstance().execute(
    //getProductHotDataList与配置文件中cacheKey对应,会根据这个查找配置
            CacheConfigHelper.getInstance().getServiceCacheConfig("getProductHotDataList"),
            new ConcurrentCacheHelper.Delegate<ArrayList<ProductHotData>>() {
                @Override
                public ArrayList<ProductHotData> execute(Object... params) {
                    ArrayList<ProductHotData> hotDatas = new ArrayList<ProductHotData>();
                    //do something
                    return hotDatas;
                }
            }, itemIds);
}
//TODO 目前尚未实现切面方式为业务函数生成如上的代理,需要手写代理函数,后续可以考虑使用注解+字节码技术完成这一步
```
