package com.atjixue.gulimall.seckill.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.atjixue.common.to.SeckillOrderTo;
import com.atjixue.common.utils.R;
import com.atjixue.common.vo.MemberRespVo;
import com.atjixue.gulimall.seckill.feign.CouponFeignService;
import com.atjixue.gulimall.seckill.feign.ProductFeignService;
import com.atjixue.gulimall.seckill.interceptor.LoginUserInterceptor;
import com.atjixue.gulimall.seckill.service.SecKillService;
import com.atjixue.gulimall.seckill.to.SecKillSkuRedisTo;
import com.atjixue.gulimall.seckill.vo.SeckillSessionsWithSkus;
import com.atjixue.gulimall.seckill.vo.SeckillSkuVo;
import com.atjixue.gulimall.seckill.vo.SkuInfoVo;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @Author LiuJixue
 * @Date 2022/9/25 21:30
 * @PackageName:com.atjixue.gulimall.seckill.service.impl
 * @ClassName: SeckillServiceImpl
 * @Description: TODO
 * @Version 1.0
 */
@Service
@Slf4j
public class SeckillServiceImpl implements SecKillService {

    @Autowired
    CouponFeignService couponFeignService;

    @Autowired
    StringRedisTemplate redisTemplate;
    @Autowired
    ProductFeignService productFeignService;
    private final String SESSIONS_CACHE_PREFIX = "seckill:sessions";

    private final String SKUKILL_CACHE_PREFIX = "seckill:skus";

    private final String SKU_STOCK_SEMAPHORE = "seckill:stock:"; //➕商品随机码
    @Autowired
    RedissonClient redissonClient;
    @Autowired
    RabbitTemplate rabbitTemplate;

    @Override
    public void uploadSeckillSkuLatest3Days() {
        // 1、扫描需要参与秒杀的活动
        R session = couponFeignService.getLatest3DaySession();
        if(session.getCode() == 0){
            // 上架商品
            List<SeckillSessionsWithSkus> sessionData = session.getData(new TypeReference<List<SeckillSessionsWithSkus>>() {
            });
            // 缓存到redis
            // 1、 缓存活动信息
            saveSessioninfos(sessionData);
            // 2、 缓存活动商品信息
            saveSessionSkuInfos(sessionData);

        }
    }
    /**
     * 返回当前时间可以参与的秒杀商品信息
     * */
    @Override
    public List<SecKillSkuRedisTo> getCurrentSeckillSkus() {
        // 确定当前时间属于哪个秒杀场次
        long time = new Date().getTime();
        Set<String> keys = redisTemplate.keys(SESSIONS_CACHE_PREFIX + "*");
        for (String key : keys) {
            String replace = key.replace(SESSIONS_CACHE_PREFIX, "");
            String[] s = replace.split("_");
            Long start = Long.parseLong(s[0]);
            Long end = Long.parseLong(s[1]);
            if(time>= start && time<=end){
                List<String> range = redisTemplate.opsForList().range(key, -100, 100);
                BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
                List<String> list = hashOps.multiGet(range);
                if(list != null){
                    List<SecKillSkuRedisTo> collect = list.stream().map(item -> {
                        SecKillSkuRedisTo redis = JSON.parseObject((String) item, SecKillSkuRedisTo.class);
                        //redis.setRandomCode(null); 当前秒杀开始了 就需要随机码
                        return redis;
                    }).collect(Collectors.toList());
                    return collect;
                }
                break;
            }
        }
        // 获取这个秒杀场次需要的所有信息
        return null;
    }

    @Override
    public SecKillSkuRedisTo getSkuSecKillInfo(Long skuId) {
        // 找到所有需要参与秒杀的商品的key
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
        Set<String> keys = hashOps.keys();
        if(keys!= null && keys.size() > 0){
            String regx = "\\d_" + skuId;
            for (String key : keys) {
                // 6_4
                if(Pattern.matches(regx,key)){
                    String json = hashOps.get(key);
                    SecKillSkuRedisTo skuRedisTo = JSON.parseObject(json, SecKillSkuRedisTo.class);
                    // 随机码
                    long current = new Date().getTime();
                    if(current >= skuRedisTo.getStartTime() && current<= skuRedisTo.getEndTime()){
                    }else {
                        skuRedisTo.setRandomCode(null);
                    }
                    return skuRedisTo;
                }
            }
        }
        return null;
    }

