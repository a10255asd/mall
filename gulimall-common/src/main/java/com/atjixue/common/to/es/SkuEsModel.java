package com.atjixue.common.to.es;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * @Author LiuJixue
 * @Date 2022/8/16 23:08
 * @PackageName:com.atjixue.common.to.es
 * @ClassName: SkuEsModel
 * @Description: TODO
 * @Version 1.0
 */
/**
 * 		"properties": {
 *
 * 			"spuId": {
 * 				"type": "keyword"
 *            },
 * 			"skuTitle": {
 * 				"type": "text",
 * 				"analyzer": "ik_smart"
 *            },
 * 			"skuPrice": {
 * 				"type": "keyword"
 *            },
 * 			"skuImg": {
 * 				"type": "keyword",
 * 				"index": false,
 * 				"doc_values": false
 *            },
 * 			"saleCount": {
 * 				"type": "long"
 *            },
 * 			"hasStock": {
 * 				"type": "boolean"
 *            },
 * 			"hotScore": {
 * 				"type": "long"
 *            },
 * 			"brandId": {
 * 				"type": "long"
 *            },
 * 			"catalogId": {
 * 				"type": "long"
 *            },
 * 			"brandName": {
 * 				"type": "keyword",
 * 				"index": false,
 * 				"doc_values": false
 *            },
 * 			"brandImg": {
 * 				"type": "keyword",
 * 				"index": false,
 * 				"doc_values": false
 *            },
 * 			"catalogName": {
 * 				"type": "keyword",
 * 				"index": false,
 * 				"doc_values": false
 *            },
 * 			"attrs": {
 * 				"type": "nested",
 * 				"properties": {
 * 					"attrId": {
 * 						"type": "long"
 *                    },
 * 					"attrName": {
 * 						"type": "keyword",
 * 						"index": false,
 * 						"doc_values": false
 *                    },
 * 					"attrValue": {
 * 						"type": "keyword"
 *                    }
 *                }
 *            }		}
 *            */
@Data
public class SkuEsModel {

    private Long skuId;

    private Long spuId;

    private String skuTitle;

    private BigDecimal skuPrice;

    private String skuImg;

    private Long saleCount;

    private Boolean hasStock;

    private Long hotScore;

    private Long brandId;

    private Long catalogId;

    private String brandName;

    private String brandImg;

    private String catalogName;

    private List<Attrs> attrs;

    @Data
    public static class Attrs{

        private Long attrId;

        private String attrName;

        private String attrValue;
    }

}

