package com.atjixue.gulimall.search;


import com.alibaba.fastjson.JSON;
import com.atjixue.gulimall.search.config.GulimallElasticSearchConfig;
import lombok.Data;
import lombok.ToString;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.Avg;
import org.elasticsearch.search.aggregations.metrics.AvgAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GulimallSearchApplicationTests {
    @Autowired
    private RestHighLevelClient client;

    /**
     * Copyright 2022 bejson.com
     */

    /**
     * Auto-generated: 2022-08-16 17:22:37
     *
     * @author bejson.com (i@bejson.com)
     * @website http://www.bejson.com/java2pojo/
     */
    @Data
    @ToString
   static public class Account {

        private int account_number;
        private int balance;
        private String firstname;
        private String lastname;
        private int age;
        private String gender;
        private String address;
        private String employer;
        private String email;
        private String city;
        private String state;

    }


    @Test
    public void searchData() throws IOException {
        // 1、创建检索请求
        SearchRequest searchRequest = new SearchRequest();
        //指定索引
        searchRequest.indices("bank");
        //指定DSL 检索条件
        //SearchSourceBuilder 封装条件
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        // 1.1、构造检索条件
        sourceBuilder.query(QueryBuilders.matchQuery("address","mill"));
        //按照年龄的值分布进行聚合
        TermsAggregationBuilder ageAgg = AggregationBuilders.terms("ageagg").field("age").size(10);
        sourceBuilder.aggregation(ageAgg);
        //计算平均薪资
        AvgAggregationBuilder balanceAvg = AggregationBuilders.avg("balanceAvg").field("balance");
        sourceBuilder.aggregation(balanceAvg);

//        sourceBuilder.from();
//        sourceBuilder.size();
//        sourceBuilder.aggregation();
        searchRequest.source(sourceBuilder);
        System.out.println("检索条件：" + sourceBuilder.toString());
        // 2、执行检索
        SearchResponse searchResponse = client.search(searchRequest, GulimallElasticSearchConfig.COMMON_OPTIONS);

        //3、 分析结果
        System.out.println(searchResponse.toString());
        //Map map = JSON.parseObject(searchResponse.toString(), Map.class);
        // 3.1)、获取所有查到的数据
        SearchHits hits = searchResponse.getHits();
        SearchHit[] searchHits = hits.getHits();
        for (SearchHit searchHit : searchHits) {
            searchHit.getIndex();
            searchHit.getType();
            searchHit.getScore();
            String string = searchHit.getSourceAsString();
            Account account = JSON.parseObject(string, Account.class);
            System.out.println("account" + account.toString());
        }
        //3.2 获取拿到的分析数据
        Aggregations aggregations = searchResponse.getAggregations();
//        for (Aggregation aggregation : aggregations.asList()) {
//            System.out.println("当前聚合：" + aggregation.getName());
//            aggregation.get
//        }
        Terms ageAgg1 = aggregations.get("ageAgg");
        for (Terms.Bucket bucket : ageAgg1.getBuckets()) {
            String keyString = bucket.getKeyAsString();
            System.out.println("年龄" + keyString +"===>" +bucket.getDocCount());
        }
        Avg balanceAvg1 = aggregations.get("balanceAvg");
        System.out.println("平均薪资" + balanceAvg1.getValue());

    }
    // 测试存储数据 更新也可以
    @Test
    public void indexData() throws IOException {
        IndexRequest indexRequest = new IndexRequest("users");
        indexRequest.id("1");
//        indexRequest.source("username","zhangsan","age",18,"gender","男");
        User user = new User();
        user.setUsername("zhangsan");
        user.setAge(18);
        user.setGender("男");
        String s = JSON.toJSONString(user);
        indexRequest.source(s, XContentType.JSON);

        // 执行操作
        IndexResponse index = client.index(indexRequest,GulimallElasticSearchConfig.COMMON_OPTIONS);
        // 提取有用的响应数据
        System.out.println(index);
    }
    @Data
    class User{
        private String Username;
        private String gender;
        private Integer age;
    }
    @Test
    public void contextLoads() {
        System.out.println(client);
    }

}