    @Override
    public String kill(String killId, String key, Integer num)  {
        long s1 = System.currentTimeMillis();
        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();
        // 1、获取当前秒杀商品的详细信息
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
        String json = hashOps.get(killId);
        if(StringUtils.isEmpty(json)){
           return null;
        }else {
            // 成功的逻辑
            SecKillSkuRedisTo redis = JSON.parseObject(json, SecKillSkuRedisTo.class);
            // 校
            // 验时间的合法性
            Long startTime = redis.getStartTime();
            Long endTime = redis.getEndTime();
            long time = new Date().getTime();
            long ttl = endTime - time;
            if( time>= startTime && time <=endTime){
                // 2、校验随机码和商品id是否正确
                String randomCode = redis.getRandomCode();
                String skuId =redis.getPromotionSessionId()+ "_" + redis.getSkuId();
                if(randomCode.equals(key) && killId.equals(skuId)){
                    // 3、验证购物数量是否合理
                    if(num <=redis.getSeckillLimit()){
                        // 4、验证是否已经购买过了。幂等性处理。规定只要秒杀成功，就去redis里面站一个位置。userId_SessionId_skuId
                        // SETNX 不存在的时候才占位 最终返回成功或者失败
                        String redisKey = memberRespVo.getId() + "_" +redis.getPromotionSessionId()  + "_" + skuId;
                        // 自动过期。
                        Boolean aBoolean = redisTemplate.opsForValue().setIfAbsent(redisKey, num.toString(), ttl, TimeUnit.MILLISECONDS);
                        if(aBoolean){
                            // 站位成功 说明从来没有买过
                            // 分布式信号量做减操作， 能减成功就说明买成功
                            RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + randomCode);
                            try {
                                // 120 20ms
                                boolean b = semaphore.tryAcquire(num);
                                if(b){
                                    String timeId = IdWorker.getTimeId();
                                    SeckillOrderTo seckillOrderTo = new SeckillOrderTo();
                                    seckillOrderTo.setOrderSn(timeId);
                                    seckillOrderTo.setMemberId(memberRespVo.getId());
                                    seckillOrderTo.setNum(num);
                                    seckillOrderTo.setPromotionSessionId(redis.getPromotionSessionId());
                                    seckillOrderTo.setSkuId(redis.getSkuId());
                                    seckillOrderTo.setSeckillPrice(redis.getSeckillPrice());
                                    // 秒杀成功
                                    // 快速下单。发送mq消息
                                    rabbitTemplate.convertAndSend("order-event-exchange",
                                            "order.seckill.order",
                                            seckillOrderTo);
                                    long s2= System.currentTimeMillis();
                                    log.info("耗时。。。。。" + (s2-s1));
                                    return timeId;
                                }else {
                                    return null;
                                }
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                        }else {
                            // 说明已经买过了
                            return null;
                        }
                    }
                }else {
                    return null;
                }
            }else {
                return null;
            }
        }
        return null;
    }

    private void saveSessioninfos(List<SeckillSessionsWithSkus> sessions){
        sessions.stream().forEach(session->{
            Long startTime = session.getStartTime().getTime();
            Long endTime = session.getEndTime().getTime();
            String key = SESSIONS_CACHE_PREFIX +  startTime + "_" + endTime;
            Boolean hasKey = redisTemplate.hasKey(key);
            // 缓存活动信息
            if(!hasKey){
                List<String> collect = session.getRelationSkus().stream().map(item ->item.getPromotionSessionId()+ "_"+ item.getSkuId().toString()).collect(Collectors.toList());
                redisTemplate.opsForList().leftPushAll(key,collect);
            }
        });
    }
    private void saveSessionSkuInfos(List<SeckillSessionsWithSkus> sessions){
        sessions.stream().forEach(session->{
            // 准备hash操作
            BoundHashOperations<String, Object, Object> ops = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
            session.getRelationSkus().stream().forEach(seckillSkuVo -> {
                // 4、随机码
                String token = UUID.randomUUID().toString().replace("_","");
                if(!ops.hasKey(seckillSkuVo.getPromotionSessionId()+"_"+seckillSkuVo.getSkuId().toString())){
                    // 缓存商品
                    SecKillSkuRedisTo redisTo = new SecKillSkuRedisTo();
                    // 1、sku的基本信息
                    R skuInfo = productFeignService.getSkuInfo(seckillSkuVo.getSkuId());
                    if(skuInfo.getCode() == 0){
                        SkuInfoVo info = skuInfo.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                        });
                        redisTo.setSkuInfo(info);
                    }
                    // 2、sku的秒杀信息
                    BeanUtils.copyProperties(seckillSkuVo,redisTo);
                    // 3、设置当前商品的秒杀信息'
                    redisTo.setStartTime(session.getStartTime().getTime());
                    redisTo.setEndTime(session.getEndTime().getTime());

                    redisTo.setRandomCode(token);
                    String jsonString = JSON.toJSONString(redisTo);
                    ops.put(seckillSkuVo.getPromotionSessionId()+ "_"+seckillSkuVo.getSkuId().toString(),jsonString);
                    // 如果当前这个场次的商品的库存信息已经上架就不需要上架
                    // 引入分布式信号量 使用库存作为分布式信号量 主要作用是限流
                    RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + token);
                    semaphore.trySetPermits(seckillSkuVo.getSeckillCount());
                }
            });
        });
    }
}
