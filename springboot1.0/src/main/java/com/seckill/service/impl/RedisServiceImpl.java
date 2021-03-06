package com.seckill.service.impl;

import com.seckill.dao.GoodsDao;
import com.seckill.entity.Goods;
import com.seckill.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 使用Redis存储商品信息
 * 秒杀的时候，MySQL就只需要专注于写数据，读数据由Redis负责
 * key的有效期设置为2秒，虽无法实时显示商品库存，但好处在于MySQL无需负责读数据，仅需每2秒提供一次数据给Redis即可
 * @Author Tiny_W
 * @Date 2020-04-05 15:41
 * @Version 1.0
 */
@Service
public class RedisServiceImpl implements RedisService {
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private GoodsDao goodsDao;

    //获取到key时直接返回，否则设置key值
    @Override
    public List<Goods> getGoodsAll(int start, int length) {
        String key = "goods:list:start:"+start+":length:"+length;
        return redisTemplate.hasKey(key)?redisTemplate.opsForList().range(key,0,-1)
                :setGoodsAll(start,length);
    }

    @Override
    public List<Goods> setGoodsAll(int start, int length) {
        ListOperations<String,Goods> listOperations = redisTemplate.opsForList();
        String key = "goods:list:start:"+start+":length:"+length;
        List<Goods> goodsList = goodsDao.findAll(start,length);
//        System.out.println(goodsList);
        for(Goods goods:goodsList)
            listOperations.rightPush(key,goods);
        //由于是秒杀期间，需要时刻刷新商品库存，所以每个key存储2秒
        redisTemplate.expire(key,2, TimeUnit.SECONDS);
        return goodsList;
    }
}
