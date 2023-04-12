package cn.itcast.hotel.service.impl;

import cn.itcast.hotel.mapper.HotelMapper;
import cn.itcast.hotel.params.PageResult;
import cn.itcast.hotel.params.SearchParams;
import cn.itcast.hotel.pojo.Hotel;
import cn.itcast.hotel.pojo.HotelDoc;
import cn.itcast.hotel.service.IHotelService;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.api.R;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class HotelService extends ServiceImpl<HotelMapper, Hotel> implements IHotelService {

    @Resource
    private RestHighLevelClient restHighLevelClient;

    @Override
    public PageResult search(SearchParams searchParams) {
        try {
            SearchRequest request = new SearchRequest("hotel");

            buildBasicQuery(searchParams, request);

            int page = searchParams.getPage();
            int size = searchParams.getSize();
            request.source().from((page - 1) * size).size(size);

            String location = searchParams.getLocation();

            if (!StringUtils.isEmpty(location)) {
                request.source().sort(SortBuilders.geoDistanceSort("location", new GeoPoint(location)).order(SortOrder.ASC).unit(DistanceUnit.KILOMETERS));
            }
            if (!("default".equals(searchParams.getSortBy()))) {
                request.source().sort(searchParams.getSortBy());
            }

            if (!StringUtils.isEmpty(searchParams.getKey())) {
                request.source().highlighter(new HighlightBuilder().field("name").requireFieldMatch(false));
            }

            SearchResponse searchResponse = restHighLevelClient.search(request, RequestOptions.DEFAULT);

            return handleResponse(searchResponse);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public Map<String, List<String>> getFilters(SearchParams searchParams) {
        try {
            SearchRequest request = new SearchRequest("hotel");

            buildBasicQuery(searchParams, request);

            request.source().size(0);

            buildAggregations(request);

            SearchResponse response = restHighLevelClient.search(request, RequestOptions.DEFAULT);

            Aggregations aggregations = response.getAggregations();

            Map<String, List<String>> filters = new HashMap<>(3);
            filters.put("brand", getAggregationByName(aggregations, "brandAgg"));
            filters.put("city", getAggregationByName(aggregations, "cityAgg"));
            filters.put("starName", getAggregationByName(aggregations, "starAgg"));

            return filters;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<String> getSuggestion(String key) {
        try {
            SearchRequest request = new SearchRequest("hotel");

            request.source().suggest(
                    new SuggestBuilder().addSuggestion("mySuggestion",
                            SuggestBuilders.completionSuggestion("suggestion")
                                    .prefix(key)
                                    .skipDuplicates(true)
                                    .size(10)
                    )
            );

            SearchResponse response = restHighLevelClient.search(request, RequestOptions.DEFAULT);

            List<String> keys = new ArrayList<>();
            CompletionSuggestion suggestion = response.getSuggest().getSuggestion("mySuggestion");
            for (Suggest.Suggestion.Entry.Option options : suggestion.getOptions()) {
                String s = options.getText().toString();
                keys.add(s);
            }

            return keys;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void deleteById(Long id) {
        try {
            DeleteRequest request = new DeleteRequest("hotel", id.toString());
            restHighLevelClient.delete(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void insertById(Long id) {
        try {
            Hotel hotel = getById(id);

            HotelDoc hotelDoc = new HotelDoc(hotel);
            IndexRequest request = new IndexRequest("hotel").id(id.toString());
            request.source(JSON.toJSONString(hotelDoc), XContentType.JSON);
            restHighLevelClient.index(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private List<String> getAggregationByName(Aggregations aggregations, String aggName) {
        Terms terms = aggregations.get(aggName);
        List<? extends Terms.Bucket> buckets = terms.getBuckets();
        List<String> list = new ArrayList<>();
        for (Terms.Bucket bucket : buckets) {
            String value = bucket.getKeyAsString();
            list.add(value);
        }
        return list;
    }

    private void buildAggregations(SearchRequest request) {
        request.source().aggregation(AggregationBuilders.terms("brandAgg").field("brand").size(100));
        request.source().aggregation(AggregationBuilders.terms("cityAgg").field("city").size(100));
        request.source().aggregation(AggregationBuilders.terms("starAgg").field("starName").size(100));
    }

    private PageResult handleResponse(SearchResponse searchResponse) {
        SearchHits searchHits = searchResponse.getHits();
        long total = searchHits.getTotalHits().value;

        SearchHit[] hits = searchHits.getHits();

        List<HotelDoc> hotels = new ArrayList<>();
        for (SearchHit hit : hits) {
            String json = hit.getSourceAsString();

            HotelDoc hotelDoc = JSON.parseObject(json, HotelDoc.class);

            Map<String, HighlightField> map = hit.getHighlightFields();
            if (map != null && !map.isEmpty()) {
                HighlightField highlightField = map.get("name");
                if (highlightField != null) {
                    // 3）获取高亮结果字符串数组中的第1个元素
                    String hName = highlightField.getFragments()[0].toString();
                    // 4）把高亮结果放到HotelDoc中
                    hotelDoc.setName(hName);
                }
            }
            Object[] sortValues = hit.getSortValues();
            if (sortValues.length > 0) {
                hotelDoc.setDistance(sortValues[0]);
            }
            hotels.add(hotelDoc);
        }

        return new PageResult(total, hotels);
    }

    private void buildBasicQuery(SearchParams searchParams, SearchRequest request) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        String key = searchParams.getKey();
        if (!StringUtils.isEmpty(key)) {
            boolQuery.must(QueryBuilders.matchQuery("all", key));
        } else {
            boolQuery.must(QueryBuilders.matchAllQuery());
        }

        String branch = searchParams.getBrand();
        if (!StringUtils.isEmpty(branch)) {
            boolQuery.filter(QueryBuilders.termQuery("brand", branch));
        }

        String city = searchParams.getCity();
        if (!StringUtils.isEmpty(city)) {
            boolQuery.filter(QueryBuilders.termQuery("city", city));
        }

        String starName = searchParams.getStarName();
        if (!StringUtils.isEmpty(starName)) {
            boolQuery.filter(QueryBuilders.termQuery("starName", starName));
        }

        Integer minPrice = searchParams.getMinPrice();
        Integer maxPrice = searchParams.getMaxPrice();
        if (minPrice != null && maxPrice != null) {
            maxPrice = maxPrice == 0 ? Integer.MAX_VALUE : maxPrice;
            boolQuery.filter(QueryBuilders.rangeQuery("price").gte(minPrice).lte(maxPrice));
        }

        /*FunctionScoreQueryBuilder functionScoreQuery = QueryBuilders.functionScoreQuery(
                boolQueryBuilder, // 原始查询，boolQuery
                new FunctionScoreQueryBuilder.FilterFunctionBuilder[]{ // function数组
                        new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                                QueryBuilders.termQuery("isAD", true), // 过滤条件
                                ScoreFunctionBuilders.weightFactorFunction(10) // 算分函数
                        )
                }
        );*/

        request.source().query(boolQuery);

    }
}
