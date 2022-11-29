package com.atjixue.gulimall.product.dao;

import com.atjixue.gulimall.product.entity.AttrGroupEntity;
import com.atjixue.gulimall.product.vo.SkuItemVo;
import com.atjixue.gulimall.product.vo.SpuItemAttrGroupVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 属性分组
 * 
 * @author liujixue
 * @email 1025519998@qq.com
 * @date 2022-08-01 12:43:18
 */
@Mapper
public interface AttrGroupDao extends BaseMapper<AttrGroupEntity> {

    List<SpuItemAttrGroupVo> getAttrGroupWithAttrsBySpuId(@Param("spuId") Long spuId, @Param("catalogId") Long catalogId);
}
