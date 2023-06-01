package com.hmdp;

import cn.hutool.core.convert.Convert;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmdp.entity.Cat;
import com.hmdp.entity.Shop;
import com.hmdp.service.IShopService;
import com.hmdp.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@SpringBootTest
class HmDianPingApplicationTests {

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Test
    void test(){
        List<Cat> cats=new ArrayList<>();
        cats.add(new Cat("cat1",11));
        cats.add(new Cat("cat2",12));
        cats.add(new Cat("cat3",13));
        cats.add(new Cat("cat4",14));

        System.out.println(cats);
        String s = JSONArray.toJSONString(cats);
        JSONArray objects = JSONArray.parseArray(s);
        System.out.println(objects.get(0).toString());


        List newList = new ArrayList();
        newList.add("o");
        newList.add("p");
        newList.add("q");
        redisTemplate.opsForList().leftPushAll("list",newList);
        List<String> list = redisTemplate.opsForList().range("list", 0, -1);
        System.out.println(list);
    }

    @Test
    void test1(){

        List<Cat> cats=new ArrayList<>();
        cats.add(new Cat("cat1",11));
        cats.add(new Cat("cat2",12));
        cats.add(new Cat("cat3",13));
        cats.add(new Cat("cat4",14));
        System.out.println("cats= "+cats);
        Cat cat = new Cat("cat1", 11);
        System.out.println(cat);
        System.out.println("cat="+cat);
        JSONObject o = (JSONObject) JSONObject.toJSON(cat);
        String s = JSONObject.toJSONString(cat);
        System.out.println("o="+o);
        System.out.println("s="+s+", s.length= "+s.length());
        System.out.println("o.name= "+o.get("name")+", o.age= "+o.get("age"));
        Cat t = JSONObject.toJavaObject(o,Cat.class);
        System.out.println("t= "+t);
        JSONObject parse = (JSONObject) JSONObject.parse(s);
        System.out.println("parse= "+parse);
        System.out.println("parse.name= "+parse.getString("name")+", parse.get= "+parse.getInteger("age"));

        String s1 = JSONObject.toJSONString(cats);
        System.out.println("s1= "+s1+", s1.length= "+s1.length());
        JSONArray objects = JSONObject.parseArray(s1);
        System.out.println("objects= "+objects);
        for (Object o1:objects){
            System.out.println(o1);
        }

        List<Cat> cats1 = JSONObject.parseArray(s1, Cat.class);
        System.out.println("cats1= "+cats1);
        for(Cat c:cats1){
            System.out.println(c);
        }

        List<Cat> cats2 = JSONArray.parseArray(s1, Cat.class);
        System.out.println("cats2= "+cats2);
        String s2 = JSONArray.toJSONString(cats);
        System.out.println("s2= "+s2+", s2.length"+s2.length());
        JSONObject o1 = (JSONObject) JSONArray.toJSON(cat);
        System.out.println("o1.name= "+o1.get("name")+", o1.age= "+o1.get("age"));


    }

    @Autowired
    IShopService shopService;
    @Test
    void test2() throws InterruptedException {
        shopService.saveShop2Redis(1L,20L);
    }

    @Autowired
    private RedisIdWorker redisIdWorker;

    private ExecutorService es=Executors.newFixedThreadPool(500);
    @Test
    void test3() throws InterruptedException {

        CountDownLatch countDownLatch = new CountDownLatch(300);
        Runnable task=()->{
            for (int i=0;i<100;i++){
                long nextId = redisIdWorker.nextId("order");
                System.out.println("id= "+nextId);
            }
            countDownLatch.countDown();
        };
        long begin = System.currentTimeMillis();
        for (int i=0;i<300;i++){
            es.submit(task);
        }
        countDownLatch.await();
        long end = System.currentTimeMillis();

        System.out.println("time: "+(end-begin));

    }

    @Test
    void test4(){
        List<Shop> list = shopService.list();
        Map<Long,List<Shop>> map=list.stream().collect(Collectors.groupingBy(Shop::getTypeId));
        for (Map.Entry<Long, List<Shop>> entry : map.entrySet()) {
            Long typeId = entry.getKey();
            String key="shop:geo:"+typeId;
            List<Shop> value = entry.getValue();

            List<RedisGeoCommands.GeoLocation<String>> locations=new ArrayList<>(value.size());


            for (Shop shop : value) {
                //redisTemplate.opsForGeo().add(key,new Point(shop.getX(),shop.getY()),shop.getId().toString());
                locations.add(new RedisGeoCommands.GeoLocation<>(shop.getId().toString(),new Point(shop.getX(),shop.getY())));

            }
            redisTemplate.opsForGeo().add(key,locations);

        }

    }


    @Test
    void testPipline(){
        Jedis jedis = new Jedis("47.120.39.2",6379);
        jedis.auth("123");

        Pipeline pipelined = jedis.pipelined();
        for (int i=2;i<=100000;i++){
//            pipelined.set("test:key_"+i,"value_"+i);
//            if (i%1000==0){
//                pipelined.sync();
//            }
            pipelined.del("test:key_"+i);
            if (i%1000==0){
                pipelined.sync();
            }
        }
    }

    @Test
    void testJackSon(){
        redisTemplate.opsForValue().set("fwj","21");

    }
}
